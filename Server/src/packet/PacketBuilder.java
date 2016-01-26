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
  
  public PacketBuilder setRemoteHost(InetAddress remoteHost) {
    this.remoteHost = remoteHost;
    return this;
  }
  
  public int getRemotePort() {
    return remotePort;
  }
  
  public PacketBuilder setRemotePort(int remotePort) {
    this.remotePort = remotePort;
    return this;
  }
  
  public InetAddress getLocalHost() {
    return localHost;
  }
  
  public PacketBuilder setLocalHost(InetAddress localHost) {
    this.localHost = localHost;
    return this;
  }
  
  public int getLocalPort() {
    return localPort;
  }
  
  public PacketBuilder setLocalPort(int localPort) {
    this.localPort = localPort;
    return this;
  }
  
  public byte[] getPacketData() {
    return packetData;
  }
  
  public PacketBuilder setPacketData(byte[] packetData) {
    this.packetData = packetData;
    return this;
  }

  @Override
  public String toString() {
    return "PacketBuilder [remoteHost=" + remoteHost + ", remotePort=" + remotePort + ", localHost=" + localHost
        + ", localPort=" + localPort + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
