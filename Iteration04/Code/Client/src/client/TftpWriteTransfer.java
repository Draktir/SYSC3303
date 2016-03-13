package client;

import configuration.Configuration;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

public class TftpWriteTransfer {
	private static final Logger logger = new Logger("TFTP-WRITE");

	public static void start(TransferState transferState) {
		// open the requested file as a lazy Stream
		final Result<Stream<byte[]>, IrrecoverableError> streamResult = FileOperations.createFileReadStream
				.apply(transferState.request.getFilename());

		if (streamResult.FAILURE) {
			NetworkOperations.sendError.accept(transferState, streamResult.failure);
			return;
		}

		final Stream<byte[]> fileBlockStream = streamResult.success;

		// create a file reader function
		final Function<TransferState, Result<TransferState, IrrecoverableError>> readFileBlock = fileReader(
				fileBlockStream.iterator());

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
			fileBlockStream.close();
			return;
		}

		TransferState currentState = reqResult.success;

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
				logger.logError("Error encountered during file transfer: " + stepResult.failure.message);
				if (stepResult.failure.errorCode != null) {
					NetworkOperations.sendError.accept(transferState, stepResult.failure);
				}
				break;
			}
		} while (currentState.blockData.length == Configuration.BLOCK_SIZE);

		logger.logAlways("Transfer has ended");
		// closing the stream also closes the file
		fileBlockStream.close();
	}

	// returns a file reader function using the provided stream iterator
	private static Function<TransferState, Result<TransferState, IrrecoverableError>> fileReader(
			Iterator<byte[]> fileBlockIterator) {
		// reads one block at a time using the iterator
		return (state) -> {
			logger.log("Reading next file block");
			byte[] fileBlock = null;
			if (fileBlockIterator.hasNext()) {
				fileBlock = fileBlockIterator.next();
			} else {
				// in case we need to send a last data packet with no data
				fileBlock = new byte[0];
			}

			return Result.success(TransferStateBuilder.clone(state)
					.blockNumber(state.blockNumber + 1)
					.blockData(fileBlock)
					.build());
		};
	}

}
