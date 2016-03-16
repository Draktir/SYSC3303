package client;

import java.nio.file.Paths;
import java.util.function.Function;

import configuration.Configuration;
import file_io.FileWriter;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

public class TftpReadTransfer {
	private static final Logger logger = new Logger("TFTP-READ");

	public static void start(TransferState transferState) {
		// create the file
		final Result<FileWriter, IrrecoverableError> fileResult = FileOperations.createFile.apply(
				Paths.get(Configuration.get().CLIENT_PATH).resolve(transferState.request.getFilename()).toString());

		if (fileResult.FAILURE) {
			// we haven't talked to the server yet, so no need to send an error
			logger.logError("Error while creating file. " + fileResult.failure.message);
			errorCleanup(transferState);
			return;
		}

		final FileWriter fileWriter = fileResult.success;

		// file writer function
		final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock = 
				FileOperations.createFileBlockWriter(fileWriter);

		// create an ROP helper (for error handling)
		final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

		// send the request
		final Result<TransferState, IrrecoverableError> reqResult = NetworkOperations.sendRequest
				.andThen(rop.map((state) -> {
					return TransferStateBuilder.clone(state)
							.blockNumber(1)
							.build();
				}))
				.apply(transferState);

		if (reqResult.FAILURE) {
			logger.logError("Sending request to server failed: " + reqResult.failure.message);
			if (reqResult.failure.errorCode != null) {
				NetworkOperations.sendError.accept(transferState, reqResult.failure);
			}
			errorCleanup(transferState);
			fileWriter.close();
			return;
		}

		TransferState currentState = reqResult.success;

		// perform the file transfer
		do {
			Result<TransferState, IrrecoverableError> stepResult = NetworkOperations.receiveValidDataPacket
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
		} while (currentState.blockData.length == Configuration.get().BLOCK_SIZE);

		logger.logAlways("Transfer has ended.");
		fileWriter.close();
	}

	private static void errorCleanup(TransferState transferState) {
		FileOperations.deleteFile.accept(
				Configuration.get().CLIENT_PATH + transferState.request.getFilename());
	}
}
