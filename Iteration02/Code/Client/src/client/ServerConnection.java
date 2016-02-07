package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import packet.GenericPacket;
import packet.GenericPacketBuilder;
import packet.InvalidPacketException;
import packet.Packet;
import packet.PacketParser;

public class ServerConnection {
  private DatagramSocket serverSocket;

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
      System.out.println("[SYSTEM] Sending request to server on port " + packet.getRemotePort() + " with length (bytes) " + data.length + ".");
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
    System.out.println("[SYSTEM] Sending request to server on port " + packet.getRemotePort() + " with length (bytes) " + data.length + ".");
    try {
      serverSocket.send(sendPacket);
    } catch (IOException e1) {
      e1.printStackTrace();
      return null;
    }

    System.out.println("[SYSTEM] Waiting for response from server on port " + serverSocket.getLocalPort() + ".");
    byte[] buffer = new byte[516];
    DatagramPacket responsePacket = new DatagramPacket(buffer, 516);
    try {
      serverSocket.receive(responsePacket);
    } catch (IOException e1) {
      e1.printStackTrace();
      return null;
    }

    System.out.println("\tReceived from server: " + Arrays.toString(buffer));
    return responsePacket;
    return null;
  }

}
