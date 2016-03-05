package modification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import packet.ErrorPacket;
import packet.Packet;

public class ErrorPacketModification extends PacketModification {
  byte[] opcode = null;
  byte[] errorCode = null;  
  byte[] errorMessge = null;
  byte[] zeroByteAfterMessage = null;
  
  public ErrorPacketModification(int packetNumber) {
    super(packetNumber);
  }
  
  public byte[] apply(Packet packet, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    if (super.tidModification != null) {
      super.performTidModification(packet, remoteReceivePort);
    }
    
    if (super.delayModification != null) {
      super.performDelayPacketModification(packet, localReceivePort, delayedPacketConsumer);
      return null;
    }
    
    if (super.dropModification != null) {
      System.out.println("Dropping packet: " + packet.toString());
      return null;
    }
    
    if (super.duplicatePacketModification != null) {
      super.performDuplicatePacketModification(packet, localReceivePort, delayedPacketConsumer);
    }
    
    ErrorPacket errorPacket = (ErrorPacket) packet;
    
    System.out.println("\n[ERROR-Modification] Applying modification: ");
    System.out.println("  > Original:     " + errorPacket.toString());
    System.out.println("  > Modification: " + this.toString() + "\n");
    
    List<Byte> modified = new ArrayList<>();
    
    if (opcode != null) {
      modified.addAll(PacketModification.byteArrayToList(opcode));
    } else {
      modified.addAll(PacketModification.byteArrayToList(errorPacket.getOpcode()));
    }
    
    if (errorCode != null) {
      modified.addAll(PacketModification.byteArrayToList(errorCode));
    } else {
      byte[] errCode = {0, (byte) errorPacket.getErrorCode().getValue()};
      
      System.out.println("ERROR CODE: " + Arrays.toString(errCode));
      modified.addAll(PacketModification.byteArrayToList(errCode));
    }
    
    if (errorMessge != null) {
      modified.addAll(PacketModification.byteArrayToList(errorMessge));
    } else {
      modified.addAll(PacketModification.byteArrayToList(errorPacket.getMessage().getBytes()));
    }
    
    if (zeroByteAfterMessage != null) {
      modified.addAll(PacketModification.byteArrayToList(zeroByteAfterMessage));
    } else {
      modified.add(new Byte((byte) 0));
    }
    
    if (appendToEnd != null) {
      modified.addAll(PacketModification.byteArrayToList(appendToEnd));
    }

    return PacketModification.byteListToArray(modified);
  }

  public byte[] getOpcode() {
    return opcode;
  }

  public void setOpcode(byte[] opcode) {
    this.opcode = opcode;
  }

  public byte[] getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(byte[] errorCode) {
    this.errorCode = errorCode;
  }

  public byte[] getErrorMessge() {
    return errorMessge;
  }

  public void setErrorMessge(byte[] errorMessge) {
    this.errorMessge = errorMessge;
  }

  public byte[] getZeroByteAfterMessage() {
    return zeroByteAfterMessage;
  }

  public void setZeroByteAfterMessage(byte[] zeroByteAfterMessage) {
    this.zeroByteAfterMessage = zeroByteAfterMessage;
  }

  @Override
  public String toString() {
    return "ErrorPacketModification [\n    opcode=" + Arrays.toString(opcode) + ",\n    errorCode="
        + Arrays.toString(errorCode) + ",\n    errorMessge=" + Arrays.toString(errorMessge)
        + ",\n    zeroByteAfterMessage=" + Arrays.toString(zeroByteAfterMessage) + ",\n    packetNumber=" + packetNumber
        + ",\n    appendToEnd=" + Arrays.toString(appendToEnd) + ",\n    tidModification=" + tidModification
        + ",\n    delayModification=" + delayModification + ",\n    dropModification=" + dropModification
        + ",\n    duplicatePacketModification=" + duplicatePacketModification + "\n]";
  }
}
