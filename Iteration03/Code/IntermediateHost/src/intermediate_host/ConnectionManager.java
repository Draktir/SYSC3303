package intermediate_host;

import java.io.IOException;
import java.net.*;

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
  }

  @Override
  public void run() {
    try {
      this.socket = new DatagramSocket();
      this.socket.setSoTimeout(200); // deliberately very short timeout
    } catch (SocketException e) {
      e.printStackTrace();
      return;
    }

    byte[] recvData = new byte[1024];
    DatagramPacket recvDatagram = null;

    while (!Thread.currentThread().isInterrupted() || sendBuffer.hasRequest()) {
      // Check if we have anything to send
      ForwardRequest forwardRequest = sendBuffer.takeRequest();
      if (forwardRequest != null) {
        sendRequest(forwardRequest);
      }

      // try to receive something
      recvDatagram = new DatagramPacket(recvData, recvData.length);
      try {
        socket.receive(recvDatagram);
      } catch (SocketTimeoutException e) {
        continue; // nothing received, back to the top
      } catch (IOException e) {
        e.printStackTrace();
      }

      log("");
      log("Received packet");
      IntermediateHost.printPacketInformation(recvDatagram);

      // we received something
      ForwardRequest receivedRequest = new ForwardRequest(recvDatagram, socket.getLocalAddress(), socket.getLocalPort());

      this.remoteHost = recvDatagram.getAddress();
      this.remotePort = recvDatagram.getPort();
      receiveBuffer.putRequest(receivedRequest);
    }

    log("Connection terminated.");
  }

  private void sendRequest(ForwardRequest forwardRequest) {
    byte[] forwardData = forwardRequest.getData();
    DatagramPacket datagram = new DatagramPacket(forwardData, forwardData.length,
        this.remoteHost, this.remotePort);

    log("Forwarding Packet:");
    IntermediateHost.printPacketInformation(datagram);

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

  public int getRemotePort() {
    return remotePort;
  }

  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  public InetAddress getRemoteHost() {
    return remoteHost;
  }

  public void setRemoteHost(InetAddress remoteHost) {
    this.remoteHost = remoteHost;
  }
}
