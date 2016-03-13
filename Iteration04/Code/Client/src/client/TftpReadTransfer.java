package client;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import configuration.Configuration;
import file_io.FileWriter;
import packet.ErrorPacket.ErrorCode;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

public class TftpReadTransfer {
	private static final Logger logger = new Logger("TFTP-READ");

	public static void start(TransferState transferState) {
		// create the file
		final Result<FileWriter, IrrecoverableError> fileResult = FileOperations.createFile.apply(transferState);

		if (fileResult.FAILURE) {
			NetworkOperations.sendError.accept(transferState, fileResult.failure);
			return;
		}

		final FileWriter fileWriter = fileResult.success;

		// file writer function
		final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock = fileBlockWriter(
				fileWriter);

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
				logger.logError("Error encountered during file transfer: " + stepResult.failure.message);
				if (stepResult.failure.errorCode != null) {
					NetworkOperations.sendError.accept(currentState, stepResult.failure);
				}
				errorCleanup(currentState);
				break;
			}
		} while (currentState.blockData.length == Configuration.BLOCK_SIZE);

		logger.logAlways("Transfer has ended.");
		fileWriter.close();
	}

	private static Function<TransferState, Result<TransferState, IrrecoverableError>> fileBlockWriter(
			FileWriter fileWriter) {

		return (state) -> {
			logger.log("Writing file block #" + state.blockNumber);
			try {
				fileWriter.writeBlock(state.blockData);
			} catch (IOException e) {
				e.printStackTrace();
				IrrecoverableError err = new IrrecoverableError(ErrorCode.NOT_DEFINED,
						"Internal Error while writing file. Please try again.");
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
