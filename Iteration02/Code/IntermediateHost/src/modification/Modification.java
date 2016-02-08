package modification;

public class Modification {
  protected int packetNumber;

  public Modification(int packetNumber) {
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
