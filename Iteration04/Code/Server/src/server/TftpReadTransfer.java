package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import Configuration.Configuration;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import packet.ErrorPacketBuilder;
import packet.InvalidAcknowledgementException;
import packet.InvalidErrorPacketException;
import packet.InvalidRequestException;
import packet.PacketParser;
import rop.ROP;
import rop.Result;
import utils.Recursive;
import packet.Request;

public class TftpReadTransfer {
  
  public void start(TransferState transferState) {
    // open the requested file as a Stream
    final Result<Stream<byte[]>, IrrecoverableError> streamResult = createFileStream(transferState.request.getFilename());

    if (streamResult.FAILURE) {
      sendError(transferState, streamResult.failure);
      return;
    }

    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

    // get an iterator for our lazy file stream
    final Stream<byte[]> fileBlockStream = streamResult.success;
    final Iterator<byte[]> fileBlockIterator = fileBlockStream.iterator();
    
    
    
    
    // TODO:
    // This function violates referential transparency. Maybe a stream isn't
    // the right idea to read from the file?
    
    // file block reader function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> readFileBlock = (state) -> {
      byte[] fileBlock = null;
      if (fileBlockIterator.hasNext()) {
        fileBlock = fileBlockIterator.next();
      } else {
        // in case we need to send a last data packet with no data
        fileBlock = new byte[0];
      }
      
      fileBlock = (fileBlock == null) ? new byte[0] : fileBlock;

      return Result.success(new TransferStateBuilder()
          .clone(state)
          .blockNumber(state.blockNumber + 1)
          .blockData(fileBlock)
          .build()
      );
    };

    // send file, block by block
    do {
      Result<TransferState, IrrecoverableError> stepResult =
          readFileBlock
          .andThen(rop.map(TftpReadTransfer::buildDataPacket))
          .andThen(rop.bind(TftpReadTransfer::sendDataPacket))
          .andThen(rop.bind(TftpReadTransfer::receiveValidAck))
          .apply(transferState);

      if (stepResult.SUCCESS) {
        transferState = stepResult.success;
      } else {
        log("Error encountered during file transfer: " + stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          sendError(transferState, stepResult.failure);
        }
        break;
      }
    } while(transferState.blockData.length == Configuration.BLOCK_SIZE);

    log("Transfer complete. Terminating thread");
    fileBlockStream.close();
  }

  private static Result<Stream<byte[]>, IrrecoverableError> createFileStream(String filename) {
    // open the file
    FileReader fileReader = new FileReader();
    Stream<byte[]> fileStream;
    try {
      fileStream = fileReader.read(filename, Configuration.BLOCK_SIZE);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      IrrecoverableError err = new IrrecoverableError(
          ErrorPacket.ErrorCode.FILE_NOT_FOUND,
          "Could not find file " + filename + "' on the server.");

      return Result.failure(err);
    } catch (AccessDeniedException e) {
      e.printStackTrace();
      IrrecoverableError err = new IrrecoverableError(
          ErrorPacket.ErrorCode.ACCESS_VIOLATION,
          "Could not access file '" + filename + "' on the server.");

      return Result.failure(err);
    }

    return Result.success(fileStream);
  }

  private static TransferState buildDataPacket(TransferState state) {
    DataPacket dp = new DataPacket(
      state.request.getRemoteHost(),
      state.request.getRemotePort(),
      state.blockNumber,
      state.blockData
    );

    return new TransferStateBuilder()
        .clone(state)
        .dataPacket(dp)
        .build();
  }

  private static Result<TransferState, IrrecoverableError> sendDataPacket(TransferState state) {
    log("Sending data packet, block #" + state.dataPacket.getBlockNumber());
    log(state.dataPacket.toString());
    
    try {
      state.connection.sendPacket(state.dataPacket);
    } catch (IOException e) {
      return Result.failure(new IrrecoverableError(e.getMessage()));
    }
    return Result.success(state);
  }
  
  private static Result<TransferState, IrrecoverableError> receiveValidAck(TransferState state) {
    // initialization
    Supplier<Long> currentTime = () -> new Date().getTime();

    final long tsStart = currentTime.get();
    Supplier<Integer> calculateNewTimeout = () -> {
      Long to = Configuration.TIMEOUT_TIME - (currentTime.get() - tsStart);
      return to.intValue();
    };
    
    ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

    final Recursive<BiFunction<Integer, Integer,
        Result<TransferState, IrrecoverableError>>> receiveAck = new Recursive<>();

    // receive ACK Function
    receiveAck.func = (timeout, numRetries) -> {
      log("Expecting ACK with block #" + state.blockNumber);

      Result<DatagramPacket, RecoverableError> recvResult =
          receiveDatagram(state, timeout);

      if (recvResult.FAILURE) {
        log(recvResult.failure.message);

        if (numRetries < Configuration.MAX_RETRIES) {
          // resend the data packet and then recursively call this function again
          log("Re-sending last Data Packet");
          return rop.bind(TftpReadTransfer::sendDataPacket)
              .andThen((s) -> receiveAck.func.apply(Configuration.TIMEOUT_TIME, numRetries + 1))
              .apply(Result.success(state));

        } else {
          IrrecoverableError err = new IrrecoverableError("Did not receive a response. Max retries exceeded.");
          return Result.failure(err);
        }
      }

      Function<DatagramPacket, Result<Acknowledgement, IrrecoverableError>> parseAck = (dp) -> {
        return TftpReadTransfer.parseAck(dp);
      };

      
      ROP<Acknowledgement, TransferState, IrrecoverableError> ropAck = new ROP<>();
      
      return parseAck
          .andThen(ropAck.bind((ack) -> {
            if (ack.getBlockNumber() > state.blockNumber) {
              // invalid block number, terminating
              IrrecoverableError err = new IrrecoverableError(
                  ErrorCode.ILLEGAL_TFTP_OPERATION,
                  "Wrong block number. Expected " + state.blockNumber + " got " + ack.getBlockNumber()
              );
              return Result.failure(err);
            }
  
            if (ack.getBlockNumber() < state.blockNumber) {
              // duplicate ACK, ignore
              log("Received a duplicate ACK, block# " + ack.getBlockNumber());
              log(ack.toString());
              // recursively call this function to listen for another packet
              return receiveAck.func.apply(calculateNewTimeout.get(), numRetries);
            }
  
            log("Received valid ACK, block #" + ack.getBlockNumber());
            log(ack.toString());
            
            // ACK is valid
            return Result.success(new TransferStateBuilder()
                .clone(state)
                .acknowledgement(ack)
                .build()
            );
          }))
          .apply(recvResult.success);
    };
    
    return receiveAck.func.apply(Configuration.TIMEOUT_TIME, 1);
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

  private static Result<Acknowledgement, IrrecoverableError> parseAck(DatagramPacket datagram) {
    PacketParser parser = new PacketParser();
    Acknowledgement ack = null;
    try {
      ack = parser.parseAcknowledgement(datagram);
    } catch (InvalidAcknowledgementException ae) {
      // try to parse it as an error
      ErrorPacket errPacket = null;
      try {
        errPacket = parser.parseErrorPacket(datagram);
      } catch (InvalidErrorPacketException ee) {
        System.out.println("Received an invalid packet: " + ae.getMessage());

      }
      return Result.failure(new IrrecoverableError(
          "Error received " + errPacket.getErrorCode() + ": " + errPacket.getMessage()));
    }
    return Result.success(ack);
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
  
  private static void log(String msg) {
    System.out.println("[TFTP-TRANSFER] " + msg);
  }
}
