package tftp_transfer;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.Function;
import java.util.stream.Stream;

import utils.Logger;
import configuration.Configuration;
import file_io.FileReader;
import file_io.FileWriter;
import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import rop.Result;
import utils.IrrecoverableError;

/**
 * Class containing Functions for File Operations during File Transfer
 * 
 */

public class FileOperations {
	private static final Logger logger = new Logger("FileOperations");

	public static Function<TransferState, Result<FileWriter, IrrecoverableError>> createFile = (state) -> {
		FileWriter fw = new FileWriter(state.request.getFilename());
		try {
			fw.createFile();
		} catch (FileAlreadyExistsException e) {
			logger.logError("File already exists: " + e.getMessage());
			return Result.failure(new IrrecoverableError(ErrorCode.FILE_ALREADY_EXISTS, e.getMessage()));

		} catch (AccessDeniedException e) {
			logger.logError("Access denied: " + e.getMessage());
			return Result.failure(new IrrecoverableError(ErrorCode.ACCESS_VIOLATION, e.getMessage()));
		}

		return Result.success(fw);
	};

	public static Function<String, Result<Stream<byte[]>, IrrecoverableError>> createFileReadStream = (filename) -> {
		FileReader fileReader = new FileReader();
		Stream<byte[]> fileStream;
		try {
			fileStream = fileReader.read(filename, Configuration.BLOCK_SIZE);
		} catch (FileNotFoundException e) {
			logger.logError("File not found: " + e.getMessage());
			IrrecoverableError err = new IrrecoverableError(ErrorPacket.ErrorCode.FILE_NOT_FOUND,
					"Could not find file '" + filename + "'.");

			return Result.failure(err);
		} catch (AccessDeniedException e) {
			logger.logError("Access denied: " + e.getMessage());
			IrrecoverableError err = new IrrecoverableError(ErrorPacket.ErrorCode.ACCESS_VIOLATION,
					"Could not access file '" + filename + "'.");

			return Result.failure(err);
		}

		return Result.success(fileStream);
	};
}
