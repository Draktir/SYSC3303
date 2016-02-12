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
import java.util.Scanner;

import Configuration.Configuration;

import java.io.File;

import file_io.FileReader;
import file_io.FileWriter;

public class Client {
	private ServerConnection serverConnection;
	private FileReader fileReader;
	private FileWriter fileWriter;
	private boolean transferComplete = false;

	/**
	 * Default Client constructor which instantiates the server Connection
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
		Scanner scan = new Scanner(System.in);
		Client client = new Client();
		int command;
		String fileName = "";

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
				} while (!client.validateFilename(fileName));
				client.sendFileToServer(fileName, "netAsCiI");
				System.out.println("[SYSTEM] Uploaded file " + fileName + " to server.");
				System.out.println("[SYSTEM] Please restart the IntermediatHost now."); // TODO:
																						// remove
																						// this
				break;
			case 2:
				do {
					System.out.print("Please enter a file name: ");
					fileName = scan.next();
				} while (fileName == null || fileName.length() == 0);
				client.downloadFileFromServer(fileName, "ocTeT");
				System.out.println("[SYSTEM] Downloaded file " + fileName + " from server.");
				System.out.println("[SYSTEM] Please restart the IntermediatHost now."); // TODO:
																						// remove
																						// this
				break;
			}
		} while (command != 0);

		scan.close();
	}

	public boolean validateFilename(String filename) {
		final double MAX_FILE_SIZE = 512 * (Math.pow(2, 16) - 1);
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
			System.out.println("The File is too big. size: " + f.length() + " max: " + MAX_FILE_SIZE);
			return false;
		}
		if (f.length() < 1) {
		  System.out.println("The File is empty. Cannot send an empty file.");
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
		transferComplete = false;
		// open file for reading
		try {
			fileReader = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			System.err.println("The file you're trying to send could not be fonud on this computer.");
			return;
		}

		InetAddress remoteHost;
		try {
			remoteHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}

		WriteRequest request = new RequestBuilder().setRemoteHost(remoteHost)
				.setRemotePort(Configuration.INTERMEDIATE_PORT).setFilename(filename).setMode(mode).buildWriteRequest();

		PacketParser parser = new PacketParser();
		DatagramPacket recvdDatagram = serverConnection.sendPacketAndReceive(request);
		
		int blockNumber = 0;
		
		do {
			Acknowledgement ack = null;
			try {
				ack = parser.parseAcknowledgement(recvdDatagram);
			} catch (InvalidAcknowledgementException e) {
				String errMsg = "Not a valid ACK: " + e.getMessage();
				handlePacketError(errMsg, recvdDatagram);
				return;
			}
			
			if (ack.getBlockNumber() != blockNumber) {
		        String errMsg = "ACK has the wrong block number, expected block #" + blockNumber;
		        System.err.println(errMsg);
		        handlePacketError(errMsg, recvdDatagram);
		        return;
		      }

			recvdDatagram = handleAcknowledgement(ack);

			blockNumber++;
			
			// TODO: format error messages properly, they currently always end successfully regardless of error.

			if (recvdDatagram != null) {
				// printPacketInformation(recvdDatagram);
			} else {
				System.out.println("[SYSTEM] File successfully transferred.");
			}

		} while (!transferComplete);

		System.out.println("[SYSTEM] File transfer ended."); // TODO: this
																// doesn't
																// really need
																// to be here.
	}

	/**
	 * Initiates downloading a file from the server (Read Request)
	 * 
	 * @param filename
	 * @param mode
	 */
	public void downloadFileFromServer(String filename, String mode) {
		transferComplete = false;
		// create file for reading
		try {
			fileWriter = new FileWriter(filename);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.err.println("Could not create new file for downloading.");
			return;
		} catch (FileAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InetAddress remoteHost;
		try {
			remoteHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}

		ReadRequest request = new RequestBuilder().setRemoteHost(remoteHost).setRemotePort(Configuration.INTERMEDIATE_PORT)
				.setFilename(filename).setMode(mode).buildReadRequest();

		PacketParser parser = new PacketParser();
		DatagramPacket recvdDatagram = serverConnection.sendPacketAndReceive(request);
		
		int blockNumber = 1;
		
		do {
			DataPacket data = null;
			try {
				data = parser.parseDataPacket(recvdDatagram);
			} catch (InvalidDataPacketException e) {
				String errMsg = "Not a valid Data Packet: " + e.getMessage();
				handlePacketError(errMsg, recvdDatagram);
				return;
			}
			
			if (data.getBlockNumber() != blockNumber) {
		        String errMsg = "Data packet has the wrong block#, expected block #" + blockNumber;
		        System.err.println(errMsg);
		        handlePacketError(errMsg, recvdDatagram);
		        return;
		      }

			recvdDatagram = handleDataPacket(data);
			
			blockNumber++;

			// TODO: format error messages properly, they currently always end successfully regardless of error.

			if (recvdDatagram != null) {
				// printPacketInformation(recvdDatagram);
			} else {
				System.out.println("[SYSTEM] File successfully transferred.");
			}

		} while (!transferComplete);

		System.out.println("[SYSTEM] File transfer ended."); // TODO: this
																// doesn't
																// really need
																// to be here.
	}

	/**
	 * Handles an incoming acknowledgement by sending the next file block
	 * 
	 * @param ack
	 * @return
	 */
	private DatagramPacket handleAcknowledgement(Acknowledgement ack) {
		System.out.println("\n\tACK received, block #" + ack.getBlockNumber());

		byte[] buffer = new byte[512];
		int bytesRead = fileReader.readNextBlock(buffer);
		
		if (bytesRead < 0) {
		  System.err.println("Could not read from the file: " + fileReader.getFilename());
		  return null;
		}

		byte[] fileData = new byte[bytesRead];
		System.arraycopy(buffer, 0, fileData, 0, bytesRead);

		System.out.println("\n\tFile data length (bytes): " + bytesRead);

		DataPacket dataPacket = new DataPacketBuilder().setRemoteHost(ack.getRemoteHost())
				.setRemotePort(ack.getRemotePort()).setBlockNumber(ack.getBlockNumber() + 1).setFileData(fileData)
				.buildDataPacket();

		printPacketInformation(dataPacket);

		// Check if we have read the whole file
		if (fileData.length < 512) {
			System.out.println("[SYSTEM] Sending last data packet.");
			transferComplete = true;
			fileReader.close();

			// send the last data packet
			serverConnection.sendPacketAndReceive(dataPacket);

			// TODO: We should make sure we get an ACK and resend the last data
			// packet
			// if it failed. Not needed for this iteration though.
			return null;
		}

		return serverConnection.sendPacketAndReceive(dataPacket);
	}

	/**
	 * Handles an incoming data packet by responding with an ACK
	 * 
	 * @param dataPacket
	 * @return
	 */
	private DatagramPacket handleDataPacket(DataPacket dataPacket) {
		System.out.println("\tDATA received, block #" + dataPacket.getBlockNumber());

		System.out.println("\tWriting file block# " + dataPacket.getBlockNumber());
		byte[] fileData = dataPacket.getFileData();
		try {
			fileWriter.writeBlock(fileData);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Acknowledgement ack = new AcknowledgementBuilder().setRemoteHost(dataPacket.getRemoteHost())
				.setRemotePort(dataPacket.getRemotePort()).setBlockNumber(dataPacket.getBlockNumber())
				.buildAcknowledgement();

		printPacketInformation(ack);

		// Check for the last data packet
		if (fileData.length < 512) {
			System.out.println("\tAcknowledging last data packet.");
			transferComplete = true;
			fileWriter.close();
			// send the last ACK
			serverConnection.sendPacket(ack);
			return null;
		}

		return serverConnection.sendPacketAndReceive(ack);
	}

	private void handlePacketError(String message, DatagramPacket requestPacket) {
		ErrorPacket errPacket = new ErrorPacketBuilder().setRemoteHost(requestPacket.getAddress())
				.setRemotePort(requestPacket.getPort()).setErrorCode(ErrorCode.ILLEGAL_TFTP_OPERATION)
				.setMessage(message).buildErrorPacket();
		serverConnection.sendPacket(errPacket);
	}

	/**
	 * Prints out request contents as a String and in bytes.
	 * 
	 * @param buffer
	 */
	public void printPacketInformation(Packet packet) {
		byte[] data = packet.getPacketData();
		String contents = new String(data); // TODO: format to handle \n in
											// string. (substrings)

		System.out.println("\n\tPacket contents: ");
		System.out.println("\t" + contents);

		System.out.println("\tPacket contents (bytes): ");
		System.out.print("\t");
		for (int i = 0; i < data.length; i++) {
			System.out.print(data[i] + " ");
		}
		System.out.println();
	}
}
