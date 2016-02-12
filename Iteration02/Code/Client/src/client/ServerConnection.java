package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.Packet;
import packet.ErrorPacket.ErrorCode;

public class ServerConnection {
  private DatagramSocket serverSocket;
  private InetAddress serverAddress = null;
  private int serverPort = -1;

  public ServerConnection() throws SocketException {
    this.serverSocket = new DatagramSocket();
  }

  /**
   * Sends a packet, but does not wait for a response.
   * 
   * @param packet
   */
  public void sendPacket(Packet packet) {
    try {
      byte[] data = packet.getPacketData();
      DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getRemoteHost(), packet.getRemotePort());
      
      System.out.println("[SYSTEM] Sending request to server on port " + packet.getRemotePort());
      Client.printPacketInformation(sendPacket);
      
      serverSocket.send(sendPacket);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Sends a packet and blocks until a response is received on the same socket
   * 
   * @param packet
   * @return GenericPacket containing data and connection information
   */
  public DatagramPacket sendPacketAndReceive(Packet packet) {
    byte[] data = packet.getPacketData();
    DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getRemoteHost(), packet.getRemotePort());
    
    System.out.println("[SERVER-CONNECTION] Sending packet to server on port " + packet.getRemotePort());
    Client.printPacketInformation(sendPacket);
    
    try {
      serverSocket.send(sendPacket);
    } catch (IOException e1) {
      e1.printStackTrace();
      return null;
    }

    byte[] buffer = null;
    DatagramPacket responseDatagram = null;
    
    do {
      buffer = new byte[517];
      responseDatagram = new DatagramPacket(buffer, 517);
      System.out.println("[SERVER-CONNECTION] Waiting for response from server on port " + serverSocket.getLocalPort());
      
      try {
        serverSocket.receive(responseDatagram);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
      
      // if this is the first time we talk to this server, record its TID
      if (this.serverAddress == null) {
        this.serverAddress = responseDatagram.getAddress();
        this.serverPort = responseDatagram.getPort();
      }
      
      // ensure the client TID is the same
      if (!isServerTidValid(responseDatagram)) {
        System.err.println("[SERVER-CONNECTION] Received packet with wrong TID");
        System.err.println("  > Server:   " + responseDatagram.getAddress() + " " + responseDatagram.getPort());
        System.err.println("  > Expected: " + serverAddress + " " + serverPort);
        // respond to the rogue client with an appropriate error packet
        ErrorPacket errPacket = new ErrorPacketBuilder()
            .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
            .setMessage("Your request had an invalid TID.")
            .setRemoteHost(responseDatagram.getAddress())
            .setRemotePort(responseDatagram.getPort())
            .buildErrorPacket();
        
        System.err.println("[SERVER-CONNECTION] Sending error to server with invalid TID\n" + errPacket.toString() + "\n");
        
        byte[] errData = errPacket.getPacketData();
        DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
            errPacket.getRemoteHost(), errPacket.getRemotePort());
        
        try {
          serverSocket.send(errDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          System.err.println("[SERVER-CONNECTION] Error sending error packet to unknown server. Ignoring this error.");
        }
        responseDatagram = null;
        
        System.err.println("[SERVER-CONNECTION] Waiting for another packet.");
      }
      
    } while (responseDatagram == null);
    
    return responseDatagram;
  }

  private boolean isServerTidValid(DatagramPacket packet) {
    return serverAddress.equals(packet.getAddress()) && packet.getPort() == serverPort;
  }
  
}
