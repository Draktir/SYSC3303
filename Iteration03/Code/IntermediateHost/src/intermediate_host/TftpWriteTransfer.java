package intermediate_host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import Configuration.Configuration;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.InvalidErrorPacketException;
import packet.InvalidPacketException;
import packet.PacketParser;
import packet.ReadRequest;

public class TftpWriteTransfer implements Runnable {
  private PacketParser packetParser = new PacketParser();
  private ReadRequest request;
  private int clientPort;
  private PacketModifier packetModifier;
  
  public TftpWriteTransfer(WriteRequest request, PacketModifier packetModifier) {
    this.packetModifier = packetModifier;
    this.request = request;
    this.clientPort = request.getRemotePort();
  }
  
  @Override
  public void run() {
    DatagramSocket serverSocket = null;
    DatagramSocket clientSocket = null;
    try {
      serverSocket = new DatagramSocket();
      clientSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }

    byte[] requestData = null;
    while (requestData == null) {
      // PacketModifier figures out if the packet needs to be modified, applies
      // the modification if applicable, and returns the packet data as a byte[].
      requestData = packetModifier.process(request, Configuration.INTERMEDIATE_PORT, Configuration.SERVER_PORT);
      
      // Packet modifier will return null if we want to drop or delay the packet.
      if (requestData == null) {
        log("Not forwarding: " + request.toString());
        // we can drop out of this routine, since the original request will be
        // retransmitted to the intermediate host's port 68, which will start
        // another thread like this one.
        return;
      }
    }
    
    log("Forwarding Write Request to server on port " + Configuration.SERVER_PORT);
    
    // send WriteRequest
    DatagramPacket requestDatagram = new DatagramPacket(requestData, requestData.length, request.getRemoteHost(),
        Configuration.SERVER_PORT);

    IntermediateHost.printPacketInformation(requestDatagram);
    
    try {
      serverSocket.send(requestDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    int serverPort;
    byte[] recvBuffer = new byte[1024];
    boolean transferEnded = false;
    boolean receivedLastDataPacket = false;

    while (!transferEnded) {
      /*****************************************************************************************
       * Receive ACK from server and forward to client
       */

      // wait for ack packet
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      Acknowledgement ack = null;
      
      do {
        log("Waiting for ACK from server on port " + serverSocket.getLocalPort());
        serverSocket.receive(ackDatagram);
        
        log("Packet received.");
        IntermediateHost.printPacketInformation(ackDatagram);
        
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
      byte[] ackPacketRaw = packetModifier.process(ack, serverSocket.getLocalPort(), clientPort);
      
      // Packet modifier will return null if we want to drop or delay the packet.
      if (ackPacketRaw == null) {
        log("ACK has been cancelled. Not forwarding: " + ack.toString());
      }
      
      DatagramPacket forwardPacket = new DatagramPacket(ackPacketRaw, ackPacketRaw.length, ack.getRemoteHost(),
          clientPort);

      printPacketInformation(forwardPacket);
      
      try {
        clientSocket.send(forwardPacket);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      
      
      // figure out if we're done (i.e. we just sent the last ACK)
      if (receivedLastDataPacket) {
        log("");
        log("File Transfer is complete.\n");
        transferEnded = true;
        break;
      }

      /*****************************************************************************************
       * Receive DataPacket from client and forward to server
       */

      // wait for Data Packet from client
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      DataPacket dataPacket = null;

      do {
      boolean packetReceived = false;
      
      while (!packetReceived) {
        try {
            log("Waiting for Data Packet from client.");
            clientSocket.receive(dataPacketDatagram);
            packetReceived = true;
        } catch (SocketTimeoutException e) {
          try {
            clientSocket.send(forwardPacket);
          } catch (IOException e1) {
              e1.printStackTrace();
          return;
          }
          } catch (IOException e) {
            e.printStackTrace();
            return;
          } 
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
      byte[] dataPacketData = packetModifier.process(dataPacket, clientSocket.getLocalPort(), serverPort);
      
      // Packet modifier will return null if we want to drop or delay the packet.
      if (dataPacketData == null) {
        log("DataPacket has been cancelled. Not forwarding: " + dataPacket.toString());
      }
      
      DatagramPacket forwardDataPacketDatagram = new DatagramPacket(dataPacketData, dataPacketData.length,
          dataPacket.getRemoteHost(), serverPort);

      printPacketInformation(forwardDataPacketDatagram);
      
      // figure out if this was the last data packet
      receivedLastDataPacket = (dataPacket.getFileData().length < 512); 
      
      try {
        serverSocket.send(forwardDataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    serverSocket.close();
  }
  
  /**
   * @param datagram that caused the parse error
   * @param socket to send potential error on
   * @param recvHost host to send potential error to
   * @param recvPort port on host to send potential error to
   * @return error code (0 - 5) or -1 if datagram wasn't an error packet
   */
  private int handleParseError(DatagramPacket datagram, DatagramSocket socket, InetAddress recvHost, int recvPort) {
    // first figure out whether datagram is an Error packet
    ErrorPacket errPacket = null;
    try {
      errPacket = packetParser.parseErrorPacket(datagram);
    } catch (InvalidErrorPacketException e) {
      // Nope, not an error packet, we'll send it anyhow
      return -1;
    }

    log("Received an error packet: " + errPacket.getErrorCode() + "\n" + errPacket.toString() + "\n");
    log("Forwarding error packet.");
    
    byte[] packetData = errPacket.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(packetData, packetData.length, recvHost, recvPort);
    
    IntermediateHost.printPacketInformation(sendDatagram);
    
    try {
      socket.send(sendDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
    
    return errPacket.getErrorCode().getValue();
  }
   
  private void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[TFTP-WRITE: " + name + "] " + msg);
  }

}
