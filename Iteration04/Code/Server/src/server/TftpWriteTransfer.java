package server;

import java.io.File;
import java.util.function.Function;

import configuration.Configuration;
import file_io.FileWriter;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

public class TftpWriteTransfer {
  private static final Logger logger = new Logger("TFTP-WRITE");

  public static void start(TransferState transferState) {
    // create the file
    final Result<FileWriter, IrrecoverableError> fileResult = FileOperations.createFile.apply(
    		Configuration.get().SERVER_PATH + transferState.request.getFilename());
    
    if (fileResult.FAILURE) {
    	if (fileResult.failure.errorCode != null) {
				NetworkOperations.sendError.accept(transferState, fileResult.failure);
			}
    	errorCleanup(transferState);
    	return;
    }

    final FileWriter fileWriter = fileResult.success;
    
    // create a file writer function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock = 
    		FileOperations.createFileBlockWriter(fileWriter);

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
      logger.logError("Sending ACK failed.");
      if (ackResult.failure.errorCode != null) {
        NetworkOperations.sendError.accept(transferState, ackResult.failure);
      }
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
          .andThen(rop.map((state) -> {
            // advance block number
            return TransferStateBuilder.clone(state)
                .blockNumber(state.blockNumber + 1)
                .build();
          }))
          .apply(currentState);
      
      if (stepResult.SUCCESS) {
        currentState = stepResult.success;
      } else {
        logger.logError("Error encountered during file transfer.");
        logger.logError(stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          NetworkOperations.sendError.accept(currentState, stepResult.failure);
        }
        errorCleanup(currentState);
        break;
      }
    } while (currentState.blockData.length == 512);
    
    logger.logAlways("Transfer has ended");
    fileWriter.close();
  }

  private static void errorCleanup(TransferState transferState) {
    File f = new File(transferState.request.getFilename());
    if (f.exists()) {
      f.delete();
    }
  }
}
