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
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Scanner;

import Configuration.Configuration;
import modification.*;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.InvalidPacketException;
import packet.PacketParser;
import packet.ReadRequest;
import packet.Request;
import packet.Request.RequestType;
import packet.WriteRequest;

public class IntermediateHost {
	private DatagramSocket clientSocket;
	private DatagramSocket serverSocket;
	private PacketModifier packetModifier;
	private int clientPort;
	private PacketParser packetParser = new PacketParser();
	
	/**
	 * Main method which creates an instance of IntermediateHost to 
	 * forward and receive TFTP requests.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		IntermediateHost h = new IntermediateHost();
		boolean exit;
		Scanner scan = new Scanner(System.in);
		do {
		  h.go();
		  System.out.print("Do you want to go again? (y/n) ");
		  exit = !scan.next().equalsIgnoreCase("y");
		} while (!exit);		
		scan.close();
	}
	
	public void go() {
	  // Show the Modification configuration menu
	  ModificationMenu modMenu = new ModificationMenu();
	  // .show() returns a packetModifier based on the user's configuration
    packetModifier = modMenu.show();
    
    try {
      clientSocket = new DatagramSocket(Configuration.INTERMEDIATE_PORT);
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }
    
    byte[] buffer = new byte[1024];
    DatagramPacket requestDatagram = new DatagramPacket(buffer, buffer.length);
    
    try {
      clientSocket.receive(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    Request request = null;
    try {
      request = packetParser.parseRequest(requestDatagram);
    } catch (InvalidPacketException e1) {
      e1.printStackTrace();
      return;
    }
    
    clientPort = requestDatagram.getPort();
    
    if (request.type() == RequestType.READ) {
      serviceTftpRead((ReadRequest) request);
    } else if (request.type() == RequestType.WRITE) {
      serviceTftpWrite((WriteRequest) request);
    }
    
    System.out.println("File Transfer ended.");
    
    clientSocket.close();
	}
	
	public void serviceTftpRead(ReadRequest request) {
	  try {
      serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }
    
	  // PacketModifier figures out if the packet needs to be modified, applies the modification if
	  // applicable, and returns the packet data as a byte[].
	  byte[] requestData = packetModifier.process(request);
	  
	  // send ReadRequest
    DatagramPacket requestDatagram = new DatagramPacket(requestData, requestData.length,
        request.getRemoteHost(), Configuration.SERVER_PORT);
    try {
      serverSocket.send(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    
    int serverPort;
    byte[] recvBuffer = new byte[1024];
    
    while (true) {
      // wait for data packet
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      try {
        serverSocket.receive(dataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // parse data packet
      DataPacket dataPacket = null;
      try {
        dataPacket = packetParser.parseDataPacket(dataPacketDatagram);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        return;
      }
      
      serverPort = dataPacketDatagram.getPort();
      
      System.out.println("Received data packet:\n" + dataPacket.toString());
      
      // forward to client
      byte[] dataPacketRaw = packetModifier.process(dataPacket);
      DatagramPacket forwardPacket = new DatagramPacket(dataPacketRaw, dataPacketRaw.length,
          dataPacket.getRemoteHost(), clientPort);

      try {
        clientSocket.send(forwardPacket);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      
      // wait for ACK from client
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      
      try {
        clientSocket.receive(ackDatagram);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      
      // parse ACK
      Acknowledgement ack;
      try {
        ack = packetParser.parseAcknowledgement(ackDatagram);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        return;
      }
      
      System.out.println("Received ACK: " + ack.toString());
      
      // forward ACK to server
      byte[] ackData = packetModifier.process(ack);
      DatagramPacket forwardAckDatagram = new DatagramPacket(ackData, ackData.length,
          ack.getRemoteHost(), serverPort);
      
      try {
        serverSocket.send(forwardAckDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // TODO: figure out when to break
	    //break;
    }
	  
	  //serverSocket.close();
	}

	public void serviceTftpWrite(WriteRequest request) {
	  try {
      serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }
    
    // PacketModifier figures out if the packet needs to be modified, applies the modification if
    // applicable, and returns the packet data as a byte[].
    byte[] requestData = packetModifier.process(request);
    
    // send WriteRequest
    DatagramPacket requestDatagram = new DatagramPacket(requestData, requestData.length,
        request.getRemoteHost(), Configuration.SERVER_PORT);
    
    try {
      serverSocket.send(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    
    int serverPort;
    byte[] recvBuffer = new byte[1024];
    
    while (true) {
      // wait for ack packet
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      try {
        serverSocket.receive(ackDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // parse data packet
      Acknowledgement ack = null;
      try {
        ack = packetParser.parseAcknowledgement(ackDatagram);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        return;
      }
      
      serverPort = ackDatagram.getPort();
      
      System.out.println("Received ack:\n" + ack.toString());
      
      // forward to client
      byte[] ackPacketRaw = packetModifier.process(ack);
      DatagramPacket forwardPacket = new DatagramPacket(ackPacketRaw, ackPacketRaw.length,
          ack.getRemoteHost(), clientPort);

      try {
        clientSocket.send(forwardPacket);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      
      // wait for Data Packet from client
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      
      try {
        clientSocket.receive(dataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      
      // parse ACK
      DataPacket dataPacket;
      try {
        dataPacket = packetParser.parseDataPacket(dataPacketDatagram);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        return;
      }
      
      System.out.println("Received DataPacket:\n" + dataPacket.toString());
      
      // forward data packet to server
      byte[] dataPacketData = packetModifier.process(dataPacket);
      DatagramPacket forwardDataPacketDatagram = new DatagramPacket(dataPacketData, dataPacketData.length,
          dataPacket.getRemoteHost(), serverPort);
      
      try {
        serverSocket.send(forwardDataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      //break;
    }
    
    //serverSocket.close();
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
