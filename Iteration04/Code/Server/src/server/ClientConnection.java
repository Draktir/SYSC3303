package server;

import java.io.IOException;

import java.net.*;
import java.util.Date;

import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import packet.ErrorPacketBuilder;
import packet.Packet;
import packet.Request;

import tftp_transfer.Connection;
import tftp_transfer.TransferId;

import utils.Logger;
import utils.PacketPrinter;

public class ClientConnection implements Connection {
  private final Logger logger = new Logger("ClientConnection");
  public final TransferId clientTid;
  private final DatagramSocket socket;

  public ClientConnection(DatagramPacket originalRequest) throws SocketException {
    clientTid = new TransferId(originalRequest);
    this.socket = new DatagramSocket();
  }

  public void sendRequest(Request request) throws IOException {
    sendPacket(request);
  }
  
  public void sendPacket(Packet packet) throws IOException {
    byte[] data = packet.getPacketData();

    DatagramPacket sendDatagram = new DatagramPacket(
        data, data.length, clientTid.address, clientTid.port);

    logger.log("Sending packet");
    PacketPrinter.print(sendDatagram);

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

    // receive packets until we receive a packet from the correct host or time out
    do {
      buffer = new byte[517];
      receiveDatagram = new DatagramPacket(buffer, 517);

      logger.log("Waiting for response from client on port " + socket.getLocalPort());

      long tsStart = new Date().getTime();
      try {
        socket.receive(receiveDatagram);
      } catch (SocketTimeoutException e) {
        logger.logAlways("Response timed out.");
        throw e;
      } catch (IOException e) {
        e.printStackTrace();
        receiveDatagram = null;
        break;
      }
      long tsStop = new Date().getTime();

      logger.log("Packet received.");
      PacketPrinter.print(receiveDatagram);

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

        System.err.println("[CLIENT-CONNECTION] Waiting for another packet."); // Why is this an error message?
        receiveDatagram = null;
      }
    } while (receiveDatagram == null);

    return receiveDatagram;
  }

  private void handleInvalidTid(DatagramPacket receiveDatagram) {
    logger.logError("Received packet with wrong TID");
    logger.logError("  > Received:   " + receiveDatagram.getAddress() + " " + receiveDatagram.getPort());
    logger.logError("  > Expected:   " + clientTid.address + " " + clientTid.port);
    // respond to the rogue client with an appropriate error packet
    ErrorPacket errPacket = new ErrorPacketBuilder()
        .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
        .setMessage("Your request had an invalid TID.")
        .setRemoteHost(receiveDatagram.getAddress())
        .setRemotePort(receiveDatagram.getPort())
        .buildErrorPacket();

    logger.logError("Sending error to client with invalid TID\n" + errPacket.toString() + "\n");
    
    byte[] errData = errPacket.getPacketData();
    DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
        errPacket.getRemoteHost(), errPacket.getRemotePort());

    PacketPrinter.print(errDatagram);
    
    try {
      socket.send(errDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      logger.logError("Error sending error packet to unknown client. Ignoring this error.");
    }
  }
}