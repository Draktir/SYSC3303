package server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import Configuration.Configuration;
import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.InvalidDataPacketException;
import packet.InvalidErrorPacketException;
import packet.PacketParser;
import packet.ErrorPacket.ErrorCode;
import rop.ROP;
import rop.Result;
import utils.Recursive;

public class TftpWriteTransfer {
  public void start(TransferState transferState) {
    // create the file
    final Result<FileWriter, IrrecoverableError> fileResult = createFile(transferState);
    
    if (fileResult.FAILURE) {
      sendError(transferState, fileResult.failure);
      return;
    }

    final FileWriter fileWriter = fileResult.success;
    
    // file writer function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock = (state) -> {
      try {
        fileWriter.writeBlock(state.blockData);
      } catch (IOException e) {
        e.printStackTrace();
        IrrecoverableError err = new IrrecoverableError(
            ErrorCode.NOT_DEFINED, "Internal server error while writing file. Please try again.");
        return Result.failure(err);
      }
      return Result.success(state);
    };
    
    // data packet receiver function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> receiveValidDataPacket = (state) -> {
      return TftpWriteTransfer.receiveValidDataPacket(state);
    };
    
    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();
    
    
    
    // create and send the first ACK.
    final Result<TransferState, IrrecoverableError> ackResult = 
        rop.buildSwitch(TftpWriteTransfer::buildAck)
        .andThen(rop.bind(TftpWriteTransfer::sendAck))
        .apply(transferState);
    
    if (ackResult.FAILURE) {
      log("ERROR: Sending ACK failed.");
      errorCleanup(transferState);
      fileWriter.close();
      return;
    }
    
    TransferState currentState = ackResult.success;
    
    // perform the actual file transfer
    do {
      Result<TransferState, IrrecoverableError> stepResult = 
          receiveValidDataPacket
          .andThen(rop.bind(writeFileBlock))
          .andThen(rop.map(TftpWriteTransfer::buildAck))
          .andThen(rop.bind(TftpWriteTransfer::sendAck))
          .apply(currentState);
      
      if (stepResult.SUCCESS) {
        currentState = stepResult.success;
      } else {
        log("Error encountered during file transfer: " + stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          sendError(currentState, stepResult.failure);
        }
        errorCleanup(currentState);
        break;
      }
    } while (currentState.blockData.length == 512);
    
    fileWriter.close();
  }
  
  private static Result<FileWriter, IrrecoverableError> createFile(TransferState state) {
    FileWriter fw = new FileWriter(state.request.getFilename());
    try {
      fw.createFile();
    } catch (FileAlreadyExistsException e) {
      log("ERROR: " + e.getMessage());
      return Result.failure(new IrrecoverableError(
          ErrorCode.FILE_ALREADY_EXISTS,
          e.getMessage()));
      
    } catch (AccessDeniedException e) {
      log("ERROR: " + e.getMessage());
      return Result.failure(new IrrecoverableError(
          ErrorCode.ACCESS_VIOLATION,
          e.getMessage()));
    }
    
    return Result.success(fw);
  }
  
  private static TransferState buildAck(TransferState state) {
    Acknowledgement ack = new AcknowledgementBuilder()
        .setBlockNumber(state.blockNumber)
        .buildAcknowledgement();
    
    return TransferStateBuilder.clone(state)
        .acknowledgement(ack)
        .build();
  };
  
  private static Result<TransferState, IrrecoverableError> sendAck(TransferState state) {
    log("Sending ACK, block #" + state.acknowledgement.getBlockNumber());
    log(state.acknowledgement.toString());
    
    try {
      state.connection.sendPacket(state.acknowledgement);
    } catch (IOException e) {
      return Result.failure(new IrrecoverableError(e.getMessage()));
    }
    return Result.success(TransferStateBuilder.clone(state)
        .blockNumber(state.blockNumber + 1)
        .build());
  }
  
  private static Result<TransferState, IrrecoverableError> receiveValidDataPacket(TransferState state) {
    // initialization
    Supplier<Long> currentTime = () -> new Date().getTime();

    final long tsStart = currentTime.get();
    Supplier<Integer> calculateNewTimeout = () -> {
      Long to = Configuration.TIMEOUT_TIME - (currentTime.get() - tsStart);
      return to.intValue();
    };
    
    ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

    final Recursive<BiFunction<Integer, Integer,
        Result<TransferState, IrrecoverableError>>> receiveDataPacket = new Recursive<>();

    // receive DataPacket Function
    receiveDataPacket.func = (timeout, numRetries) -> {
      log("Expecting DataPacket with block #" + state.blockNumber);

      Result<DatagramPacket, RecoverableError> recvResult = receiveDatagram(state, timeout);

      if (recvResult.FAILURE) {
        log(recvResult.failure.message);

        if (numRetries < Configuration.MAX_RETRIES) {
          // resend the last ack and then recursively call this function again
          log("Re-sending last ACK");
          return rop.bind(TftpWriteTransfer::sendAck)
              .andThen((s) -> receiveDataPacket.func.apply(Configuration.TIMEOUT_TIME, numRetries + 1))
              .apply(Result.success(state));

        } else {
          IrrecoverableError err = new IrrecoverableError("Did not receive a response. Max retries exceeded.");
          return Result.failure(err);
        }
      }

      Function<DatagramPacket, Result<DataPacket, IrrecoverableError>> parseDataPacket = (dp) -> {
        return TftpWriteTransfer.parseDataPacket(dp);
      };

      
      ROP<DataPacket, TransferState, IrrecoverableError> ropDp = new ROP<>();
      
      return parseDataPacket
          .andThen(ropDp.bind((dataPacket) -> {
            if (dataPacket.getBlockNumber() > state.blockNumber) {
              // invalid block number, terminating
              IrrecoverableError err = new IrrecoverableError(
                  ErrorCode.ILLEGAL_TFTP_OPERATION,
                  "Wrong block number. Expected " + state.blockNumber + " got " + dataPacket.getBlockNumber()
              );
              return Result.failure(err);
            }
  
            if (dataPacket.getBlockNumber() < state.blockNumber) {
              // duplicate DataPacket, ignore
              log("Received a duplicate DataPacket, block# " + dataPacket.getBlockNumber());
              log(dataPacket.toString());
              
              log("Sending response to duplicate DataPacket");
              // respond with an ACK with the same block number as the duplicate data packet
              try {
                state.connection.sendPacket(
                    new AcknowledgementBuilder()
                      .setBlockNumber(dataPacket.getBlockNumber())
                      .buildAcknowledgement()
                );
              } catch (Exception e) {
                e.printStackTrace();
              }
              
              // recursively call this function to listen for another packet
              return receiveDataPacket.func.apply(calculateNewTimeout.get(), numRetries);
            }
  
            log("Received valid DataPacket, block #" + dataPacket.getBlockNumber());
            log(dataPacket.toString());
            
            // DataPacket is valid
            return Result.success(TransferStateBuilder.clone(state)
                .dataPacket(dataPacket)
                .blockData(dataPacket.getFileData())
                .build()
            );
          }))
          .apply(recvResult.success);
    };
    
    return receiveDataPacket.func.apply(Configuration.TIMEOUT_TIME, 1);
  }
  
  private static Result<DatagramPacket, RecoverableError> receiveDatagram(TransferState state, int timeout) {
    DatagramPacket datagram = null;
    try {
      datagram = state.connection.receive(timeout);
    } catch (SocketTimeoutException e) {
      return Result.failure(new RecoverableError("Receiving timed out."));
    }

    if (datagram == null) {
      return Result.failure(new RecoverableError("Nothing received."));
    }

    return Result.success(datagram);
  }

  private static Result<DataPacket, IrrecoverableError> parseDataPacket(DatagramPacket datagram) {
    PacketParser parser = new PacketParser();
    DataPacket dataPacket = null;
    try {
      dataPacket = parser.parseDataPacket(datagram);
    } catch (InvalidDataPacketException de) {
      // try to parse it as an error
      ErrorPacket errPacket = null;
      try {
        errPacket = parser.parseErrorPacket(datagram);
      } catch (InvalidErrorPacketException ee) {
        System.out.println("Received an invalid packet: " + de.getMessage());
        return Result.failure(new IrrecoverableError(
            ErrorCode.ILLEGAL_TFTP_OPERATION, de.getMessage()));
      }
      return Result.failure(new IrrecoverableError(
          "Error received " + errPacket.getErrorCode() + ": " + errPacket.getMessage()));
    }
    return Result.success(dataPacket);
  }
  
  
  private static void sendError(TransferState state, IrrecoverableError error) {
    log("Sending error to client " + error.errorCode + ": " + error.message);
    ErrorPacket err = new ErrorPacketBuilder()
        .setErrorCode(error.errorCode)
        .setMessage(error.message)
        .buildErrorPacket();

    try {
      state.connection.sendPacket(err);
    } catch (IOException e) {
      e.printStackTrace();
      log("Error occured while sending error packet. We're done!");
    }
  }
  
  private static void errorCleanup(TransferState transferState) {
    File f = new File(transferState.request.getFilename());
    if (f.exists()) {
      f.delete();
    }
  }
  
  private static void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[TFTP-WRITE " + name  + "] " + msg);
  }
}
