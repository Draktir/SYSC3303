package intermediate_host;

import java.net.*;
import java.util.function.Consumer;

import configuration.Configuration;
import packet.*;
import packet.Request.RequestType;

public class TftpTransfer implements Runnable {
  private boolean transferComplete = false;
  private int lastBlockNumber = -1;

  // make these references immutable so it's safe to use them from other Threads 
  private final ConnectionManager clientConnection; // initialized in Constructor
  private final ConnectionManager serverConnnection; // initialized in Constructor
  private final RequestBuffer clientReceiveBuffer = new RequestBuffer();
  private final RequestBuffer clientSendBuffer = new RequestBuffer();
  private final RequestBuffer serverReceiveBuffer = new RequestBuffer();
  private final RequestBuffer serverSendBuffer = new RequestBuffer();
  private final DatagramPacket requestDatagram;
  private final PacketModifier packetModifier;

  public TftpTransfer(DatagramPacket requestDatagram, InetAddress serverAddress, PacketModifier packetModifier) {
    this.packetModifier = packetModifier;
    this.requestDatagram = requestDatagram;
    
    InetAddress clientAddress = requestDatagram.getAddress();
   
    this.clientConnection = new ConnectionManager(
    		clientReceiveBuffer, clientSendBuffer, clientAddress, requestDatagram.getPort());
    this.serverConnnection = new ConnectionManager(
    		serverReceiveBuffer, serverSendBuffer, serverAddress, Configuration.get().SERVER_PORT);
  }

  @Override
  public void run() {
    // figure out if we got a valid request
    PacketParser packetParser = new PacketParser();
    Request recvdRequest = null;
    try {
      recvdRequest = packetParser.parseRequest(requestDatagram);
    } catch (InvalidRequestException e1) {
      e1.printStackTrace();
      System.err.println("Received packet is not a valid RRQ or WRQ. Ignore it.");
      return;
    }
    
    if (recvdRequest.type() == RequestType.READ) {
      log("Received valid ReadRequest:");
      log(recvdRequest.toString());
    } else if (recvdRequest.type() == RequestType.WRITE) {
      log("Received valid WriteRequest:");
      log(recvdRequest.toString());
    }
    
    log("Initiating new TFTP transfer");
    
    // start the connection managers
    Thread clientConnectionThread = new Thread(clientConnection, "CLIENT-CONNECTION");
    Thread serverConnectionThread = new Thread(serverConnnection, "SERVER-CONNECTION");

    clientConnectionThread.start();
    serverConnectionThread.start();
    
    // start the request handlers. These wait for a request to be put
    // into their respective buffer and then process it.
    Thread clientHandler = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        ForwardRequest clientRequest = clientReceiveBuffer.takeRequest();
        handleRequest(clientRequest, serverConnnection);
      }
    }, "CLIENT-HANDLER");
        
    Thread serverHandler = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        ForwardRequest serverRequest = serverReceiveBuffer.takeRequest();
        handleRequest(serverRequest, clientConnection);
      }
    }, "SERVER-HANDLER");
    
    clientHandler.start();
    serverHandler.start();
      
    // to kick things off add the original request to the client receive buffer
    ForwardRequest reqForward = 
        new ForwardRequest(this.requestDatagram, recvdRequest.getRemoteHost(), this.clientConnection.getLocalPort());
    clientReceiveBuffer.putRequest(reqForward);
    
    // wait for the transfer to complete
    while (!this.getTransferComplete()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    clientConnectionThread.interrupt();
    serverConnectionThread.interrupt();
    clientHandler.interrupt();
    serverHandler.interrupt();
    
    log("Transfer has ended");
  }
 
  /**
   * This method is called from other threads. However, it does not need to be
   * synchronized. Whenever we need to access local state we call synchronized
   * local methods (set/getTransferComplete or set/getLastBlockNumber).
   * Any other data sources are themselves synchronized.
   * 
   * @param fwdRequest
   * @param remotePort
   * @param sendBuffer
   */
  private void handleRequest(ForwardRequest fwdRequest, ConnectionManager connection) {
    log("Request received. Forwarding...");
    final PacketParser packetParser = new PacketParser();
    final DatagramPacket datagramPacket = fwdRequest.getDatagramPacket();
    final int remotePort = connection.getRemotePort();
    
    Packet packet = null;
    try {
      packet = packetParser.parse(datagramPacket);
    } catch (InvalidPacketException e) {
      e.printStackTrace();
      System.err.println("This shouldn't happen. So let's get outta here!");
      this.setTransferComplete(true);
      return;
    }

    byte[] rawData = null;
    if (packet instanceof ReadRequest) {
      ReadRequest rrq = (ReadRequest) packet;
      // delayed/duplicated RRQs are handled in the IntermediateHost class
      rawData = packetModifier.process(rrq, fwdRequest.getReceivingPort(), connection.getRemoteHost(), remotePort, (p) -> {});
      if (rawData != null) {
        log("Forwarding ReadRequest:");
        log(rrq.toString());
      }
      
    } else if (packet instanceof WriteRequest) {
      WriteRequest wrq = (WriteRequest) packet;
      // delayed/duplicated WRQs are handled in the IntermediateHost class
      rawData = packetModifier.process(wrq, fwdRequest.getReceivingPort(), connection.getRemoteHost(), remotePort, (p) -> {});
      if (rawData != null) {
        log("Forwarding WriteRequest:");
        log(wrq.toString());
      }

    } else if (packet instanceof Acknowledgement) {
      Acknowledgement ack = (Acknowledgement) packet;
      rawData = packetModifier.process(ack, fwdRequest.getReceivingPort(), connection.getRemoteHost(), remotePort, buildPacketConsumer(connection));
      // if we are sending the last ACK we are done after this
      if (rawData != null && this.getLastBlockNumber() == ack.getBlockNumber()) {
        this.setTransferComplete(true);
      }
      if (rawData != null) {
        log("Forwarding ACK:");
        log(ack.toString());
      }

    } else if (packet instanceof DataPacket) {
      DataPacket dp = (DataPacket) packet;
      rawData = packetModifier.process(dp, fwdRequest.getReceivingPort(), connection.getRemoteHost(), remotePort, buildPacketConsumer(connection));
      // if this is the last block, remember the block number
      if (dp.getFileData().length < 512) {
        this.setLastBlockNumber(dp.getBlockNumber());
      }
      if (rawData != null) {
        log("Forwarding DataPacket");
        log(dp.toString());
      }

    } else if (packet instanceof ErrorPacket) {
      ErrorPacket errPacket = (ErrorPacket) packet;
      rawData = packetModifier.process(errPacket, fwdRequest.getReceivingPort(), connection.getRemoteHost(), remotePort, buildPacketConsumer(connection));
      // if this is an error we are done after this
      if (rawData != null) {
        this.setTransferComplete(true);
      }
      if (rawData != null) {
        log("Forwarding ErrorPacket");
        log(errPacket.toString());
      }

    }

    if (rawData == null) {
      // we are not sending this packet
      // (it's either delayed or dropped)
      log("Not sending " + packet.toString());
      return;
    }

    // set our potentially modified data and hand off
    // the request to the connection manager
    fwdRequest.setData(rawData);
    connection.getSendBuffer().putRequest(fwdRequest);
  }

  
  private Consumer<Packet> buildPacketConsumer(final ConnectionManager connection) {
    /**
     * Called from the Packet Modification after a packet has been
     * delayed. It will add the delayed packet to the appropriate
     * send buffer so it can be forwarded.
     */
  	return (packet) -> {
  		ForwardRequest fwRequest = new ForwardRequest(
  				packet.getPacketData(), packet.getRemoteHost(), connection.getRemotePort());
  		connection.getSendBuffer().putRequest(fwRequest);
  	};
  }
  
  private synchronized void setTransferComplete(boolean transferComplete) {
    this.transferComplete = transferComplete;
  }
  
  private synchronized boolean getTransferComplete() {
    return this.transferComplete;
  }
  
  private synchronized void setLastBlockNumber(int blockNumber) {
    this.lastBlockNumber = blockNumber;
  }
  
  private synchronized int getLastBlockNumber() {
    return this.lastBlockNumber;
  }
  
  private void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[TFTP: " + name + "] " + msg);
  }
}
