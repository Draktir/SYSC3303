package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import packet.ErrorPacketBuilder;
import packet.Packet;

public class ClientConnection {
  private DatagramSocket clientSocket;
  private InetAddress clientAddress;
  private int clientPort;
  
  public ClientConnection(DatagramPacket originalRequest) throws SocketException {
    this.clientAddress = originalRequest.getAddress();
    this.clientPort = originalRequest.getPort();
    this.clientSocket = new DatagramSocket();
  }
  
  /**
   * Sends a packet, but does not wait for a response.
   * 
   * @param packet
   * @throws InvalidClientTidException 
   */
  public void sendPacket(Packet packet) {
    byte[] data = packet.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(
        data, data.length, packet.getRemoteHost(), packet.getRemotePort());
    System.out.println("[SYSTEM] Sending response to client on port " + packet.getRemotePort());
    
    try {
      clientSocket.send(sendDatagram);
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
   * @throws InvalidClientTidException 
   */
  public DatagramPacket sendPacketAndReceive(Packet packet) {
    // 1. form datagram packet
    byte[] data = packet.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(
        data, data.length, packet.getRemoteHost(), packet.getRemotePort());
    
    // 2. send datagram packet
    System.out.println("[SYSTEM] Sending response to client on port " + packet.getRemotePort());
    try {
      clientSocket.send(sendDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    // 3. wait to receive a response
    byte[] buffer;
    DatagramPacket responseDatagram = null;
    
    do {
      buffer = new byte[517];
      responseDatagram = new DatagramPacket(buffer, 517);
      System.out.println("[SYSTEM] Waiting for response from client on port " + clientSocket.getLocalPort());
      
      try {
        clientSocket.receive(responseDatagram);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
      
      // ensure the client TID is the same
      if (!isClientTidValid(responseDatagram)) {
        System.err.println("Received packet with wrong TID");
        System.err.println("  > Client:   " + responseDatagram.getAddress() + " " + responseDatagram.getPort());
        System.err.println("  > Expected: " + clientAddress + " " + clientPort);
        // respond to the rogue client with an appropriate error packet
        ErrorPacket errPacket = new ErrorPacketBuilder()
            .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
            .setMessage("Your request had an invalid TID.")
            .setRemoteHost(responseDatagram.getAddress())
            .setRemotePort(responseDatagram.getPort())
            .buildErrorPacket();
        
        byte[] errData = errPacket.getPacketData();
        DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
            errPacket.getRemoteHost(), errPacket.getRemotePort());
        
        try {
          clientSocket.send(errDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          System.err.println("Error sending error packet to unknown client. Ignoring this error.");
        }
        responseDatagram = null;
      }
      
    } while (responseDatagram == null);
    
    
    System.out.println("Received packet from client, length: " + responseDatagram.getLength());
    return responseDatagram;
  }
  
  private boolean isClientTidValid(DatagramPacket packet) {
    return clientAddress.equals(packet.getAddress()) && packet.getPort() == clientPort;
  }
}
