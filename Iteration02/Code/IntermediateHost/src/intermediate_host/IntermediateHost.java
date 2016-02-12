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
   *          unused
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
    log("Waiting for client request on port " + Configuration.INTERMEDIATE_PORT);

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
      serviceTftpRead((ReadRequest) request);
    } else if (request.type() == RequestType.WRITE) {
      log("Received valid Write Request: " + request.toString());
      serviceTftpWrite((WriteRequest) request);
    }

    log("File Transfer ended.\n");

    clientSocket.close();
  }

  public void serviceTftpRead(ReadRequest request) {
    try {
      serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }

    // PacketModifier figures out if the packet needs to be modified, applies
    // the modification if applicable, and returns the packet data as a byte[].
    byte[] requestData = packetModifier.process(request, Configuration.SERVER_PORT);
    
    log("Forwarding Read Request to server on port " + Configuration.SERVER_PORT);
    
    // send ReadRequest
    DatagramPacket requestDatagram = new DatagramPacket(requestData, requestData.length, request.getRemoteHost(),
        Configuration.SERVER_PORT);
    
    printPacketInformation(requestDatagram);
    
    try {
      serverSocket.send(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    int serverPort;
    byte[] recvBuffer = new byte[1024];
    boolean transferEnded = false;

    while (!transferEnded) {

      /*****************************************************************************************
       * Receive Data packet from server and forward to client
       */
      
      // wait for data packet
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);

      log("Waiting for data packet from server on port " + serverSocket.getLocalPort());
      
      // receive a data packet
      DataPacket dataPacket = null;
      do {
        try {
          serverSocket.receive(dataPacketDatagram);
        } catch (IOException e) {
          e.printStackTrace();
        }
        
        log("Packet received");
        printPacketInformation(dataPacketDatagram);
        
        // parse data packet
        try {
          dataPacket = packetParser.parseDataPacket(dataPacketDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing data packet: " + e.getMessage());
          // if this is not an error packet or it's an error packet != error
          // code 5 we are done.
          boolean isRecoverableError = handleParseError(dataPacketDatagram, clientSocket,
              dataPacketDatagram.getAddress(), clientPort);
          if (!isRecoverableError) {
            log("Non-recoverable error. Terminating this connection.");
            transferEnded = true;
            break;
          }
          log("Error is recoverable.");
          dataPacket = null;
        }
      } while (dataPacket == null);

      if (transferEnded) {
        break;
      }
    
      serverPort = dataPacketDatagram.getPort();
      
      log("Server is receiving on port " + serverPort);
      log("Received valid data packet:\n" + dataPacket.toString() + "\n");
      log("Forward Data Packet to client.");
      
      // forward to client
      byte[] dataPacketRaw = packetModifier.process(dataPacket, clientPort);
      DatagramPacket forwardPacket = new DatagramPacket(dataPacketRaw, dataPacketRaw.length, dataPacket.getRemoteHost(),
          clientPort);
      
      printPacketInformation(forwardPacket);

      try {
        clientSocket.send(forwardPacket);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

      /*****************************************************************************************
       * Receive ACK from client and forward to server
       */

      log("Waiting for an ACK from the client on port " + clientSocket.getLocalPort());
      // wait for ACK from client
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      Acknowledgement ack = null;
      do {
        try {
          clientSocket.receive(ackDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }

        log("Packet received.");
        printPacketInformation(ackDatagram);
        
        // parse ACK
        try {
          ack = packetParser.parseAcknowledgement(ackDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing ACK");
          // if this is not an error packet or it's an error packet != error
          // code 5 we are done.
          boolean isRecoverableError = handleParseError(ackDatagram, serverSocket, ackDatagram.getAddress(),
              serverPort);
          if (!isRecoverableError) {
            transferEnded = true;
            log("Non-recoverable error. Terminating this connection");
            break;
          }
          log("Error is recoverable.");
          ack = null;
        }
      } while (ack == null);

      if (transferEnded) {
        break;
      }

      log("Received valid ACK:\n" + ack.toString() + "\n");
      log("Forwarding ACK to server.");

      // forward ACK to server
      byte[] ackData = packetModifier.process(ack, serverPort);
      DatagramPacket forwardAckDatagram = new DatagramPacket(ackData, ackData.length, ack.getRemoteHost(), serverPort);

      printPacketInformation(forwardAckDatagram);
      
      try {
        serverSocket.send(forwardAckDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // check if we're done
      if (dataPacket.getFileData().length < 512) {
        log("File transfer complete.");
        transferEnded = true;
      }
    }

    serverSocket.close();
  }

  public void serviceTftpWrite(WriteRequest request) {
    try {
      serverSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }

    // PacketModifier figures out if the packet needs to be modified, applies
    // the modification if applicable, and returns the packet data as a byte[].
    byte[] requestData = packetModifier.process(request, Configuration.SERVER_PORT);

    log("Forwarding Write Request to server on port " + Configuration.SERVER_PORT);
    
    // send WriteRequest
    DatagramPacket requestDatagram = new DatagramPacket(requestData, requestData.length, request.getRemoteHost(),
        Configuration.SERVER_PORT);

    printPacketInformation(requestDatagram);
    
    try {
      serverSocket.send(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    int serverPort;
    byte[] recvBuffer = new byte[1024];
    boolean transferEnded = false;

    while (!transferEnded) {
      /*****************************************************************************************
       * Receive ACK from server and forward to client
       */

      // wait for ack packet
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      Acknowledgement ack = null;

      log("Waiting for ACK from server on port " + serverSocket.getLocalPort());
      
      do {
        try {
          serverSocket.receive(ackDatagram);
        } catch (IOException e) {
          e.printStackTrace();
        }

        log("Packet received.");
        printPacketInformation(ackDatagram);
        
        // parse data packet
        try {
          ack = packetParser.parseAcknowledgement(ackDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing ACK");
          // if this is not an error packet or it's an error packet != error
          // code 5 we are done.
          boolean isRecoverableError = handleParseError(ackDatagram, clientSocket, ackDatagram.getAddress(),
              clientPort);
          if (!isRecoverableError) {
            log("Non-recoverable error. Terminating this connection");
            transferEnded = true;
            break;
          }
          log("Error is recoverable.");
          ack = null;
        }
      } while (ack == null);

      if (transferEnded) {
        break;
      }

      serverPort = ackDatagram.getPort();

      log("Server is receiving on port " + serverPort);
      log("Received valid ACK:\n" + ack.toString() + "\n");
      log("Forwarding ACK to client on port " + clientPort);
      
      // forward to client
      byte[] ackPacketRaw = packetModifier.process(ack, clientPort);
      DatagramPacket forwardPacket = new DatagramPacket(ackPacketRaw, ackPacketRaw.length, ack.getRemoteHost(),
          clientPort);

      printPacketInformation(forwardPacket);
      
      try {
        clientSocket.send(forwardPacket);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

      /*****************************************************************************************
       * Receive DataPacket from client and forward to server
       */

      // wait for Data Packet from client
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      DataPacket dataPacket = null;

      log("Waiting for Data Packet from server.");
      
      do {
        try {
          clientSocket.receive(dataPacketDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        
        log("Received packet.");
        printPacketInformation(dataPacketDatagram);

        // parse ACK
        try {
          dataPacket = packetParser.parseDataPacket(dataPacketDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing data packet.");
          // if this is not an error packet or it's an error packet != error
          // code 5 we are done.
          boolean isRecoverableError = handleParseError(dataPacketDatagram, serverSocket,
              dataPacketDatagram.getAddress(), serverPort);
          if (!isRecoverableError) {
            log("Non-recoverable error. Terminating this connnection.");
            transferEnded = true;
            break;
          }
          log("Error is recoverable.");
          dataPacket = null;
        }
      } while (dataPacket == null);

      if (transferEnded) {
        break;
      }

      log("Received valid data packet:\n" + dataPacket.toString() + "\n");
      log("Forwarding Data Packet to server on port " + serverPort);

      // forward data packet to server
      byte[] dataPacketData = packetModifier.process(dataPacket, serverPort);
      DatagramPacket forwardDataPacketDatagram = new DatagramPacket(dataPacketData, dataPacketData.length,
          dataPacket.getRemoteHost(), serverPort);

      printPacketInformation(forwardDataPacketDatagram);
      
      try {
        serverSocket.send(forwardDataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }

      // figure out if we're done
      if (dataPacket != null && dataPacket.getBlockNumber() > 0 && dataPacket.getFileData().length < 512) {
        log("");
        log("File Transfer is complete.\n");
        transferEnded = true;
      }
    }

    serverSocket.close();
  }

  private boolean handleParseError(DatagramPacket datagram, DatagramSocket socket, InetAddress recvHost, int recvPort) {
    // first figure out whether datagram is an Error packet
    ErrorPacket errPacket = null;
    try {
      errPacket = packetParser.parseErrorPacket(datagram);
    } catch (InvalidErrorPacketException e) {
      // Nope, not an error packet, someone screwed up.
      log("Invalid packet type received. Terminating connection.\n\n");
      return false;
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
  public static void printPacketInformation(DatagramPacket datagram) {
    byte[] data = new byte[datagram.getLength()];
    System.arraycopy(datagram.getData(), 0, data, 0, datagram.getLength());
    String contents = new String(data);
    
    System.out.println("\tAddress: " + datagram.getAddress());
    System.out.println("\tPort: " + datagram.getPort());
    System.out.println("\tRequest contents: ");
    System.out.println("\t" + contents);

    System.out.println("\tRequest contents (bytes): ");
    System.out.print("\t");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println();
  }
  
  private void log(String msg) {
    System.out.println("[INTERMEDIATE] " + msg);
  }
}
