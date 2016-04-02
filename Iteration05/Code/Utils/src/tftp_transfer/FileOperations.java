package tftp_transfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.Consumer;
import java.util.function.Function;

import configuration.Configuration;
import file_io.DiskFullException;
import file_io.FileReader;
import file_io.FileWriter;
import packet.ErrorPacket.ErrorCode;
import rop.Result;
import utils.IrrecoverableError;
import utils.Logger;

/**
 * Class containing Functions for File Operations during File Transfer
 * 
 */

public class FileOperations {
	private static final Logger logger = new Logger("FileOperations");

	public static Function<String, Result<FileWriter, IrrecoverableError>> createFile = (filepath) -> {
		FileWriter fw = new FileWriter(filepath);
		
		logger.log("Creating file " + filepath + " for writing.");
		
		try {
			fw.createFile();
		} catch (FileAlreadyExistsException fae) {
			return Result.failure(new IrrecoverableError(ErrorCode.FILE_ALREADY_EXISTS, "File already exists"));
		} catch (AccessDeniedException ade) {
			return Result.failure(new IrrecoverableError(ErrorCode.ACCESS_VIOLATION, "Insufficient permissions to create file."));
		}
		
		return Result.success(fw);
	};
	
	public static Consumer<String> deleteFile = (filepath) -> {
		File f = new File(filepath);
		logger.log("Deleting " + filepath);
		if (f.exists()) {
		  RandomAccessFile raf = null;
		  try {
		    raf = new RandomAccessFile(filepath, "rw");
		  } catch (IOException e) {
		    logger.log("Error deleting file " + filepath);
		    return;
		  }
		  
			FileLock lock = null;
	    try {
	      lock = raf.getChannel().lock();
	    } catch (IOException e) {
        logger.log("File is in use, cannot delete it.");
        return;
	    } catch (OverlappingFileLockException e) {
	      logger.log("File is in use, cannot delete it.");
	      return;
	    } finally {
	      try {
	        if (lock != null) {
	          lock.release();
	        }
	        if (raf != null) {
	          raf.close();
	        }
	      } catch (IOException e) {} 
	    }
	    
		  f.delete();
		}
	};
	
	/**
	 * returns a function that writes blocks to file using the provided FileWriter
	 * 
	 * @param fileWriter
	 * @return Function<TransferState, Result<TransferState, IrrecoverableError>>
	 */
	public static Function<TransferState, Result<TransferState, IrrecoverableError>> createFileBlockWriter(
			FileWriter fileWriter) {

		return (state) -> {
			logger.log("Writing file block #" + state.blockNumber);
			try {
				fileWriter.writeBlock(state.blockData);
			} catch (FileAlreadyExistsException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.FILE_ALREADY_EXISTS, e.getMessage()));
			} catch (AccessDeniedException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.ACCESS_VIOLATION, e.getMessage()));
			} catch (FileNotFoundException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.FILE_NOT_FOUND, e.getMessage()));
			} catch (DiskFullException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.DISK_FULL_OR_ALLOCATION_EXCEEDED, e.getMessage()));
			}
			return Result.success(state);
		};
	}

	public static Function<String, Result<FileReader, IrrecoverableError>> createFileReader = (filepath) -> {
		FileReader fr = null;
		
		logger.log("Preparing to read file " + filepath);
		
		try {
			fr = new FileReader(filepath);
		} catch (FileNotFoundException fnf) {
			return Result.failure(new IrrecoverableError(ErrorCode.FILE_NOT_FOUND, fnf.getMessage()));
		} catch (AccessDeniedException ade) {
			return Result.failure(new IrrecoverableError(ErrorCode.ACCESS_VIOLATION, ade.getMessage()));
		}
		
		return Result.success(fr);
	};
	
  /**
   * returns a file block reader function using the provided FileReader object
   * 
   * @param FileReader
   * @return Function<TransferState, Result<TransferState, IrrecoverableError>>
   */
	public static Function<TransferState, Result<TransferState, IrrecoverableError>> createFileBlockReader (
			FileReader fr) {
		// reads one block at a time
		return (state) -> {
			logger.log("Reading file block #" + (state.blockNumber + 1));
			
			byte[] read = null;
			try {
				read = fr.read(Configuration.get().BLOCK_SIZE);
			} catch (AccessDeniedException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.ACCESS_VIOLATION, e.getMessage()));
			} catch (FileNotFoundException e) {
				return Result.failure(new IrrecoverableError(ErrorCode.FILE_NOT_FOUND, e.getMessage()));
			}
			
			return Result.success(TransferStateBuilder.clone(state)
					.blockData(read)
					.blockNumber(state.blockNumber + 1)
					.build());
		};
	}
}
