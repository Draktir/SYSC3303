package intermediate_host;

import java.net.InetAddress;

import packet.Packet;

public class ForwardMessage {
  private Packet packet;
  private InetAddress localAddress;
  private int receivingPort;
  
  public ForwardMessage(Packet packet, InetAddress localAddress, int receivingPort) {
    super();
    this.packet = packet;
    this.localAddress = localAddress;
    this.receivingPort = receivingPort;
  }
  public Packet getPacket() {
    return packet;
  }
  public void setPacket(Packet packet) {
    this.packet = packet;
  }
  public InetAddress getLocalAddress() {
    return localAddress;
  }
  public void setLocalAddress(InetAddress localAddress) {
    this.localAddress = localAddress;
  }
  public int getReceivingPort() {
    return receivingPort;
  }
  public void setReceivingPort(int receivingPort) {
    this.receivingPort = receivingPort;
  }
  
  @Override
  public String toString() {
    return "ForwardMessage [\n    packet=" + packet + ",\n    localAddress=" + localAddress + ",\n    receivingPort="
        + receivingPort + "\n]";
  }
}
