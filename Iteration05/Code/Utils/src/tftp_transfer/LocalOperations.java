package tftp_transfer;

import java.net.DatagramPacket;
import java.util.function.Function;

import packet.*;
import packet.ErrorPacket.ErrorCode;
import rop.Result;
import utils.IrrecoverableError;
import utils.Logger;

/**
 * Class containing Functions for Local Operations during File Transfer
 * 
 */

public class LocalOperations {
	private static final Logger logger = new Logger("LocalOperations");

	public static Function<TransferState, TransferState> buildDataPacket = (state) -> {
		DataPacket dp = new DataPacketBuilder()
				.setBlockNumber(state.blockNumber)
				.setFileData(state.blockData)
				.buildDataPacket();

		return TransferStateBuilder.clone(state).dataPacket(dp).build();
	};

	public static Function<TransferState, TransferState> buildAck = (state) -> {
		Acknowledgement ack = new AcknowledgementBuilder().setBlockNumber(state.blockNumber).buildAcknowledgement();

		return TransferStateBuilder.clone(state).acknowledgement(ack).build();
	};

	public static Function<DatagramPacket, Result<DataPacket, IrrecoverableError>> parseDataPacket = (datagram) -> {
		PacketParser parser = new PacketParser();
		DataPacket dataPacket = null;
		try {
			dataPacket = parser.parseDataPacket(datagram);
		} catch (InvalidDataPacketException de) {
			ErrorPacket errPacket = null;
			try {
				errPacket = parser.parseErrorPacket(datagram);
			} catch (InvalidErrorPacketException ee) {
				logger.log("Received an invalid packet: " + de.getMessage());
				return Result.failure(
						new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION, "Expected a DataPacket. " + de.getMessage()));
			}
			return Result.failure(
					new IrrecoverableError("Error received, code " + errPacket.getErrorCode().getValue() + ", " + errPacket.getErrorCode() + ": " + errPacket.getMessage()));
		}
		return Result.success(dataPacket);
	};

	public static Function<DatagramPacket, Result<Acknowledgement, IrrecoverableError>> parseAck = (datagram) -> {
		PacketParser parser = new PacketParser();
		Acknowledgement ack = null;
		try {
			ack = parser.parseAcknowledgement(datagram);
		} catch (InvalidAcknowledgementException ae) {
			// try to parse it as an error
			ErrorPacket errPacket = null;
			try {
				errPacket = parser.parseErrorPacket(datagram);
			} catch (InvalidErrorPacketException ee) {
				logger.log("Received an invalid packet: " + ae.getMessage());
				return Result.failure(new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION,
						"Expected an Acknowledgement. " + ae.getMessage()));
			}
			return Result.failure(
					new IrrecoverableError("Error received, code " + errPacket.getErrorCode().getValue() + ", " + errPacket.getErrorCode() + ": " + errPacket.getMessage()));
		}
		return Result.success(ack);
	};

	public static Function<DatagramPacket, Result<Request, IrrecoverableError>> parseRequest = (datagram) -> {
		PacketParser parser = new PacketParser();
		Request req = null;
		try {
			req = parser.parseRequest(datagram);
		} catch (InvalidRequestException e) {
			return Result.failure(new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION, e.getMessage()));
		}
		return Result.success(req);
	};
}
