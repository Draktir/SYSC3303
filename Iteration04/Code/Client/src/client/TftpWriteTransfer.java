package client;

import configuration.Configuration;
import file_io.FileReader;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

import java.nio.file.Paths;
import java.util.function.Function;

public class TftpWriteTransfer {
	private static final Logger logger = new Logger("TFTP-WRITE");

	public static void start(TransferState transferState) {
		// prepare to read the requested file
		final Result<FileReader, IrrecoverableError> fileResult = FileOperations.createFileReader.apply(
				Paths.get(Configuration.get().CLIENT_PATH).resolve(transferState.request.getFilename()).toString());

		if (fileResult.FAILURE) {
			// we haven't talked to the server yet, so no need to send an error
			logger.logError("Error while preparing file. " + fileResult.failure.message);
			return;
		}

		final FileReader fileReader = fileResult.success;

		// create a file reader function
		final Function<TransferState, Result<TransferState, IrrecoverableError>> readFileBlock = 
				FileOperations.createFileBlockReader(fileReader);
				
		// create an ROP helper (for error handling)
		final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

		// send the request
		final Result<TransferState, IrrecoverableError> reqResult = NetworkOperations.sendRequest
				.andThen(rop.map((state) -> {
					return TransferStateBuilder.clone(state)
							.blockNumber(0)
							.build();
				}))
				.andThen(rop.bind(NetworkOperations.receiveValidAck))
				.apply(transferState);

		if (reqResult.FAILURE) {
			logger.logError("Sending request to server failed: " + reqResult.failure.message);
			if (reqResult.failure.errorCode != null) {
				NetworkOperations.sendError.accept(transferState, reqResult.failure);
			}
			fileReader.close();
			return;
		}

		TransferState currentState = reqResult.success;		
		logger.logAlways("Successfully sent request to server.");

		// send file, block by block
		do {
			Result<TransferState, IrrecoverableError> stepResult = readFileBlock
					.andThen(rop.map(LocalOperations.buildDataPacket))
					.andThen(rop.bind(NetworkOperations.sendDataPacket))
					.andThen(rop.bind(NetworkOperations.receiveValidAck))
					.apply(currentState);

			if (stepResult.SUCCESS) {
				currentState = stepResult.success;
			} else {
				logger.logError("Error encountered during file transfer.");
				logger.logError(stepResult.failure.message);
				if (stepResult.failure.errorCode != null) {
					NetworkOperations.sendError.accept(transferState, stepResult.failure);
				}
				break;
			}
		} while (currentState.blockData.length == Configuration.get().BLOCK_SIZE);

		logger.logAlways("Transfer has ended");
		fileReader.close();
	}
}
