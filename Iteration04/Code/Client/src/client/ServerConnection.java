package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;

import configuration.Configuration;
import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.Packet;
import packet.Request;
import packet.ErrorPacket.ErrorCode;
import tftp_transfer.Connection;
import tftp_transfer.TransferId;
import utils.Logger;
import utils.PacketPrinter;

public class ServerConnection implements Connection {
	private final Logger logger = new Logger("ServerConnection");
	private final DatagramSocket socket;
	private final InetAddress serverAddress;
	private TransferId serverTid;

	public ServerConnection(InetAddress address) throws SocketException {
		this.serverAddress = address;
		this.socket = new DatagramSocket();
	}

	/**
	 * sends a request to the server always on the port configured in the
	 * Configuration (INTERMEDIATE)
	 * 
	 * @param request
	 * @throws IOException
	 */
	public void sendRequest(Request request) throws IOException {
		byte[] data = request.getPacketData();
		DatagramPacket reqDatagram = new DatagramPacket(data, data.length, serverAddress, Configuration.get().INTERMEDIATE_PORT);

		logger.logAlways("Sending request to " + serverAddress + ":" + Configuration.get().INTERMEDIATE_PORT);
		PacketPrinter.print(reqDatagram);

		socket.send(reqDatagram);
	}

	/**
	 * Sends a packet to the server
	 * 
	 * @param packet
	 * @throws IOException
	 */
	public void sendPacket(Packet packet) throws IOException {
		if (serverTid == null) {
			throw new RuntimeException("Do not know the Server's receiving port. Make sure to send a request first.");
		}

		byte[] data = packet.getPacketData();
		DatagramPacket sendDatagram = new DatagramPacket(data, data.length, serverAddress, serverTid.port);

		logger.log("[SERVER-CONNECTION] sending packet");
		PacketPrinter.print(sendDatagram);

		socket.send(sendDatagram);
	}

	public DatagramPacket receive(int timeout) throws SocketTimeoutException {
		byte[] buffer;
		DatagramPacket receiveDatagram = null;

		// set the timeout
		try {
			socket.setSoTimeout(timeout);
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}

		// receive packets until we receive a packet from the correct host
		do {
			buffer = new byte[517];
			receiveDatagram = new DatagramPacket(buffer, 517);

			logger.log("Waiting for response from server on port " + socket.getLocalPort());

			long tsStart = new Date().getTime();
			try {
				socket.receive(receiveDatagram);
			} catch (SocketTimeoutException e) {
				logger.log("Response timed out.");
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
				receiveDatagram = null;
				break;
			}
			long tsStop = new Date().getTime();

			logger.log("Packet received");
			PacketPrinter.print(receiveDatagram);

			// if this is the first time we receive from this server
			if (serverTid == null) {
				this.serverTid = new TransferId(serverAddress, receiveDatagram.getPort());
			}

			// ensure the TID is correct
			if (!serverTid.equals(new TransferId(receiveDatagram))) {
				handleInvalidTid(receiveDatagram);
				
				// recalculate the timeout
				try {
					int newTimeout = socket.getSoTimeout() - (int) (tsStop - tsStart);
					socket.setSoTimeout(newTimeout);
				} catch (SocketException e) {
					e.printStackTrace();
				}

				logger.logError("Waiting for another packet.");
				receiveDatagram = null;
			}
		} while (receiveDatagram == null);

		return receiveDatagram;
	}

	/**
	 * handles an invalid TID by sending an Error: Code 5
	 * 
	 * @param receiveDatagram
	 */
	private void handleInvalidTid(DatagramPacket receiveDatagram) {
		logger.logError("Received packet with wrong TID");
		logger.logError("  > Received:   " + receiveDatagram.getAddress() + " " + receiveDatagram.getPort());
		logger.logError("  > Expected:   " + serverTid.address + " " + serverTid.port);
		// respond to the rogue client with an appropriate error packet
		ErrorPacket errPacket = new ErrorPacketBuilder()
				.setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
				.setMessage("Your request had an invalid TID.")
				.buildErrorPacket();

		logger.logError("Sending error to client with invalid TID\n" + errPacket.toString() + "\n");

		byte[] errData = errPacket.getPacketData();
		DatagramPacket errDatagram = new DatagramPacket(errData, errData.length, receiveDatagram.getAddress(),
				receiveDatagram.getPort());

		PacketPrinter.print(errDatagram);

		try {
			socket.send(errDatagram);
		} catch (IOException e) {
			e.printStackTrace();
			logger.logError("Error sending error packet to unknown client. Ignoring this error.");
		}
	}

	public InetAddress getServerAddress() {
		return serverAddress;
	}
}
