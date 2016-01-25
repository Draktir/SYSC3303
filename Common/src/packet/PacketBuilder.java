package packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class PacketBuilder {
  protected InetAddress remoteHost = findLocalHost();
  protected int remotePort = 69;
  protected InetAddress localHost = findLocalHost();
  protected int localPort = 69;
  protected byte[] packetData = new byte[0];
  
  public GenericPacket buildGenericPacket() {
    return new GenericPacket(remoteHost, remotePort, localHost, localPort, packetData);
  }
  
  private InetAddress findLocalHost() {
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return null;
    }
  }
  
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
  
  public byte[] getPacketData() {
    return packetData;
  }
  
  public void setPacketData(byte[] packetData) {
    this.packetData = packetData;
  }

  @Override
  public String toString() {
    return "PacketBuilder [remoteHost=" + remoteHost + ", remotePort=" + remotePort + ", localHost=" + localHost
        + ", localPort=" + localPort + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
