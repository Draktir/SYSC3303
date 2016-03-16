package tftp_transfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import utils.Logger;
import configuration.Configuration;
import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.ErrorPacket.ErrorCode;
import rop.ROP;
import rop.Result;
import utils.IrrecoverableError;
import utils.RecoverableError;
import utils.Recursive;

/**
 * Class containing Functions for Network Operations during File Transfer
 * 
 */

public class NetworkOperations {
	private static Logger logger = new Logger("NetworkOperations");

	public static final Function<TransferState, Result<TransferState, IrrecoverableError>> sendDataPacket = (state) -> {
		logger.log("Sending data packet, block #" + state.dataPacket.getBlockNumber());
		logger.log(state.dataPacket.toString());

		try {
			state.connection.sendPacket(state.dataPacket);
		} catch (IOException e) {
			return Result.failure(new IrrecoverableError(e.getMessage()));
		}
		return Result.success(state);
	};

	public static final Function<TransferState, Result<TransferState, IrrecoverableError>> sendAck = (state) -> {
		logger.log("Sending ACK, block #" + state.acknowledgement.getBlockNumber());
		logger.log(state.acknowledgement.toString());

		try {
			state.connection.sendPacket(state.acknowledgement);
		} catch (IOException e) {
			return Result.failure(new IrrecoverableError(e.getMessage()));
		}
		return Result.success(state);
	};

	public static final Function<TransferState, Result<TransferState, IrrecoverableError>> sendRequest = (state) -> {
		logger.log("Sending Request");
		logger.log(state.request.toString());

		try {
			state.connection.sendRequest(state.request);
		} catch (IOException e) {
			return Result.failure(new IrrecoverableError(e.getMessage()));
		}
		return Result.success(state);
	};

	// returns the current UNIX timestamp
	private static final Supplier<Long> currentTime = () -> new Date().getTime();

	private static final BiFunction<TransferState, Integer, Result<DatagramPacket, RecoverableError>> receiveDatagram = 
	(state, timeout) -> {
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
	};

	public static final Function<TransferState, Result<TransferState, IrrecoverableError>> receiveValidAck = (state) -> {
		final long tsStart = currentTime.get();
		Supplier<Integer> calculateNewTimeout = () -> {
			Long to = Configuration.TIMEOUT_TIME - (currentTime.get() - tsStart);
			return to.intValue();
		};

		final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

		final Recursive<BiFunction<Integer, Integer, Result<TransferState, IrrecoverableError>>> receiveAck = new Recursive<>();

		// receive ACK Function
		receiveAck.func = (timeout, numAttempts) -> {
			logger.log("Expecting ACK with block #" + state.blockNumber);

			Result<DatagramPacket, RecoverableError> recvResult = receiveDatagram.apply(state, timeout);

			if (recvResult.FAILURE) {
				logger.log(recvResult.failure.message);

				if (numAttempts < Configuration.MAX_RETRIES) {
					// resend the data packet and then recursively call this function
					// again
					logger.log("Re-sending last Data Packet");
					return rop.bind(state.dataPacket != null ? NetworkOperations.sendDataPacket : NetworkOperations.sendRequest)
							.andThen((s) -> receiveAck.func.apply(Configuration.TIMEOUT_TIME, numAttempts + 1))
							.apply(Result.success(state));

				} else {
					IrrecoverableError err = new IrrecoverableError("Did not receive a response. Max retries exceeded.");
					return Result.failure(err);
				}
			}

			ROP<Acknowledgement, TransferState, IrrecoverableError> ropAck = new ROP<>();

			return LocalOperations.parseAck.andThen(ropAck.bind((ack) -> {
				if (ack.getBlockNumber() > state.blockNumber) {
					// invalid block number, terminating
					IrrecoverableError err = new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION,
							"Wrong block number. Expected " + state.blockNumber + " got " + ack.getBlockNumber());
					return Result.failure(err);
				}

				if (ack.getBlockNumber() < state.blockNumber) {
					// duplicate ACK, ignore
					logger.log("Received a duplicate ACK, block# " + ack.getBlockNumber());
					logger.log(ack.toString());
					// recursively call this function to listen for another packet
					return receiveAck.func.apply(calculateNewTimeout.get(), numAttempts);
				}

				logger.log("Received valid ACK, block #" + ack.getBlockNumber());
				logger.log(ack.toString());

				return Result.success(TransferStateBuilder.clone(state).acknowledgement(ack).build());
			})).apply(recvResult.success);
		};

		return receiveAck.func.apply(Configuration.TIMEOUT_TIME, 1);
	};

	public static final Function<TransferState, Result<TransferState, IrrecoverableError>> receiveValidDataPacket = 
	(state) -> {
		final long tsStart = currentTime.get();
		Supplier<Integer> calculateNewTimeout = () -> {
			Long to = Configuration.TIMEOUT_TIME - (currentTime.get() - tsStart);
			return to.intValue();
		};

		ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

		final Recursive<BiFunction<Integer, Integer, Result<TransferState, IrrecoverableError>>> receiveDataPacket = new Recursive<>();

		// receive DataPacket Function
		receiveDataPacket.func = (timeout, numAttempts) -> {
			logger.log("Expecting DataPacket with block #" + state.blockNumber);

			Result<DatagramPacket, RecoverableError> recvResult = receiveDatagram.apply(state, timeout);

			if (recvResult.FAILURE) {
				logger.logError(recvResult.failure.message);

				if (numAttempts < Configuration.MAX_RETRIES) {
					// resend the last packet and then recursively call this function
					// again
					logger.log("Re-sending last packet");
					return rop.bind(state.acknowledgement != null ? NetworkOperations.sendAck : NetworkOperations.sendRequest)
							.andThen((s) -> receiveDataPacket.func.apply(Configuration.TIMEOUT_TIME, numAttempts + 1))
							.apply(Result.success(state));

				} else {
					IrrecoverableError err = new IrrecoverableError("Did not receive a response. Max retries exceeded.");
					return Result.failure(err);
				}
			}

			Function<DatagramPacket, Result<DataPacket, IrrecoverableError>> parseDataPacket = (dp) -> {
				return LocalOperations.parseDataPacket.apply(dp);
			};

			ROP<DataPacket, TransferState, IrrecoverableError> ropDp = new ROP<>();

			return parseDataPacket.andThen(ropDp.bind((dataPacket) -> {
				if (dataPacket.getBlockNumber() > state.blockNumber) {
					// invalid block number, terminating
					IrrecoverableError err = new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION,
							"Wrong block number. Expected " + state.blockNumber + " got " + dataPacket.getBlockNumber());
					return Result.failure(err);
				}

				if (dataPacket.getBlockNumber() < state.blockNumber) {
					// duplicate DataPacket, ignore
					logger.log("Received a duplicate DataPacket, block# " + dataPacket.getBlockNumber());
					logger.log(dataPacket.toString());

					logger.log("Sending response to duplicate DataPacket");
					// respond with an ACK with the same block number as the duplicate
					// data packet
					try {
						state.connection.sendPacket(
								new AcknowledgementBuilder().setBlockNumber(dataPacket.getBlockNumber()).buildAcknowledgement());
					} catch (Exception e) {
						e.printStackTrace();
					}

					// recursively call this function to listen for another packet
					return receiveDataPacket.func.apply(calculateNewTimeout.get(), numAttempts);
				}

				logger.log("Received valid DataPacket, block #" + dataPacket.getBlockNumber());
				logger.log(dataPacket.toString());

				return Result.success(
						TransferStateBuilder.clone(state).dataPacket(dataPacket).blockData(dataPacket.getFileData()).build());
			})).apply(recvResult.success);
		};

		return receiveDataPacket.func.apply(Configuration.TIMEOUT_TIME, 1);
	};

	public static final BiConsumer<TransferState, IrrecoverableError> sendError = (state, error) -> {
		if (error.errorCode == null) {
			logger.logError("Cannot send an error packet without an error code.");
			return;
		}

		logger.log("Sending error " + error.errorCode + " " + error.message);
		ErrorPacket err = new ErrorPacketBuilder()
				.setErrorCode(error.errorCode)
				.setMessage(error.message)
				.buildErrorPacket();

		try {
			state.connection.sendPacket(err);
		} catch (IOException e) {
			e.printStackTrace();
			logger.logError("Error occured while sending error packet.");
		}
	};
}
