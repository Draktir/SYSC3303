package modification;

import java.util.Arrays;

import packet.Packet;

public class DataPacketModification extends PacketModification {
  byte[] opcode = null;
  byte[] blockNumber = null;
  byte[] data = null;
  
  public DataPacketModification(int packetNumber) {
    super(packetNumber);
  }

  @Override
  public byte[] apply(Packet packet) {
    // TODO Auto-generated method stub
    return null;
  }

  public byte[] getOpcode() {
    return opcode;
  }

  public void setOpcode(byte[] opcode) {
    this.opcode = opcode;
  }

  public byte[] getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(byte[] blockNumber) {
    this.blockNumber = blockNumber;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "DataPacketModification [opcode=" + Arrays.toString(opcode) + ", blockNumber=" + Arrays.toString(blockNumber)
        + ", data=" + Arrays.toString(data) + ", packetNumber=" + packetNumber + "]";
  }
}
