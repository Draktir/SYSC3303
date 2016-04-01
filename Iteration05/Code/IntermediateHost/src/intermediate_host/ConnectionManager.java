package intermediate_host;

import java.io.IOException;
import java.net.*;

import utils.PacketPrinter;

public class ConnectionManager implements Runnable {
  private DatagramSocket socket;

  private RequestBuffer receiveBuffer;
  private RequestBuffer sendBuffer;
  InetAddress remoteHost;
  int remotePort;

  public ConnectionManager(RequestBuffer receiveBuffer, RequestBuffer sendBuffer,
                           InetAddress remoteHost, int remotePort) {
    this.receiveBuffer = receiveBuffer;
    this.sendBuffer = sendBuffer;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    
    try {
      this.socket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
      return;
    }
  }

  @Override
  public void run() {
    // start a receiveHandler Thread for incoming requests
    String name = Thread.currentThread().getName();
    Thread recvThread = new Thread(this.packetReceiver, name + " recv");
    recvThread.start();   

    // send any requests from the sendBuffer
    while (!Thread.currentThread().isInterrupted() || sendBuffer.hasRequest()) {
      // Check if we have anything to send
      ForwardRequest forwardRequest = sendBuffer.takeRequest();
      sendRequest(forwardRequest);
    }

    this.socket.close(); // will cause the receive handler to shut down as well
    log("Connection terminated.");
    
  }
  
  private Runnable packetReceiver = () -> {
    while (!Thread.currentThread().isInterrupted()) {
      byte[] recvData = new byte[1024];
      DatagramPacket recvDatagram = new DatagramPacket(recvData, recvData.length);
      log("Waiting to receive on port " + socket.getLocalPort());
      try {
        socket.receive(recvDatagram);
      } catch (IOException e) {
        log("ReceiveHandler shutting down");
        return;
      }

      log("");
      log("Received packet");
      PacketPrinter.print(recvDatagram);

      ForwardRequest receivedRequest = new ForwardRequest(recvDatagram, socket.getLocalAddress(), socket.getLocalPort());
      
      this.setRemoteHost(recvDatagram.getAddress());
      this.setRemotePort(recvDatagram.getPort());
      this.receiveBuffer.putRequest(receivedRequest); 
    }
  };
  

  private void sendRequest(ForwardRequest forwardRequest) {
    byte[] forwardData = forwardRequest.getData();
    DatagramPacket datagram = new DatagramPacket(forwardData, forwardData.length,
        this.getRemoteHost(), this.getRemotePort());
    
    log("Forwarding Packet:");
    PacketPrinter.print(datagram);

    try {
      this.socket.send(datagram);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[" + name + "] " + msg);
  }

  public synchronized int getRemotePort() {
    return remotePort;
  }

  public synchronized void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  public synchronized InetAddress getRemoteHost() {
    return remoteHost;
  }

  public synchronized void setRemoteHost(InetAddress remoteHost) {
    this.remoteHost = remoteHost;
  }
  
  public synchronized int getLocalPort() {
    return this.socket.getLocalPort();
  }
  
  public synchronized RequestBuffer getReceiveBuffer() {
  	return this.receiveBuffer;
  }
  
  public synchronized RequestBuffer getSendBuffer() {
  	return this.sendBuffer;
  }
}
