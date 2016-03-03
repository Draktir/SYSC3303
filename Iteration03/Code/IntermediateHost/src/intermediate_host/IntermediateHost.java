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
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import Configuration.Configuration;
import modification.*;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.InvalidErrorPacketException;
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
   * Main method which creates an instance of IntermediateHost to forward and
   * receive TFTP requests.
   * 
   * @param args
   */
  public static void main(String[] args) {
    IntermediateHost h = new IntermediateHost();
    boolean exit;
    Scanner scan = new Scanner(System.in);
    do {
      h.go();
      System.out.println("\nSimulation complete!\n");
      System.out.print("Do you want to simulate another error scenario? (y/n) ");
      exit = !scan.next().equalsIgnoreCase("y");
    } while (!exit);
    scan.close();
  }

  public void go() {
    List<Thread> connectionThreads = new ArrayList<>();
    
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

    log("");
    log("Waiting for client requests on port " + Configuration.INTERMEDIATE_PORT);

    // run forever
    do {
      byte[] buffer = new byte[1024];
      DatagramPacket requestDatagram = new DatagramPacket(buffer, buffer.length);
  
      Request request = null;
      do {
        try {
          clientSocket.receive(requestDatagram);
        } catch (IOException e) {
          e.printStackTrace();
        }  
  
        log("Received packet");
        printPacketInformation(requestDatagram);
  
        try {
          request = packetParser.parseRequest(requestDatagram);
        } catch (InvalidPacketException e1) {
          request = null;
          log("Not a valid request. Expecting RRQ or WRQ.\n");
        }
      } while (request == null);
  
      clientPort = requestDatagram.getPort();
      log("Client is receiving on port " + clientPort);
  
      // determine the type of request and then service the file transfer
      if (request.type() == RequestType.READ) {
        log("Received valid Read Request: " + request.toString());
        Runnable tftpReadTransfer = new TftpReadTransfer((ReadRequest) request, packetModifier);
        Thread t = new Thread(tftpReadTransfer, "#" + (connectionThreads.size() + 1));
        connectionThreads.add(t);
        t.start();
        
      } else if (request.type() == RequestType.WRITE) {
        log("Received valid Write Request: " + request.toString());
        serviceTftpWrite((WriteRequest) request);
      }
    } while (connectionThreads.stream().anyMatch((t) -> t.isAlive()));
    
    log("All connections terminated");
  }

  public void serviceTftpWrite(WriteRequest request) {
    
  }

  private boolean handleParseError(DatagramPacket datagram, DatagramSocket socket, InetAddress recvHost, int recvPort) {
    // first figure out whether datagram is an Error packet
    ErrorPacket errPacket = null;
    try {
      errPacket = packetParser.parseErrorPacket(datagram);
    } catch (InvalidErrorPacketException e) {
      // Nope, not an error packet, someone screwed up.
      log("Not an error packet, but unexpected type. Forwarding anyhow.\n\n");
      return true;
    }

    log("Received an error packet: " + errPacket.getErrorCode() + "\n" + errPacket.toString() + "\n");
    log("Forwarding error packet.");
    
    byte[] packetData = errPacket.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(packetData, packetData.length, recvHost, recvPort);
    
    printPacketInformation(sendDatagram);
    
    try {
      socket.send(sendDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    
    // return true if it's an error code 5 and we want to continue
    return errPacket.getErrorCode() == ErrorPacket.ErrorCode.UNKNOWN_TRANSFER_ID;
  }

  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
   */
  public static void printPacketInformation(DatagramPacket packet) {
    byte[] data = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
    String contents = new String(data);
    
    System.out.println("\n-------------------------------------------");
    System.out.println("\tAddress: " + packet.getAddress());
    System.out.println("\tPort: " + packet.getPort());
    System.out.println("\tPacket contents: ");
    System.out.println("\t" + contents.replaceAll("\n", "\t\n"));

    System.out.println("\tPacket contents (bytes): ");
    System.out.print("\t");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println("\n-------------------------------------------\n");
  }
  
  private void log(String msg) {
    System.out.println("[INTERMEDIATE] " + msg);
  }
}
