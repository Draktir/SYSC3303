/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 * 
 * @author  Loktin Wong
 * @author  Philip Klostermann
 * @version 1.0.0
 * @since 22-01-2016
 */

// TODO: add proper documentation to all new functions, and make changes to existing documentation.

package client;

import packet.*;
import packet.ErrorPacket.ErrorCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.util.Scanner;
import configuration.Configuration;

import java.sql.Timestamp;
import java.io.File;

import file_io.FileReader;
import file_io.FileWriter;

import utils.PacketPrinter;

public class Client {
	final double MAX_FILE_SIZE = 512 * (Math.pow(2, 16) - 1); // 2 byte range
																// for block
																// numbers, less
																// block number
																// 0
	private ServerConnection serverConnection;
	private PacketParser packetParser = new PacketParser();

	/**
	 * Default Client constructor, instantiates the server Connection
	 */
	public Client() {
		try {
			serverConnection = new ServerConnection();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main method which creates an instance of Client.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new Client().start();
	}

	public void start() {
		Scanner scan = new Scanner(System.in);
		int command;
		String fileName = "";
		
		if (!Configuration.setMode())
			return;

		do {
			System.out.println("TFTP Client");
			System.out.println("  [ 1 ] Write file to server");
			System.out.println("  [ 2 ] Read file from server");
			System.out.println("  [ 0 ] Exit");
			System.out.print(" > ");

			command = scan.nextInt();

			switch (command) {
			case 1:
				do {
					System.out.print("Please enter a file name: ");
					fileName = scan.next();
				} while (!this.validateFilename(fileName));
				this.sendFileToServer(fileName, "netAsCiI");
				serverConnection.resetTid(); // reset for next connection
				break;

			case 2:
				do {
					System.out.print("Please enter a file name: ");
					fileName = scan.next();
				} while (fileName == null || fileName.length() == 0);
				this.downloadFileFromServer(fileName, "ocTeT");
				serverConnection.resetTid(); // reset for next connection
				break;
			}
		} while (command != 0);

		scan.close();
	}

	public boolean validateFilename(String filename) {
		File f = new File(filename);

		if (!f.exists()) {
			System.out.println("The file does not exist.");
			return false;
		}
		if (f.isDirectory()) {
			System.out.println("The filename you entered is a directory.");
			return false;
		}
		if (f.length() > MAX_FILE_SIZE) {
			System.out.println("The file is too big. size: " + f.length() + " max: " + MAX_FILE_SIZE);
			return false;
		}
		if (f.length() < 1) {
			System.out.println("The file is empty. Cannot send an empty file.");
			return false;
		}

		return true;
	}

	/**
	 * Initiates sending a file to the server (WriteRequest)
	 * 
	 * @param filename
	 * @param mode
	 */
	private void sendFileToServer(String filename, String mode) {
		boolean transferComplete = false;
		FileReader fileReader = null;

		InetAddress remoteHost;
		try {
			remoteHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}

		WriteRequest request = new RequestBuilder().setRemoteHost(remoteHost)
				.setRemotePort(Configuration.INTERMEDIATE_PORT).setFilename(filename).setMode(mode).buildWriteRequest();

		boolean errorOccured = false;
		Acknowledgement ack = null;
		DatagramPacket recvDatagram = null;
		int sendAttempts = 0;
		do {
			log("Sending WriteRequest to server on port " + Configuration.INTERMEDIATE_PORT);
			serverConnection.sendPacket(request);

			while (ack == null) {
				log("Expecting ACK with block #0");
				recvDatagram = serverConnection.receive();

				if (recvDatagram == null) {
					log("No response from Server");
					break;
				}

				log("Received packet");
				PacketPrinter.print(recvDatagram);

				try {
					ack = packetParser.parseAcknowledgement(recvDatagram);
				} catch (InvalidAcknowledgementException e) {
					String errMsg = "Not a valid ACK: " + e.getMessage();
					log(errMsg);
					handleParseError(errMsg, recvDatagram);
					errorOccured = true;
					return;
				}

				if (ack.getBlockNumber() != 0) {
					String errMsg = "Invalid block number. Expected #0, got #" + ack.getBlockNumber();
					sendErrorPacket(errMsg, recvDatagram);
					ack = null;
					errorOccured = true;
				}
			}
			serverConnection.setTimeOut(Configuration.TIMEOUT_TIME);
		} while (sendAttempts++ < Configuration.MAX_RETRIES && ack == null);

		// did we exceed max retries?
		if (sendAttempts >= Configuration.MAX_RETRIES) {
			log("Retried sending WriteRequest " + Configuration.MAX_RETRIES + " times. Giving up.");
			errorOccured = true;
			return;
		}

		if (errorOccured) {
			return;
		}

		log("ACK recived");
		log(ack.toString());

		// remember the server's TID
		serverConnection.setServerAddress(recvDatagram.getAddress());
		serverConnection.setServerPort(recvDatagram.getPort());

		log("opening " + filename + " for reading");
		// open file for reading
		try {
			fileReader = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			log("ERROR: The file you're trying to send could not be found.");
			return;
		}

		int blockNumber = 0;
		transferComplete = false;

		while (!errorOccured && !transferComplete) {
			serverConnection.setTimeOut(Configuration.TIMEOUT_TIME);

			blockNumber++;

			log("Reading block #" + blockNumber + " from file");

			byte[] buffer = new byte[512];
			int bytesRead = fileReader.readNextBlock(buffer);
			bytesRead = bytesRead >= 0 ? bytesRead : 0;

			byte[] fileData = new byte[bytesRead];
			System.arraycopy(buffer, 0, fileData, 0, bytesRead);

			DataPacket dataPacket = new DataPacketBuilder().setRemoteHost(ack.getRemoteHost())
					.setRemotePort(ack.getRemotePort()).setBlockNumber(ack.getBlockNumber() + 1).setFileData(fileData)
					.buildDataPacket();

			ack = null;
			sendAttempts = 0;
			do {
				log("sending data packet, block #" + dataPacket.getBlockNumber());
				log(dataPacket.toString());

				serverConnection.sendPacket(dataPacket);

				while (ack == null) {
					log("expecting ACK, block #" + blockNumber);
					long tsStart = currentTime();
					DatagramPacket responseDatagram = serverConnection.receive();
					long tsStop = currentTime();

					// will receive null if socket timed out
					if (responseDatagram == null) {
						log("Did not receive a response from the server.");
						break;
					}

					log("Received packet.");
					PacketPrinter.print(responseDatagram);

					try {
						ack = packetParser.parseAcknowledgement(responseDatagram);
					} catch (InvalidAcknowledgementException e) {
						String errMsg = "Not a valid ACK: " + e.getMessage();
						log(errMsg);
						handleParseError(errMsg, responseDatagram);
						errorOccured = true;
						break;
					}

					// check for duplicate. If the ack is duplicate, just
					// ignore.
					if (ack.getBlockNumber() < blockNumber) {
						log("Received duplicate ACK with block #" + ack.getBlockNumber());
						serverConnection.setTimeOut(tsStop - tsStart);
						ack = null;
						continue;
					} else if (ack.getBlockNumber() > blockNumber) {
						// if it's not a duplicate, send an error and terminate
						String errMsg = "ACK has the wrong block#, got #" + ack.getBlockNumber() + "expected #"
								+ blockNumber;
						log(errMsg);
						sendErrorPacket(errMsg, responseDatagram);
						errorOccured = true;
						break;
					}
				}

				// reset socket timeout for retries
				serverConnection.setTimeOut(Configuration.TIMEOUT_TIME);
			} while (sendAttempts++ < Configuration.MAX_RETRIES && ack == null);

			// did we exceed max retries?
			if (sendAttempts >= Configuration.MAX_RETRIES) {
				log("Retried sending DataPacket " + Configuration.MAX_RETRIES + " times. Giving up.");
				errorOccured = true;
				break;
			}

			if (dataPacket.getFileData().length < 512) {
				transferComplete = true;
				fileReader.close();
			}
		}

		if (fileReader != null) {
			fileReader.close();
		}

		if (errorOccured) {
			log("Error occured. No file transferred");
		} else {
			log("File transfer successful.");
		}
	}

	/**
	 * Initiates downloading a file from the server (Read Request)
	 * 
	 * @param filename
	 * @param mode
	 */
	public void downloadFileFromServer(String filename, String mode) {
		boolean transferComplete = false;
		FileWriter fileWriter = null;

		InetAddress remoteHost;
		try {
			remoteHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}

		ReadRequest request = new RequestBuilder()
				.setRemoteHost(remoteHost)
				.setRemotePort(Configuration.INTERMEDIATE_PORT)
				.setFilename(filename)
				.setMode(mode)
				.buildReadRequest();

		boolean errorOccured = false;
		DataPacket dataPacket = null;
		DatagramPacket recvDatagram = null;
		int sendAttempts = 0;

		do {
			log("Sending ReadRequest to server on port " + Configuration.INTERMEDIATE_PORT);
			serverConnection.sendPacket(request);

			while (dataPacket == null) {
				log("Expecting DataPacket with block #1");
				recvDatagram = serverConnection.receive();

				if (recvDatagram == null) {
					log("No response from Server");
					break;
				}

				log("Received packet");
				PacketPrinter.print(recvDatagram);

				try {
					dataPacket = packetParser.parseDataPacket(recvDatagram);
				} catch (InvalidDataPacketException e) {
					String errMsg = "Not a valid DataPacket: " + e.getMessage();
					log(errMsg);
					handleParseError(errMsg, recvDatagram);
					errorOccured = true;
					return;
				}

				if (dataPacket.getBlockNumber() != 1) {
					String errMsg = "Invalid block number. Expected #1, got #" + dataPacket.getBlockNumber();
					sendErrorPacket(errMsg, recvDatagram);
					dataPacket = null;
					errorOccured = true;
					break;
				}
			}
		} while (sendAttempts++ < Configuration.MAX_RETRIES && dataPacket == null);

		// did we exceed max retries?
		if (sendAttempts >= Configuration.MAX_RETRIES) {
			log("Retried sending WriteRequest " + Configuration.MAX_RETRIES + " times. Giving up.");
			errorOccured = true;
			return;
		}

		if (errorOccured) {
			return;
		}

		// remember the server's TID
		serverConnection.setServerAddress(recvDatagram.getAddress());
		serverConnection.setServerPort(recvDatagram.getPort());

		log("Received valid Data packet:\n" + dataPacket.toString() + "\n");

		// creating file
		if (fileWriter == null) {
			log("Creating file " + filename + " for writing.");
			// create file for reading
			try {
				fileWriter = new FileWriter(filename);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				log("ERROR: Could not create new file for downloading.");
				errorOccured = true;
				return;
			} catch (FileAlreadyExistsException e) {
				log("ERROR: " + filename + " already exists on this machine.");
				e.printStackTrace();
				errorOccured = true;
				return;
			} catch (IOException e) {
				log("ERROR: " + e.getMessage());
				e.printStackTrace();
				errorOccured = true;
				return;
			}
		}

		int blockNumber = 1;

		while (!transferComplete && !errorOccured) {
			log("writing block #" + blockNumber);
			try {
				fileWriter.writeBlock(dataPacket.getFileData());
			} catch (IOException e1) {
				e1.printStackTrace();
				errorOccured = true;
				break;
			}

			Acknowledgement ack = new AcknowledgementBuilder()
					.setRemoteHost(dataPacket.getRemoteHost())
					.setRemotePort(dataPacket.getRemotePort())
					.setBlockNumber(dataPacket.getBlockNumber())
					.buildAcknowledgement();
			serverConnection.sendPacket(ack);

			if (dataPacket.getFileData().length < 512) {
				transferComplete = true;
				fileWriter.close();
				break;
			}

			dataPacket = null;
			sendAttempts = 0;

			blockNumber++;

			do {
				log("Sending ACK");
				log(ack.toString());
				serverConnection.sendPacket(ack);

				while (dataPacket == null) {
					log("expecting Data, block #" + blockNumber);
					long tsStart = currentTime();
					DatagramPacket responseDatagram = serverConnection.receive();
					long tsStop = currentTime();

					// will receive null if socket timed out
					if (responseDatagram == null) {
						log("Did not receive a response from the client.");
						break;
					}

					log("Received packet.");
					PacketPrinter.print(responseDatagram);

					try {
						dataPacket = packetParser.parseDataPacket(responseDatagram);
					} catch (InvalidDataPacketException e) {
						String errMsg = "Not a valid DataPacket: " + e.getMessage();
						log(errMsg);
						handleParseError(errMsg, responseDatagram);
						errorOccured = true;
						break;
					}

					// check for duplicate. If the data is duplicate, send ACK
					// and ignore data.
					if (dataPacket.getBlockNumber() < blockNumber) {
						log("Received duplicate DataPacket with block #" + dataPacket.getBlockNumber());

						Acknowledgement ackForWrongData = new Acknowledgement(dataPacket.getRemoteHost(),
								dataPacket.getRemotePort(), dataPacket.getBlockNumber());
						serverConnection.sendPacket(ackForWrongData);
						serverConnection.setTimeOut(tsStop - tsStart);
						dataPacket = null;
						continue;
					} else if (dataPacket.getBlockNumber() > blockNumber) {
						// if it's not a duplicate, send an error and terminate
						String errMsg = "DataPacket has the wrong block#, got #" + dataPacket.getBlockNumber()
								+ "expected #" + blockNumber;
						log(errMsg);
						sendErrorPacket(errMsg, responseDatagram);
						errorOccured = true;
						break;
					}
				}

				// reset socket timeout for retries
				serverConnection.setTimeOut(Configuration.TIMEOUT_TIME);
			} while (sendAttempts++ < Configuration.MAX_RETRIES && dataPacket == null);

			// did we exceed max retries?
			if (sendAttempts >= Configuration.MAX_RETRIES) {
				log("Retried sending DataPacket " + Configuration.MAX_RETRIES + " times. Giving up.");
				errorOccured = true;
				break;
			}

		}
		while (!errorOccured && !transferComplete)

			if (fileWriter != null) {
				fileWriter.close();
			}

		if (errorOccured) {
			log("Error occured, deleting file");
			new File(request.getFilename()).delete();
		} else {
			log("File transfer successful.");
		}
	}

	private void handleParseError(String message, DatagramPacket packet) {
		// first figure out whether datagram is an Error packet
		ErrorPacket errPacket = null;
		try {
			errPacket = packetParser.parseErrorPacket(packet);
		} catch (InvalidErrorPacketException e) {
			// Nope, not an error packet, the client screwed up, send him an
			// error
			log("Invalid packet received. Sending error packet to server.\n");
			sendErrorPacket(message, packet);
			return;
		}

		// yes, we got an error packet, so we (the server) screwed up.
		log("Received an error packet: " + errPacket.getErrorCode() + "\n" + errPacket.toString() + "\n");
		log("");
		log("received ERROR " + errPacket.getErrorCode() + ": Server says\n'" + errPacket.getMessage() + "'\n");
	}

	private void sendErrorPacket(String message, DatagramPacket packet) {
		ErrorPacket errPacket = new ErrorPacketBuilder().setRemoteHost(packet.getAddress())
				.setRemotePort(packet.getPort()).setErrorCode(ErrorCode.ILLEGAL_TFTP_OPERATION).setMessage(message)
				.buildErrorPacket();

		log("Sending error packet to server:\n" + errPacket.toString() + "\n");
		serverConnection.sendPacket(errPacket);
	}

	public static long currentTime() {
		Date d = new Date();
		return new Timestamp(d.getTime()).getTime();
	}

	private void log(String message) {
		System.out.println("[CLIENT] " + message);
	}
}
