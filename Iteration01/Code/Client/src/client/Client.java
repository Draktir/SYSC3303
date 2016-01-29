/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 * 
 * @author  Loktin Wong
 * @version 1.0.0
 * @since 22-01-2016
 */

// TODO: add proper documentation to all new functions, and make changes to existing documentation.

package client;
import packet.*;
import client.FileReader;
import client.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	public static final int SERVER_PORT = 68;
	
	FileReader fileReader;
	FileWriter fileWriter;
	
	DatagramSocket srSocket;
	DatagramPacket sendPacket, receivePacket;
	
	/**
	 * Default Client constructor which instantiates a 
	 * DatagramSocket on an open port on the local machine
	 */
	public Client() {
		try {
			srSocket = new DatagramSocket();
		}
		catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Main method which creates an instance of Client to
	 * sends alternating read and write TFTP requests to 
	 * a server on the local machine at the SERVER_PORT port.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Client c = new Client();
		
		// Assume we only need to read, and write to a file.
		
		c.sendRequest(1, "in.dat", "NeTaScIi");
		c.sendRequest(2, "out.dat", "oCtEt");
	}
	
	/**
	 * Sends a TFTP request to a TFTP server based on the type
	 * of request.
	 * 
	 * @param type used to determine what type of request should be sent
	 * @param file name of the file the client is requesting
	 * @param mode the mode in which a file is encoded
	 */	
	public void sendRequest(int type, String file, String mode) {
		DatagramPacket requestPacket = null;
		
		System.out.println("[SYSTEM] Sending request to server at port " + SERVER_PORT + ".");
		
		try {
			switch (type) {
			case 1: // RRQ
				ReadRequest rrq = new ReadRequest(InetAddress.getLocalHost(), SERVER_PORT, InetAddress.getLocalHost(), srSocket.getLocalPort(), file, mode);
				requestPacket = new DatagramPacket(rrq.getPacketData(), rrq.getPacketData().length, InetAddress.getLocalHost(), SERVER_PORT);
				srSocket.send(requestPacket);
				System.out.println("[SYSTEM] Waiting for response from server.");
				receiveResponse(1); // Receive response from server here, and respond appropriately
				break;
			case 2: // WRQ
				WriteRequest wrq = new WriteRequest(InetAddress.getLocalHost(), SERVER_PORT, InetAddress.getLocalHost(), srSocket.getLocalPort(), file, mode);
				requestPacket = new DatagramPacket(wrq.getPacketData(), wrq.getPacketData().length, InetAddress.getLocalHost(), SERVER_PORT);
				srSocket.send(requestPacket);
				System.out.println("[SYSTEM] Waiting for response from server.");
				receiveResponse(2); // Receive response from server here, and respond appropriately 
				break;
			default:
				break;
			}
		}
		catch (UnknownHostException e) {
			// This should not happen in Iteration 1.
			e.printStackTrace();
		}
		catch (IOException e) {
			// An error occurred while sending packet out of srSocket.
			e.printStackTrace();
		}
		
		System.out.println("[SYSTEM] End of request.");
	}
	
	public void receiveResponse(int requestTypeSent) 
	{
		DatagramPacket responsePacket = null;
		
		byte[] buffer = new byte[516];
		responsePacket = new DatagramPacket(buffer, buffer.length);
		
		try {
			srSocket.receive(responsePacket);
		} catch (IOException e) {
			// An error occurred while receiving a packet from srSocket.
			e.printStackTrace();
		}
		
		/*
		 *  Ideally we should check the packet for the appropriate type before handling it,
		 *  but in this case we know what to expect in response to a particular request.
		 *  
		 *  Phil, I'm not too familiar with your packet package, so perhaps you could finish
		 *  the rest of this up.
		 *  
		 *  TODO: Implement the rest of this.
		 */
		
		switch (requestTypeSent) {
			case 1: // RRQ sent, should receive DATA
				writeDataToFile(responsePacket);
				break;
			case 2: // WRQ sent, should receive ACK
				sendDataFromFile(responsePacket);
				break;
		}
	}
	
	public void writeDataToFile(DatagramPacket packet) {
		// TODO: implement FileWriter to write to the appropriate file
	}
	
	public void sendDataFromFile(DatagramPacket packet) {
		// TODO: implement FileReader to read from the appropriate file
		
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
