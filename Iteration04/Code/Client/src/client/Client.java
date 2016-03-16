/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 *
 * @author  Loktin Wong
 * @author  Philip Klostermann
 * @version 1.0.0
 * @since 22-01-2016
 */

package client;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import configuration.ConfigurationMenu;
import packet.RequestBuilder;

import java.io.File;

import tftp_transfer.TransferState;
import tftp_transfer.TransferStateBuilder;

public class Client {
	// 2 byte range for block numbers, less block number 0
	final double MAX_FILE_SIZE = 512 * (Math.pow(2, 16) - 1);

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

		// let user choose a configuration
		new ConfigurationMenu().show();

		// TODO: for Iteration 5, ask the user for a server IP
		InetAddress serverAddress = null;
		try {
			serverAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			scan.close();
			return;
		}

		do {
			System.out.println("TFTP Client");
			System.out.println("  [ 1 ] Write file to server");
			System.out.println("  [ 2 ] Read file from server");
			System.out.println("  [ 0 ] Exit");
			System.out.print(" > ");

			command = scan.nextInt();

			String filename = "";

			switch (command) {
				case 1:
					do {
						System.out.print("Please enter a file name: ");
						filename = scan.next();
					} while (!this.validateFilename(filename));
					initiateTftpWrite(serverAddress, filename);
					break;
	
				case 2:
					// TODO: What should happen if the file already exists?
					do {
						System.out.print("Please enter a file name: ");
						filename = scan.next();
					} while (filename == null || filename.length() == 0);
					initiateTftpRead(serverAddress, filename);
					break;
			}
		} while (command != 0);

		scan.close();
	}

	private void initiateTftpWrite(InetAddress serverAddress, String filename) {
		final ServerConnection connection;
		try {
			connection = new ServerConnection(serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}

		TransferState initialState = new TransferStateBuilder()
				.connection(connection)
				.request(new RequestBuilder()
						.setFilename(filename)
						.setMode("netAsCiI")
						.buildWriteRequest())
				.build();
		TftpWriteTransfer.start(initialState);
	}

	private void initiateTftpRead(InetAddress serverAddress, String filename) {
		final ServerConnection connection;
		try {
			connection = new ServerConnection(serverAddress);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}

		TransferState initialState = new TransferStateBuilder()
				.connection(connection)
				.request(new RequestBuilder()
						.setFilename(filename)
						.setMode("ocTET")
						.buildReadRequest())
				.build();
		TftpReadTransfer.start(initialState);
	}

	private boolean validateFilename(String filename) {
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
}
