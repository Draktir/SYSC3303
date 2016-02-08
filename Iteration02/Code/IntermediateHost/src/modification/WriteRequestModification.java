package modification;

import java.util.Arrays;

public class WriteRequestModification extends PacketModification {
  byte[] opcode = null;
  byte[] filename = null;
  byte zeroByteAfterFilename = -1;
  byte[] mode = null;
  byte zeroByteAfterMode = -1;
  
  public WriteRequestModification(int packetNumber) {
    super(packetNumber);
  }

  public byte[] getOpcode() {
    return opcode;
  }

  public void setOpcode(byte[] opcode) {
    this.opcode = opcode;
  }

  public byte[] getFilename() {
    return filename;
  }

  public void setFilename(byte[] filename) {
    this.filename = filename;
  }

  public byte getZeroByteAfterFilename() {
    return zeroByteAfterFilename;
  }

  public void setZeroByteAfterFilename(byte zeroByteAfterFilename) {
    this.zeroByteAfterFilename = zeroByteAfterFilename;
  }

  public byte[] getMode() {
    return mode;
  }

  public void setMode(byte[] mode) {
    this.mode = mode;
  }

  public byte getZeroByteAfterMode() {
    return zeroByteAfterMode;
  }

  public void setZeroByteAfterMode(byte zeroByteAfterMode) {
    this.zeroByteAfterMode = zeroByteAfterMode;
  }

  @Override
  public String toString() {
    return "WriteRequestModification [opcode=" + Arrays.toString(opcode) + ", filename=" + Arrays.toString(filename)
        + ", zeroByteAfterFilename=" + zeroByteAfterFilename + ", mode=" + Arrays.toString(mode)
        + ", zeroByteAfterMode=" + zeroByteAfterMode + ", packetNumber=" + packetNumber + "]";
  }
}
