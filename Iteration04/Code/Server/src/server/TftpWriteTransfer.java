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

import configuration.Configuration;
import file_io.FileWriter;
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
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;
import utils.RecoverableError;
import utils.Recursive;

public class TftpWriteTransfer {
  private static final Logger logger = new Logger("TFTP-WRITE");

  public static void start(TransferState transferState) {
    // create the file
    final Result<FileWriter, IrrecoverableError> fileResult = FileOperations.createFile.apply(transferState);
    
    if (fileResult.FAILURE) {
      NetworkOperations.sendError.accept(transferState, fileResult.failure);
      return;
    }

    final FileWriter fileWriter = fileResult.success;
    
    // file writer function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock =
        fileBlockWriter(fileWriter);

    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();
    
    // create and send the first ACK
    final Result<TransferState, IrrecoverableError> ackResult = 
        rop.buildSwitch(LocalOperations.buildAck)
        .andThen(rop.bind(NetworkOperations.sendAck))
        .andThen(rop.map((state) -> {
          // advance the block number
          return TransferStateBuilder.clone(state)
              .blockNumber(state.blockNumber + 1)
              .build();
        }))
        .apply(transferState);
    
    if (ackResult.FAILURE) {
      logger.logError("ERROR: Sending ACK failed.");
      errorCleanup(transferState);
      fileWriter.close();
      return;
    }
    
    TransferState currentState = ackResult.success;
    
    // perform the file transfer
    do {
      Result<TransferState, IrrecoverableError> stepResult = 
          NetworkOperations.receiveValidDataPacket
          .andThen(rop.bind(writeFileBlock))
          .andThen(rop.map(LocalOperations.buildAck))
          .andThen(rop.bind(NetworkOperations.sendAck))
          .apply(currentState);
      
      if (stepResult.SUCCESS) {
        currentState = stepResult.success;
      } else {
        logger.logError("Error encountered during file transfer: " + stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          NetworkOperations.sendError.accept(currentState, stepResult.failure);
        }
        errorCleanup(currentState);
        break;
      }
    } while (currentState.blockData.length == 512);
    
    fileWriter.close();
  }

  private static Function<TransferState, Result<TransferState, IrrecoverableError>> fileBlockWriter(FileWriter fileWriter) {
    return (state) -> {
      try {
        fileWriter.writeBlock(state.blockData);
      } catch (IOException e) {
        e.printStackTrace();
        IrrecoverableError err = new IrrecoverableError(
            ErrorCode.NOT_DEFINED, "Internal error while writing file.");
        return Result.failure(err);
      }
      return Result.success(state);
    };
  }
  
  private static void errorCleanup(TransferState transferState) {
    File f = new File(transferState.request.getFilename());
    if (f.exists()) {
      f.delete();
    }
  }
}
