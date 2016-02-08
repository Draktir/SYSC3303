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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import modification.*;

public class IntermediateHost {
	public static final int RECEIVE_PORT = 6800; // This is the port used to receive packets from the client
	public static final int SERVER_PORT = 6900; // This is the port used to forward the packet to the server
	
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
		
		// Do we need to be able to modify Error Packets?
		// Can we use a GUI, since the IntermediateHost is not multi-threaded?
		
		
		List<Modification> modifications = new ArrayList<>();
		Scanner scan = new Scanner(System.in);
		int menuSelection = -1;
		
		// main menu
		System.out.println("TFTP Error Simulator");
		System.out.println(" -- DUMMY MENU -- remove when implemented -- ");
		System.out.println("\nWhich type of packet do you want to modify.");
		System.out.println("  [ 1 ] Read Request (RRQ)");
		System.out.println("  [ 2 ] Write Request (WRQ)");
		System.out.println("  [ 3 ] Data Packet (DATA)");
		System.out.println("  [ 4 ] Acknowledgement (ACK)");
		System.out.println("  [ 5 ] Error Packet (ERROR)");
		System.out.println("  [ 6 ] Other (modify the i-th packet)");
		System.out.println("  [ 0 ] Make no modification");
		System.out.print(" > ");
		
		menuSelection = scan.nextInt();
		
		/*
		
		// ReadRequest
		System.out.print("\nWhich one of the ReadRequests do you want to modify? #");
		
		System.out.println("Which field do you want to modify?");
		System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
		System.out.println("  [ 2 ] Filename");
		System.out.println("  [ 3 ] zero-byte after filename");
		System.out.println("  [ 3 ] Mode");
		System.out.println("  [ 4 ] zero-byte after mode");
		System.out.println(" > ");
		
		// WriteRequest
		System.out.print("\nWhich one of the WriteRequests do you want to modify? #");
		
    System.out.println("Which field do you want to modify?");
    System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
    System.out.println("  [ 2 ] Filename");
    System.out.println("  [ 3 ] zero-byte after filename");
    System.out.println("  [ 3 ] Mode");
    System.out.println("  [ 4 ] zero-byte after mode");
    System.out.println(" > ");
    
    
    // DataPacket
		System.out.print("Which one of the DataPackets do you want to modify? #");
		
    System.out.println("Which field do you want to modify?");
    System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
    System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
    System.out.println("  [ 3 ] Data");
    System.out.println(" > ");
		*/
    
		
		if (menuSelection == 4) {
		  int packetNumber;
		  // Acknowledgement
	    System.out.print("Which one of the Acknowledgements do you want to modify? #");
	    packetNumber = scan.nextInt();
	    
      AcknowledgementModification ackMod = new AcknowledgementModification(packetNumber);
	    
	    int fieldSelection = -1;
	    while (fieldSelection != 0) {
	      System.out.println("Enter your modifications");
	      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
	      System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
	      System.out.println("  [ 0 ] Done");
	      System.out.print(" > ");
	      
	      fieldSelection = scan.nextInt();
	      
	      System.out.println("How do you want to modify the field?");
        System.out.println("  [ 1 ] Replace with bytes");
        System.out.println("  [ 2 ] Replace with int");
        System.out.println("  [ 3 ] Replace with string");
        System.out.println("  [ 4 ] Remove field");
        System.out.print(" > ");
	       
	      int modType = scan.nextInt(); 
	      byte[] modValue;
	      
	      Scanner modScanner = new Scanner(System.in);
	      
	      switch (modType) {
	        case 1:
	          System.out.print("Enter bytes separated by spaces: ");
	          String bytesStr = modScanner.nextLine();
	          
	          String[] splitBytes = bytesStr.split(" ");
	          modValue = new byte[splitBytes.length];
	          
	          for (int i = 0; i < splitBytes.length; i++) {
	            BigInteger integer = new BigInteger(splitBytes[i]);
	            modValue[i] = integer.byteValue();
	          }
	          break;
	          
	        case 2:
	          System.out.print("Enter your int: ");
	          BigInteger newInt = BigInteger.valueOf(scan.nextInt());
	          modValue = new byte[] { newInt.byteValue() };
	          break;
	          
	        case 3:
	          System.out.print("Enter your string: ");
	          String str = scan.nextLine();
	          modValue = str.getBytes();
	          break;
	          
	        case 4:
	          modValue = new byte[0];
	          break;
	          
          default:
            System.err.println("Invalid selection");
            continue;
	      }

	      switch (fieldSelection) {
	        case 1:
	          ackMod.setOpcode(modValue);
	          break;
	        case 2:
	          ackMod.setBlockNumber(modValue);
	          break;
          default:
            System.err.println("Invalid field selection");
            break;
	      }
	      
	      System.out.println("\n");
	      System.out.println(ackMod);
	    }
	    
	    
		}

    
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
