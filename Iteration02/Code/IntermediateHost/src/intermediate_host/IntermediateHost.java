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
import java.util.Scanner;

import Configuration.Configuration;
import modification.*;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.InvalidPacketException;
import packet.Packet;
import packet.PacketParser;
import packet.ReadRequest;
import packet.Request;
import packet.Request.RequestType;
import packet.WriteRequest;

public class IntermediateHost {
	private DatagramSocket clientSocket;
	private DatagramSocket serverSocket;
	private PacketModification modification;
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
	  // allow user to configure error simulation
	  ModificationMenu modMenu = new ModificationMenu();
    modification = modMenu.show();
    
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
	  // set up our packet counters
	  int packetCount = 0;
	  int ackCount = 0;
	  int dataCount = 0;
	  
	  try {
      serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }
    
	  // send ReadRequest
    byte[] requestData = request.getPacketData();
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
      byte[] dataPacketRaw = dataPacket.getPacketData();
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
      byte[] ackData = ack.getPacketData();
      DatagramPacket forwardAckDatagram = new DatagramPacket(ackData, ackData.length,
          ack.getRemoteHost(), serverPort);
      
      try {
        serverSocket.send(forwardAckDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
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
    
    // send WriteRequest
    byte[] requestData = request.getPacketData();
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
      byte[] ackPacketRaw = ack.getPacketData();
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
      byte[] dataPacketData = dataPacket.getPacketData();
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
	
	/*public static void performFileTransfer() {
	  byte[] buffer = new byte[1024];
	  
	  
	  DatagramSocket clientRequestSocket;
    try {
      clientRequestSocket = new DatagramSocket(Configuration.INTERMEDIATE_PORT);
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }
	  DatagramPacket clientRequestDatagram = new DatagramPacket(buffer, buffer.length);
	  
	  try {
      clientRequestSocket.receive(clientRequestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
    }
	  
	  int clientPort = clientRequestDatagram.getPort();
	  
	  try {
      DatagramSocket serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
      return;
    }
	  
	  serverSocket
	}*/
	
	
	/**
	 * Receives requests from the receiveSocket and forwards them
	 * to the server through the srSocket. Also receives the server
	 * response to the client from the receiveSocket through a 
	 * temporary socket.
	 */
	/*public void sendAndReceiveRequest() {
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
	}*/
	
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
