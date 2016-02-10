package modification;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import packet.Acknowledgement;
import packet.Packet;

public class AcknowledgementModification extends PacketModification {
  private byte[] opcode = null;
  private byte[] blockNumber = null;
  
  public AcknowledgementModification(int packetNumber) {
    super(packetNumber);
  }

  @Override
  public byte[] apply(Packet packet) {
    Acknowledgement ackPacket = (Acknowledgement) packet;
    List<Byte> modified = new ArrayList<>();
    
    System.out.println("Applying modification: ");
    System.out.println(" - Original:     " + ackPacket.toString());
    System.out.println(" - Modification: " + this.toString());
    
    if (opcode != null) {
      modified.addAll(PacketModification.byteArrayToList(opcode));
    } else {
      modified.addAll(PacketModification.byteArrayToList(ackPacket.getOpcode()));
    }
    
    if (blockNumber != null) {
      modified.addAll(PacketModification.byteArrayToList(blockNumber));
    } else {
      BigInteger bInt = BigInteger.valueOf(ackPacket.getBlockNumber());
      byte[] bnBytes = bInt.toByteArray();
      if (bnBytes.length < 2) {
        bnBytes = new byte[] {0, bnBytes[0]};
      } else {
        // we only want the last two bytes
        bnBytes = new byte[] {bnBytes[bnBytes.length - 2], bnBytes[bnBytes.length - 1]};
      }
      modified.addAll(PacketModification.byteArrayToList(bnBytes));
    }
    
    return PacketModification.byteListToArray(modified);
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
