package server;

import java.nio.file.Paths;
import java.util.function.Function;

import configuration.Configuration;
import file_io.FileReader;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

public class TftpReadTransfer {
  private static final Logger logger = new Logger("TFTP-READ");
  
  public static void start(TransferState transferState) {
    // prepare to read from the requested file
    final Result<FileReader, IrrecoverableError> fileResult = FileOperations.createFileReader.apply(
    		Paths.get(Configuration.get().FILE_PATH).resolve(transferState.request.getFilename()).toString());

    if (fileResult.FAILURE) {
    	if (fileResult.failure.errorCode != null) {
				NetworkOperations.sendError.accept(transferState, fileResult.failure);
			}
    	return;
    }

    final FileReader fileReader = fileResult.success;

    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

    // create a file block reader function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> readFileBlock = 
    		FileOperations.createFileBlockReader(fileReader);
    
    boolean transferSuccess = true;
    
    // send file, block by block
    do {
      Result<TransferState, IrrecoverableError> stepResult =
          readFileBlock
          .andThen(rop.map(LocalOperations.buildDataPacket))
          .andThen(rop.bind(NetworkOperations.sendDataPacket))
          .andThen(rop.bind(NetworkOperations.receiveValidAck))
          .apply(transferState);

      if (stepResult.SUCCESS) {
        transferState = stepResult.success;
      } else {
        logger.logError("Error encountered during file transfer: " + stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          NetworkOperations.sendError.accept(transferState, stepResult.failure);
        }
        transferSuccess = false;
        break;
      }
    } while(transferState.blockData.length == Configuration.get().BLOCK_SIZE);
    
    logger.log("Transfer ended.");
    
    if (transferSuccess) {
    	logger.logAlways("File " + transferState.request.getFilename() + " sent successfully.");
    } else {
    	logger.logError("Error occured. No file transferred.");
    }
    
    fileReader.close();
  }
}
