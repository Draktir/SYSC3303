package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import packet.GenericPacket;
import packet.GenericPacketBuilder;
import packet.InvalidPacketException;
import packet.Packet;
import packet.PacketParser;

public class ClientConnection {
  private DatagramSocket clientSocket;
  
  public ClientConnection() throws SocketException {
    this.clientSocket = new DatagramSocket();
  }
  
  /**
   * Sends a packet, but does not wait for a response.
   * 
   * @param packet
   */
  public void sendPacket(Packet packet) {
    try {
      byte[] data = packet.getPacketData();
      DatagramPacket sendPacket = new DatagramPacket(
          data, data.length, packet.getRemoteHost(), packet.getRemotePort());
      System.out.println("[SYSTEM] Sending response to client on port " + packet.getRemotePort());
      clientSocket.send(sendPacket);
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
  
  /**
   * Sends a packet and blocks until a response is received on the same socket
   * 
   * @param packet
   * @return GenericPacket containing data and connection information
   */
  public Packet sendPacketAndReceive(Packet packet) {
    try {
      byte[] data = packet.getPacketData();
      DatagramPacket sendPacket = new DatagramPacket(
          data, data.length, packet.getRemoteHost(), packet.getRemotePort());
      System.out.println("[SYSTEM] Sending response to client on port " + packet.getRemotePort());
      clientSocket.send(sendPacket);
      
      byte[] buffer = new byte[516];
      DatagramPacket responsePacket = new DatagramPacket(buffer, 516);
      System.out.println("[SYSTEM] Waiting for response from client on port " + clientSocket.getLocalPort());
      clientSocket.receive(responsePacket);
      
      System.out.println("Received packet from client, length: " + responsePacket.getLength());
      
      // copy data out of the buffer
      int len = responsePacket.getLength();
      byte[] received = new byte[len]; 
      System.arraycopy(responsePacket.getData(), 0, received, 0, len);
            
      GenericPacket recvdPacket = new GenericPacketBuilder()
              .setRemoteHost(responsePacket.getAddress())
              .setRemotePort(responsePacket.getPort())
              .setPacketData(received)
              .buildGenericPacket();
      
      PacketParser packetParser = new PacketParser();
      try {
        return packetParser.parse(recvdPacket);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }
  
}
