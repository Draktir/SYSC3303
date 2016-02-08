package modification;

public class PacketModification {
  protected int packetNumber;

  public PacketModification(int packetNumber) {
    this.packetNumber = packetNumber;
  }
  
  public int getPacketNumber() {
    return packetNumber;
  }

  public void setPacketNumber(int packetNumber) {
    this.packetNumber = packetNumber;
  }

  @Override
  public String toString() {
    return "Modification [packetNumber=" + packetNumber + "]";
  }
}
