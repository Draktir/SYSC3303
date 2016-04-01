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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import configuration.Configuration;
import configuration.ConfigurationMenu;
import packet.RequestBuilder;

import java.io.File;

import tftp_transfer.TransferState;
import tftp_transfer.TransferStateBuilder;

public class Client {
	private Scanner scan = new Scanner(System.in);
	// 2 byte range for block numbers, less block number 0
	private final double MAX_FILE_SIZE = Configuration.get().BLOCK_SIZE * (Math.pow(2, 16) - 1);

	/**
	 * Main method which creates an instance of Client.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		new Client().start();
	}

	public void start() {
		int command;

		// let user choose a configuration
		new ConfigurationMenu().show();

		// TODO: for Iteration 5, ask the user for a server IP
		InetAddress serverAddress = null;
		try {
			serverAddress = InetAddress.getByAddress(
					new byte[] {(byte) 127, (byte) 0, (byte) 0, (byte) 1});
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

			try {
			  command = scan.nextInt();
			} catch (InputMismatchException e) {
			  command = -1;
			}

			switch (command) {
				case 1:
					initiateTftpWrite(serverAddress);
					break;
				case 2:
					initiateTftpRead(serverAddress);
					break;
			}
		} while (command != 0);

		scan.close();
	}

	private void initiateTftpWrite(InetAddress serverAddress) {
		File selectedFile = userSelectFile(getClientFilePath());
		
		if (selectedFile == null) {
			return;
		}
		
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
						.setFilename(selectedFile.getName())
						.setMode("netAsCiI")
						.buildWriteRequest())
				.build();
		TftpWriteTransfer.start(initialState);
	}

	private void initiateTftpRead(InetAddress serverAddress) {
		String filename = userEnterFilename(getClientFilePath());
		
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

	private Path getClientFilePath() {
		if (Configuration.get().FILE_PATH != null && Configuration.get().FILE_PATH.length() > 0) {
			return Paths.get(Configuration.get().FILE_PATH);
		} else {
			return Paths.get(System.getProperty("user.dir"));
		}
	}
	
	private File userSelectFile(Path path) {
		if (path.toFile().listFiles() == null) {
		  System.err.println("You do not have permission to read from " + path.toString());
		  return null;
		}
	  
	  // show a list of all files in the client directory
		List<File> files = Arrays.asList(path.toFile().listFiles())
			.stream()
			.filter((f) -> !f.isDirectory())
			.sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
			.collect(Collectors.toList());
		
		if (files.size() == 0) {
			System.out.println("\nThere are no files in " + path.toString() + "\n");
			return null;
		}
		
		System.out.println("\nPlease select a file from " + path.toString());
		System.out.println("  [0] Enter a filename by hand.");
		for (int i = 0; i < files.size(); i++) {
			System.out.println("  [" + (i+1) + "] " + files.get(i).getName());
		}
		
		int selected = -1;
		do {
			System.out.print(" > ");
			try {
				selected = scan.nextInt();
			} catch (InputMismatchException e) {
				scan.nextLine();
				selected = -1;
			}
			
		} while (selected < 0 || selected > files.size());
		
		if (selected == 0) {
		  System.out.print("Please enter a filename: ");
		  String fn = scan.next();
		  File f = new File(fn);
		  return validateWriteFile(f) ? f : null;
		} else {
		  return validateWriteFile(files.get(selected - 1)) ? files.get(selected - 1) : null;
		}
	}
	
	
	private boolean validateWriteFile(File file) {
		if (!file.exists()) {
			System.out.println("The file does not exist.");
			return false;
		}
		if (!file.canRead()) {
		  System.out.println("You do not have read persmissions for this file.");
		}
		if (file.isDirectory()) {
			System.out.println(file.toString() + " is a directory. Cannot send a directory");
			return false;
		}
		if (file.length() > MAX_FILE_SIZE) {
			System.out.println("The file is too big. size: " + file.length() + " max: " + MAX_FILE_SIZE);
			return false;
		}
		if (file.length() < 1) {
			System.out.println("The file is empty. Cannot send an empty file.");
			return false;
		}

		return true;
	}
	
	private String userEnterFilename(Path path) {
		String filename = null;
		
		do {
			System.out.print("Please enter a filename: ");
			filename = scan.next();
			
			File f = new File(path.resolve(filename).toString());
			if (f.exists()) {
				System.out.println("A file with that name already exists in " + path.toString());
				filename = null;
			}
			
		} while (filename == null || filename.equals(""));
		
		return filename;
	}
}
