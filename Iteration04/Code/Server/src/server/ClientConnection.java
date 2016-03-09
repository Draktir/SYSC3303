package server;

import java.io.IOException;

import java.net.*;
import java.util.Date;

import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import packet.ErrorPacketBuilder;
import packet.Packet;

public class ClientConnection {
  public final TransferId clientTid;
  private final DatagramSocket socket;

  public ClientConnection(DatagramPacket originalRequest) throws SocketException {
    clientTid = new TransferId(originalRequest);
    this.socket = new DatagramSocket();
  }

  public void sendPacket(Packet packet) throws IOException {
    byte[] data = packet.getPacketData();
    InetAddress remoteHost = packet.getRemoteHost() == null ? clientTid.address : packet.getRemoteHost();
    int remotePort = packet.getRemotePort() < 1 ? clientTid.port : packet.getRemotePort();

    DatagramPacket sendDatagram = new DatagramPacket(
        data, data.length, remoteHost, remotePort);

    System.out.println("[CLIENT-CONNECTION] sending packet");
    printDatagramPacket(sendDatagram);

    socket.send(sendDatagram);
  }

  public DatagramPacket receive(int timeout) throws SocketTimeoutException {
    byte[] buffer;
    DatagramPacket receiveDatagram = null;

    // set the timeout
    try {
      socket.setSoTimeout(timeout);
    } catch (SocketException e) {
      e.printStackTrace();
      return null;
    }

    // receive packets until we receive a packet from the correct host
    do {
      buffer = new byte[517];
      receiveDatagram = new DatagramPacket(buffer, 517);

      System.out.println("[CLIENT-CONNECTION] Waiting for response from client on port " + socket.getLocalPort());

      long tsStart = new Date().getTime();
      try {
        socket.receive(receiveDatagram);
      } catch (SocketTimeoutException e) {
        System.out.println("[CLIENT-CONNECTION] Response timed out.");
        throw e;
      } catch (IOException e) {
        e.printStackTrace();
        receiveDatagram = null;
        break;
      }
      long tsStop = new Date().getTime();

      System.out.println("[CLIENT-CONNECTION] Packet received.");
      printDatagramPacket(receiveDatagram);

      // ensure the client TID is correct
      if (!clientTid.equals(new TransferId(receiveDatagram))) {
        handleInvalidTid(receiveDatagram);
        // recalculate the timeout
        try {
          int newTimeout = socket.getSoTimeout() - (int)(tsStop - tsStart);
          socket.setSoTimeout(newTimeout);
        } catch (SocketException e) {
          e.printStackTrace();
        }

        System.err.println("[CLIENT-CONNECTION] Waiting for another packet.");
        receiveDatagram = null;
      }
    } while (receiveDatagram == null);

    return receiveDatagram;
  }

  private void handleInvalidTid(DatagramPacket receiveDatagram) {
    System.err.println("[CLIENT-CONNECTION] Received packet with wrong TID");
    System.err.println("  > Received:   " + receiveDatagram.getAddress() + " " + receiveDatagram.getPort());
    System.err.println("  > Expected:   " + clientTid.address + " " + clientTid.port);
    // respond to the rogue client with an appropriate error packet
    ErrorPacket errPacket = new ErrorPacketBuilder()
        .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
        .setMessage("Your request had an invalid TID.")
        .setRemoteHost(receiveDatagram.getAddress())
        .setRemotePort(receiveDatagram.getPort())
        .buildErrorPacket();

    System.err.println("[CLIENT-CONNECTION] Sending error to client with invalid TID\n" + errPacket.toString() + "\n");

    byte[] errData = errPacket.getPacketData();
    DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
        errPacket.getRemoteHost(), errPacket.getRemotePort());

    try {
      socket.send(errDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("[CLIENT-CONNECTION] Error sending error packet to unknown client. Ignoring this error.");
    }
  }

  private void printDatagramPacket(DatagramPacket packet) {
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
}