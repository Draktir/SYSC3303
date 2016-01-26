package packet;

import java.net.InetAddress;

public abstract class Packet {
  protected InetAddress remoteHost;
  protected int remotePort;
  protected InetAddress localHost;
  protected int localPort;
  
  public Packet(InetAddress remoteHost, int remotePort, InetAddress localHost, int localPort) {
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.localHost = localHost;
    this.localPort = localPort;
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

  public InetAddress getLocalHost() {
    return localHost;
  }

  public void setLocalHost(InetAddress localHost) {
    this.localHost = localHost;
  }

  public int getLocalPort() {
    return localPort;
  }

  public void setLocalPort(int localPort) {
    this.localPort = localPort;
  }

  @Override
  public String toString() {
    return "Packet [remoteHost=" + remoteHost + ", remotePort=" + remotePort + ", localHost=" + localHost
        + ", localPort=" + localPort + "]";
  }
}
