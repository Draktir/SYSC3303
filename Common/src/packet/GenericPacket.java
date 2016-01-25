package packet;

import java.net.InetAddress;
import java.util.Arrays;

public class GenericPacket extends Packet {
  byte[] packetData;
  
  public GenericPacket(InetAddress remoteHost, int remotePort, InetAddress localHost, int localPort, byte[] packetData) {
    super(remoteHost, remotePort, localHost, localPort);
    this.packetData = packetData;
  }

  @Override
  public byte[] getPacketData() {
    return packetData;
  }
  
  public void setPacketData(byte[] packetData) {
    this.packetData = packetData;
  }

  @Override
  public String toString() {
    return "GenericPacket [packetData=" + Arrays.toString(packetData) + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + ", localHost=" + localHost + ", localPort=" + localPort + "]";
  }
}
