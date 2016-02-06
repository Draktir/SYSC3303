package packet;


import java.util.Arrays;

public class GenericPacketBuilder extends PacketBuilder<GenericPacketBuilder>{
  byte[] packetData;
  
  public GenericPacket buildGenericPacket() {
    return new GenericPacket(remoteHost, remotePort, packetData);
  }

  public byte[] getPacketData() {
    return packetData;
  }

  public GenericPacketBuilder setPacketData(byte[] packetData) {
    this.packetData = packetData;
    return this;
  }

  @Override
  public String toString() {
    return "GenericPacketBuilder [packetData=" + Arrays.toString(packetData) + ", remoteHost=" + remoteHost
        + ", remotePort=" + remotePort + "]";
  }
}
