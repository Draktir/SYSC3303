package intermediate_host;

import java.net.*;

import Configuration.Configuration;
import packet.*;

public class TftpReadTransfer implements Runnable {
  private PacketParser packetParser = new PacketParser();
  private boolean transferComplete = false;
  private int lastBlockNumber = -1;

  private ConnectionManager clientConnection;
  private ConnectionManager serverConnnection;

  private RequestBuffer clientReceiveBuffer = new RequestBuffer();
  private RequestBuffer clientSendBuffer = new RequestBuffer();
  private RequestBuffer serverReceiveBuffer = new RequestBuffer();
  private RequestBuffer serverSendBuffer = new RequestBuffer();

  private ReadRequest request;
  private PacketModifier packetModifier;

  public TftpReadTransfer(ReadRequest request, PacketModifier packetModifier) {
    this.packetModifier = packetModifier;
    this.request = request;
  }

  @Override
  public void run() {
    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return;
    }

    // start the connection managers
    clientConnection =
        new ConnectionManager(clientReceiveBuffer, clientSendBuffer, request.getRemoteHost(), request.getRemotePort());
    serverConnnection =
        new ConnectionManager(serverReceiveBuffer, serverSendBuffer, localhost, Configuration.SERVER_PORT);

    Thread clientThread = new Thread(clientConnection, "TFTP-READ CLIENT");
    Thread serverThread = new Thread(serverConnnection, "TFTP-READ SERVER");

    clientThread.start();
    serverThread.start();

    boolean transferComplete = false;
    ForwardRequest clientRequest = null;
    ForwardRequest serverRequest = null;

    // to kick things off add the ReadRequest to the buffer
    byte[] rrqRaw = packetModifier.process(request, Configuration.INTERMEDIATE_PORT, Configuration.SERVER_PORT);
    if (rrqRaw == null) {
      // this request will be delayed/dropped. So we are done here.
      // another thread will be started if it's delayed
      return;
    }

    ForwardRequest rrqForward = new ForwardRequest(rrqRaw, localhost, Configuration.INTERMEDIATE_PORT);
    serverSendBuffer.putRequest(rrqForward);

    while (!transferComplete) {
      // will return after 200ms if buffer is still empty
      clientRequest = clientReceiveBuffer.takeRequest(200);
      if (clientRequest != null) {
        handleRequest(clientRequest, clientConnection.getRemotePort(), serverSendBuffer);
      }

      // will return after 200ms if buffer is still empty
      serverRequest = serverReceiveBuffer.takeRequest(200);
      if (serverRequest != null) {
        handleRequest(serverRequest, serverConnnection.getRemotePort(), clientSendBuffer);
      }
    }

    clientThread.interrupt();
    serverThread.interrupt();
  }

  private void handleRequest(ForwardRequest fwdRequest, int remotePort, RequestBuffer sendBuffer) {
    log("Request received. Forwarding...");

    DatagramPacket datagramPacket = fwdRequest.getDatagramPacket();
    Packet packet = null;
    try {
      packet = packetParser.parse(datagramPacket);
    } catch (InvalidPacketException e) {
      e.printStackTrace();
      System.err.println("This shouldn't happen. So let's get outta here!");
      this.transferComplete = true;
      return;
    }

    byte[] rawData = null;
    if (packet instanceof ReadRequest) {
      ReadRequest rrq = (ReadRequest) packet;
      rawData = packetModifier.process(rrq, fwdRequest.getReceivingPort(), remotePort);
      if (rawData != null) {
        log("Forwarding ReadRequest:");
        log(rrq.toString());
      }
    } else if (packet instanceof WriteRequest) {
      WriteRequest wrq = (WriteRequest) packet;
      rawData = packetModifier.process((WriteRequest) packet, fwdRequest.getReceivingPort(), remotePort);
      if (rawData != null) {
        log("Forwarding WriteRequest:");
        log(wrq.toString());
      }

    } else if (packet instanceof Acknowledgement) {
      Acknowledgement ack = (Acknowledgement) packet;
      rawData = packetModifier.process(ack, fwdRequest.getReceivingPort(), remotePort);
      // if we are sending the last ACK we are done after this
      this.transferComplete = (rawData != null && this.lastBlockNumber == ack.getBlockNumber());
      if (rawData != null) {
        log("Forwarding ACK:");
        log(ack.toString());
      }

    } else if (packet instanceof DataPacket) {
      DataPacket dp = (DataPacket) packet;
      rawData = packetModifier.process(dp, fwdRequest.getReceivingPort(), remotePort);
      // if this is the last block remember the block number
      this.lastBlockNumber = dp.getFileData().length < 512 ? dp.getBlockNumber() : -1;
      if (rawData != null) {
        log("Forwarding DataPacket");
        log(dp.toString());
      }

    } else if (packet instanceof ErrorPacket) {
      ErrorPacket errPacket = (ErrorPacket) packet;
      rawData = packetModifier.process(errPacket, fwdRequest.getReceivingPort(), remotePort);
      // if this is an error with code != 5, we are done after this
      this.transferComplete = (rawData != null && errPacket.getErrorCode() != ErrorPacket.ErrorCode.UNKNOWN_TRANSFER_ID);
      if (rawData != null) {
        log("Forwarding ErrorPacket");
        log(errPacket.toString());
      }

    }

    if (rawData == null) {
      // we are not sending this packet
      // (it's either delayed or dropped)
      return;
    }

    // set our potentially modified data and hand off
    // the request to the connection manager
    fwdRequest.setData(rawData);
    sendBuffer.putRequest(fwdRequest);
  }

  private void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[TFTP-READ: " + name + "] " + msg);
  }
}
