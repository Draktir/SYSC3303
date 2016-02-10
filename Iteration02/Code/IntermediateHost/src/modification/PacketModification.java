package modification;

import java.util.Arrays;
import java.util.List;

import packet.Packet;

public abstract class PacketModification {
  protected int packetNumber;

  public PacketModification(int packetNumber) {
    this.packetNumber = packetNumber;
  }

  public abstract byte[] apply(Packet packet);
  
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
  
  public static List<Byte> byteArrayToList(byte[] arr) {
    Byte[] bytes = new Byte[arr.length];
    for (int i = 0; i < arr.length; i++) {
      bytes[i] = new Byte(arr[i]);
    }
    return Arrays.asList(bytes);
  }
  
  public static byte[] byteListToArray(List<Byte> list) {
    byte[] arr = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i).byteValue();
    }
    return arr;
  }
}
