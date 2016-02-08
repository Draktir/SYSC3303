package modification;

import java.util.Arrays;

public class AcknowledgementModification extends Modification {
  private byte[] opcode = null;
  private byte[] blockNumber = null;
  
  public AcknowledgementModification(int packetNumber) {
    super(packetNumber);
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

  @Override
  public String toString() {
    return "AcknowledgementModification [opcode=" + Arrays.toString(opcode) + ", blockNumber="
        + Arrays.toString(blockNumber) + ", packetNumber=" + packetNumber + "]";
  }
}
