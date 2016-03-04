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
import packet.WriteRequest;

public class TftpWriteTransfer implements Runnable {
  private PacketParser packetParser = new PacketParser();
  private WriteRequest request;
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
      Packet forwardAckPacket = null;
      
      do {
        log("Waiting for ACK from server on port " + serverSocket.getLocalPort());
        try {
          serverSocket.receive(ackDatagram);
        } catch (IOException e2) {
          e2.printStackTrace();
          return;
        }
        
        log("Packet received.");
        IntermediateHost.printPacketInformation(ackDatagram);
        
        // parse data packet
        try {
          forwardAckPacket = packetParser.parseAcknowledgement(ackDatagram);
        } catch (InvalidPacketException e) {
          log("Error parsing ACK packet: " + e.getMessage());
          int errorCode = handleParseError(ackDatagram, clientSocket,
              ackDatagram.getAddress(), clientPort);
          
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
      } while (forwardAckPacket == null);

      if (transferEnded) {
        break;
      }

      serverPort = ackDatagram.getPort();

      log("Server is receiving on port " + serverPort);
      log("Received packet:\n" + forwardAckPacket.toString() + "\n");
      log("Forwarding packet to client on port " + clientPort);
      
      // forward to client
      byte[] ackPacketRaw = packetModifier.process(forwardAckPacket, serverSocket.getLocalPort(), clientPort);
      
      
      
      
      /*********
       * TODO:
       * If we go back to listening for an ACK, we may be waiting forever.
       * But if the client decides to send another data packet we won't see it
       * because we are waiting for the ACK.
       * 
       * Rewrite this more generically, so that it doesn't matter what type of
       * packet we get, we'll just send it (or delay/drop).
       */
      
      
      
      // Packet modifier will return null if we want to drop or delay the packet.
      if (ackPacketRaw == null) {
        log("Not forwarding: " + forwardAckPacket.toString());
        continue; // back to listening for another ACK
      } else {
        DatagramPacket forwardPacket = new DatagramPacket(ackPacketRaw, ackPacketRaw.length, forwardAckPacket.getRemoteHost(),
            clientPort);

        IntermediateHost.printPacketInformation(forwardPacket);
        
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

      }
      
      
      /*****************************************************************************************
       * Receive DataPacket from client and forward to server
       */

      // wait for Data Packet from client
      DatagramPacket dataPacketDatagram = new DatagramPacket(recvBuffer, recvBuffer.length);
      byte[] dataPacketData = null;
      Packet forwardDataPacket = null;
      boolean waitForNextPacket = true;
      
      while (waitForNextPacket) {
            
        while (forwardDataPacket == null) {
          try {
            log("Waiting for Data Packet from client.");
            clientSocket.receive(dataPacketDatagram);
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
            
          log("Received packet.");
          IntermediateHost.printPacketInformation(dataPacketDatagram);
    
          // parse ACK
          try {
            forwardDataPacket = packetParser.parseDataPacket(dataPacketDatagram);
          } catch (InvalidPacketException e) {
            log("Error parsing data packet.");
            int errorCode = handleParseError(dataPacketDatagram, serverSocket,
                dataPacketDatagram.getAddress(), serverPort);
            
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


        log("Received valid data packet:\n" + forwardDataPacket.toString() + "\n");
        log("Forwarding Data Packet to server on port " + serverPort);
        
        dataPacketData = packetModifier.process(forwardDataPacket, clientSocket.getLocalPort(), serverPort);
      
        // Packet modifier will return null if we want to drop or delay the packet.
        if (dataPacketData == null) {
          log("Not forwarding: " + forwardDataPacket.toString());
          waitForNextPacket = true;
        } else {
          waitForNextPacket = false;
        }
      }
      
      DatagramPacket forwardDataPacketDatagram = new DatagramPacket(dataPacketData, dataPacketData.length,
          forwardDataPacket.getRemoteHost(), serverPort);

      IntermediateHost.printPacketInformation(forwardDataPacketDatagram);
      
      try {
        serverSocket.send(forwardDataPacketDatagram);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      if (forwardDataPacket instanceof DataPacket) {
        // figure out if this was the last data packet
        receivedLastDataPacket = ((DataPacket) forwardDataPacket).getFileData().length < 512;
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
