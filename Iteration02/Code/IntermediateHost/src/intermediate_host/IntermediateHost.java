package intermediate_host;
/**
 * The IntermediateHost class implements an application 
 * that acts as both a client, and a server that will 
 * receive and forward TFTP requests to their intended
 * destinations.
 * 
 * @author  Loktin Wong
 * @author  Philip Klostermann
 * @version 1.0.0
 * @since 22-01-2016
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class IntermediateHost {
	public static final int RECEIVE_PORT = 68; // This is the port used to receive packets from the client
	public static final int SERVER_PORT = 69; // This is the port used to forward the packet to the server
	
	private int serverPort;
	
	DatagramSocket srSocket, receiveSocket, clientSocket;
	DatagramPacket sendPacket, receivePacket;
	
	/**
	 * Default IntermediateHost class constructor instantiates two
	 * DatagramSockets, one of which uses the RECEIVE_PORT constant
	 * defined within the class. 
	 */
	public IntermediateHost() {
	  this.serverPort = SERVER_PORT;
	  try {
	    srSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(RECEIVE_PORT);
		}
		catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Main method which creates an instance of IntermediateHost to 
	 * forward and receive TFTP requests.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		IntermediateHost h = new IntermediateHost();
		
		while (true) {
			h.sendAndReceiveRequest();
		}
	}
	
	/**
	 * Receives requests from the receiveSocket and forwards them
	 * to the server through the srSocket. Also receives the server
	 * response to the client from the receiveSocket through a 
	 * temporary socket.
	 */
	public void sendAndReceiveRequest() {
		byte[] buffer = new byte[516];
		byte[] data = null;
		int clientPort = 0;
		
		receivePacket = new DatagramPacket(buffer, buffer.length);

		try {
			System.out.println("[SYSTEM] Waiting for request from client on port " + receiveSocket.getLocalPort());
			receiveSocket.receive(receivePacket);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		clientPort = receivePacket.getPort();
		System.out.println("Client Port: " + clientPort);
		
		
		// continue to route all packets between server and client using their temp sockets
		while (true) {
  		// Trim excess null bytes from the buffer and store it into data
  		// Makes printing look nice in console, but not needed here
  		data = new byte[receivePacket.getLength()];
  		System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
  		printRequestInformation(data);
  		
  		try {
  			sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), serverPort);
  			System.out.println("[SYSTEM] Sending request to server on port " + sendPacket.getPort());
  			printRequestInformation(data);
  			srSocket.send(sendPacket);
  		}
  		catch (UnknownHostException e) {
  			e.printStackTrace();
  			System.exit(1);
  		}
  		catch (IOException e) {
  			e.printStackTrace();
  			System.exit(1);
  		}
  		
  		buffer = new byte[516];
  		receivePacket = new DatagramPacket(buffer, buffer.length);
  		
  		try {
  			System.out.println("[SYSTEM] Waiting for response from server on port " + srSocket.getPort());
  			srSocket.receive(receivePacket);
  		}
  		catch (IOException e) {
  			e.printStackTrace();
  			System.exit(1);
  		}
  		
  		System.out.println("[SYSTEM] received response from server on port " + receivePacket.getPort());
  		// from now on we'll use the server's newly opened port
  		serverPort = receivePacket.getPort();
  		
  		// Trim excess null bytes from the buffer and store it into data
  		// Makes printing look nice in console, but not needed here
  		data = new byte[receivePacket.getLength()];
  		System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
  		printRequestInformation(data);
  		
  		try {
  			sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), clientPort);
  			clientSocket = new DatagramSocket();
  			System.out.println("[SYSTEM] Sending server response to client on port " + sendPacket.getPort());
  			printRequestInformation(data);
  			clientSocket.send(sendPacket);
  			
  			byte[] buf = new byte[516];
  			receivePacket = new DatagramPacket(buf, buf.length);
  			clientSocket.receive(receivePacket);
  		}
  		catch (UnknownHostException e) {
  			e.printStackTrace();
  			System.exit(1);
  		}
  		catch (IOException e) {
  			e.printStackTrace();
  			System.exit(1);
  		}
		}
	}
	
	/**
	 * Prints out request contents as a String and in bytes.
	 * 
	 * @param buffer
	 */
	public void printRequestInformation(byte[] buffer) {
		String contents = new String(buffer);
		
		System.out.println("\tRequest contents: ");
		System.out.println("\t" + contents);
		
		System.out.println("\tRequest contents (bytes): ");
		System.out.print("\t");
		for (int i = 0; i < buffer.length; i++) {
			System.out.print(buffer[i] + " ");
		}
		System.out.println();
	}
}
