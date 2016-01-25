/**
 * The Server class implements an application that acts as a 
 * server that will receive and respond to TFTP requests.
 * 
 * @author  Loktin Wong
 * @version 1.0.0
 * @since 22-01-2016
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {
	public static final int SERVER_PORT = 69;
	
	DatagramSocket receiveSocket;
	
	/**
	 * Default Server class constructor instantiates a DatagramSocket
	 * which uses the the SERVER_PORT constant defined within the class.
	 */
	public Server() {
		try {
			receiveSocket = new DatagramSocket(SERVER_PORT);
		}
		catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Main method which creates an instance of Server to receive 
	 * TFTP requests.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		Server s = new Server();
		
		while (true) {
			s.receiveRequest();
		}
	}
	
	/**
	 * This method listens for DatagramPackets and blocks until
	 * one is received. Upon receiving a DatagramPacket, it calls
	 * the processRequest(DatagramPacket) method to process the 
	 * request.
	 */
	public void receiveRequest() {
		byte[] buffer = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
		
		try {
			System.out.println("[SYSTEM] Listening for requests.");
			receiveSocket.receive(receivePacket);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		processRequest(receivePacket);
	}
	
	/**
	 * This method checks a TFTP request to see if it is valid
	 * and returns a boolean valid indicating its validity.
	 * 
	 * @param buffer
	 * @return boolean
	 */
	public boolean isValidRequest(byte[] buffer) {
		boolean isValid = false;
		
		if (buffer[0] == 0 && buffer[1] == 1 || buffer[1] == 2) {
			int[] index = new int[2];
			int numOfZeroes = 0;
			
			for (int i = 2; i < buffer.length; i++) {
				if (buffer[i] == 0) 
					index[numOfZeroes++] = i;
			}
			
			int j = 0;
			byte[] requestFile = new byte[index[0] - 2];
			for (int i = 2; i < index[0]; i++) {
				requestFile[j++] = buffer[i];
			}
			
			j = 0;
			byte[] requestMode = new byte[(index[1] - index[0]) - 1];
			for (int i = index[0] + 1; i < index[1]; i++) {
				requestMode[j++] = buffer[i];
			}
			
			if (numOfZeroes == 2)
				if (checkFileName(requestFile))
					if (checkMode(requestMode))
						isValid = true;
		}
		else {
			isValid = false;
		}
		return isValid;
	}
	
	/**
	 * Checks to see if the file name is valid (for our purposes a 
	 * request has a valid file name as long as it contains at least
	 * one character).
	 * 
	 * @param file
	 * @return boolean
	 */
	public boolean checkFileName(byte[] file) {
		boolean validFileName = false;
		
		// As long as a file name exists, it's valid
		if (file.length > 0)
			validFileName = true;
		
		return validFileName;
	}
	
	/**
	 * Checks to see if the mode in the TFTP request is valid.
	 * 
	 * @param mode
	 * @return boolean
	 */
	public boolean checkMode(byte[] mode) {
		boolean validMode = false;
		String strMode = new String(mode);
		
		if (strMode.length() == 8)
			if (strMode.toLowerCase().equals("netascii")) 
				validMode = true;
		
		if (strMode.length() == 5)
			if (strMode.toLowerCase().equals("octet"))
				validMode = true;
			
		return validMode;
	}

	/**
	 * Processes a received DatagramPacket by testing it's contents
	 * and responds appropriately.
	 *  
	 * @param packet
	 */
	public void processRequest(DatagramPacket packet) {
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
		printRequestInformation(data);			
		
		System.out.println("[SYSTEM] Validating request.");
		
		if (isValidRequest(data)) {
			byte[] header = new byte[4];
			if (data[0] == 0 && data[1] == 1) {
				// DATA header
				header[0] = 0;
				header[1] = 3;
				header[2] = 0;
				header[3] = 1;
			}
			else if (data[0] == 0 && data[1] == 2) {
				// ACK header
				header[0] = 0;
				header[1] = 4;
				header[2] = 0;
				header[3] = 0;
			}
			
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		
			try {
				outStream.write(header);
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			data = outStream.toByteArray();
			
			try {
				DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
				DatagramSocket tempSock = new DatagramSocket();
				System.out.println("[SYSTEM] Sending response to client at port " + packet.getPort());
				printRequestInformation(data);
				tempSock.send(sendPacket);
				tempSock.close();
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("[SYSTEM] End of request reached.");
		}
		else {
			// TODO: throw exception and quit
			System.out.println("[ERROR] Invalid request received.");
			System.exit(1);
		}
	}
	
	/**
	 * Prints out request contents as a String and in bytes.
	 * 
	 * @param buffer
	 */
	public void printRequestInformation(byte[] buffer) {
		String contents = new String(buffer);
		
		System.out.println("Request contents: ");
		System.out.println(contents);
		
		System.out.println("Request contents (bytes): ");
		for (int i = 0; i < buffer.length; i++) {
			System.out.print(buffer[i] + " ");
		}
		System.out.println();
	}
}
