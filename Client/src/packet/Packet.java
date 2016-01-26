package packet;

import java.net.InetAddress;

public abstract class Packet {
  protected InetAddress remoteHost;
  protected int remotePort;
  
  public Packet(InetAddress remoteHost, int remotePort) {
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
  }
  
  public abstract byte[] getPacketData();

  public InetAddress getRemoteHost() {
    return remoteHost;
  }

  public void setRemoteHost(InetAddress remoteHost) {
    this.remoteHost = remoteHost;
  }

  public int getRemotePort() {
    return remotePort;
  }

  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  @Override
  public String toString() {
    return "Packet [remoteHost=" + remoteHost + ", remotePort=" + remotePort + "]";
  }
}
