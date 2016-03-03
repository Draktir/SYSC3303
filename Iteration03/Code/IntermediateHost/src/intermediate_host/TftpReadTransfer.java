package intermediate_host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import Configuration.Configuration;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.InvalidErrorPacketException;
import packet.InvalidPacketException;
import packet.Packet;
import packet.PacketParser;
import packet.ReadRequest;

public class TftpReadTransfer implements Runnable {
  private PacketParser packetParser = new PacketParser();
  private ReadRequest request;
  private int clientPort;
  private PacketModifier packetModifier;
  
  public TftpReadTransfer(ReadRequest request, PacketModifier packetModifier) {
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
       
    log("Forwarding Read Request to server on port " + Configuration.SERVER_PORT);
    
    // forward ReadRequest
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

    while (!transferEnded) {

      /*****************************************************************************************
       * Receive Data packet from server and forward to client
       */
      
      // wait for data packet
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      // receive a data packet
      Packet forwardDataPacket = null;
      do {
        log("Waiting for data packet from server on port " + serverSocket.getLocalPort());
        try {
          serverSocket.receive(dataPacketDatagram);
        } catch (IOException e2) {
          e2.printStackTrace();
          return;
        }

        log("Packet received");
        IntermediateHost.printPacketInformation(dataPacketDatagram);
        
        // parse data packet
        try {
          forwardDataPacket = packetParser.parseDataPacket(dataPacketDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing data packet: " + e.getMessage());
          int errorCode = handleParseError(dataPacketDatagram, clientSocket,
              dataPacketDatagram.getAddress(), clientPort);
          
          if (errorCode >= 0 && errorCode != 5) {
            log("Non-recoverable error. Terminating this connection.");
            transferEnded = true;
            break;
          } else if (errorCode == 5) {
            log("Received error code 5 (unknown TID), ignore it and keep listening");
            forwardDataPacket = null;
          } else {
            log("Received an unexpected packet that is not an error. Forwarding anyhow.");
            // parse the packet, no matter what it is, and send it on.
            try {
              forwardDataPacket = packetParser.parse(dataPacketDatagram);
            } catch (InvalidPacketException e1) {
              e1.printStackTrace();
              System.out.println("This packet is broken! Will not send it: " + e1.getMessage());
              IntermediateHost.printPacketInformation(dataPacketDatagram);
            }
          }
        }
      } while (forwardDataPacket == null);

      if (transferEnded) {
        break;
      }
    
      serverPort = dataPacketDatagram.getPort();
      
      log("Server is receiving on port " + serverPort);
      log("Received packet:\n" + forwardDataPacket.toString() + "\n");
      log("Forward Packet to client.");
      
      // forward to client
      byte[] dataPacketRaw = packetModifier.process(forwardDataPacket, serverSocket.getLocalPort(), clientPort);
      
      // Packet modifier will return null if we want to drop or delay the packet.
      if (dataPacketRaw == null) {
        log("Not forwarding: " + forwardDataPacket.toString());
        continue; // back to listening for another data packet
      }
      
      DatagramPacket forwardDatagram = new DatagramPacket(dataPacketRaw, dataPacketRaw.length, forwardDataPacket.getRemoteHost(),
          clientPort);
      
      IntermediateHost.printPacketInformation(forwardDatagram);

      try {
        clientSocket.send(forwardDatagram);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

      /*****************************************************************************************
       * Receive ACK from client and forward to server
       */

      // wait for ACK from client
      DatagramPacket ackDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      byte[] ackData = null;
      Packet forwardAckPacket = null;
      boolean waitForNextPacket = true;
      
      while (waitForNextPacket) {
        
        /*** Receive the packet ***/
        
        while (forwardAckPacket == null) {
          try {
            log("Waiting for an ACK from the client on port " + clientSocket.getLocalPort());
            clientSocket.receive(ackDatagram);
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          
          log("Packet received.");
          IntermediateHost.printPacketInformation(ackDatagram);
          
          // parse ACK
          try {
            forwardAckPacket = packetParser.parseAcknowledgement(ackDatagram);
          } catch (InvalidPacketException e) {
            log("Error parsing ACK packet: " + e.getMessage());
            int errorCode = handleParseError(ackDatagram, serverSocket,
                ackDatagram.getAddress(), serverPort);
            
            if (errorCode >= 0 && errorCode != 5) {
              log("Non-recoverable error. Terminating this connection.");
              transferEnded = true;
              break;
            } else if (errorCode == 5) {
              log("Received error code 5 (unknown TID), ignore it and keep listening");
              forwardAckPacket = null;
            } else {
              log("Received an unexpected packet that is not an error. Forwarding anyhow.");
              // parse the packet, no matter what it is, and send it on.
              try {
                forwardAckPacket = packetParser.parse(ackDatagram);
              } catch (InvalidPacketException e1) {
                e1.printStackTrace();
                System.out.println("This packet is broken! Will not send it: " + e1.getMessage());
                IntermediateHost.printPacketInformation(ackDatagram);
              }
            }
          }
        }
  
        if (transferEnded) {
          break;
        }
  
        log("Received Packet:\n" + forwardAckPacket.toString() + "\n");
        log("Forwarding Packet to server.");
  
        
        /*** Determine whether or not to forward the packet ***/
        
        ackData = packetModifier.process(forwardAckPacket, clientSocket.getLocalPort(), serverPort);
        
        // Packet modifier will return null if we want to drop or delay the packet.
        if (ackData == null) {
          log("Not forwarding: " + forwardAckPacket.toString());
          waitForNextPacket = true; // don't forward and keep waiting
        } else {
          waitForNextPacket = false; // forward the packet
        }
      }
      
      DatagramPacket forwardAckDatagram = new DatagramPacket(ackData, ackData.length, 
          forwardAckPacket.getRemoteHost(), serverPort);

      IntermediateHost.printPacketInformation(forwardAckDatagram);
      
      try {
        serverSocket.send(forwardAckDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      // check if we're done
      if (forwardDataPacket instanceof DataPacket) {
        transferEnded = ((DataPacket) forwardDataPacket).getFileData().length < 512;
      }
    }
    
    log("TFTP Read transfer complete");
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
    System.out.println("[TFTP-READ: " + name + "] " + msg);
  }
}
