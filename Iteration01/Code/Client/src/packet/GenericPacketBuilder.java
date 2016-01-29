package packet;

public class GenericPacketBuilder extends PacketBuilder<GenericPacketBuilder>{
  public GenericPacket buildGenericPacket() {
    return new GenericPacket(remoteHost, remotePort, packetData);
  }
}
