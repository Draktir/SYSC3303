package modification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import packet.Packet;
import packet.ReadRequest;

public class ReadRequestModification extends PacketModification {
  byte[] opcode = null;
  byte[] filename = null;
  byte[] zeroByteAfterFilename = null;
  byte[] mode = null;
  byte[] zeroByteAfterMode = null;
  
  public ReadRequestModification() {
    super(1);
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
    
    ReadRequest readRequest = (ReadRequest) packet;

    System.out.println("\n[RRQ-Modification] Applying modification: ");
    System.out.println("  > Original:     " + readRequest.toString());
    System.out.println("  > Modification: " + this.toString() + "\n");
    
    List<Byte> modified = new ArrayList<>();
    
    if (opcode != null) {
      modified.addAll(PacketModification.byteArrayToList(opcode));
    } else {
      modified.addAll(PacketModification.byteArrayToList(readRequest.getOpcode()));
    }
    
    if (filename != null) {
      modified.addAll(PacketModification.byteArrayToList(filename));
    } else {
      modified.addAll(PacketModification.byteArrayToList(readRequest.getFilename().getBytes()));
    }
    
    if (zeroByteAfterFilename != null) {
      modified.addAll(PacketModification.byteArrayToList(zeroByteAfterFilename));
    } else {
      modified.add(new Byte((byte) 0));
    }
    
    if (mode != null) {
      modified.addAll(PacketModification.byteArrayToList(mode));
    } else {
      modified.addAll(PacketModification.byteArrayToList(readRequest.getMode().getBytes()));
    }
    
    if (zeroByteAfterMode != null) {
      modified.addAll(PacketModification.byteArrayToList(zeroByteAfterMode));
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

  public byte[] getFilename() {
    return filename;
  }

  public void setFilename(byte[] filename) {
    this.filename = filename;
  }

  public byte[] getZeroByteAfterFilename() {
    return zeroByteAfterFilename;
  }

  public void setZeroByteAfterFilename(byte[] zeroByteAfterFilename) {
    this.zeroByteAfterFilename = zeroByteAfterFilename;
  }

  public byte[] getMode() {
    return mode;
  }

  public void setMode(byte[] mode) {
    this.mode = mode;
  }

  public byte[] getZeroByteAfterMode() {
    return zeroByteAfterMode;
  }

  public void setZeroByteAfterMode(byte[] zeroByteAfterMode) {
    this.zeroByteAfterMode = zeroByteAfterMode;
  }

  @Override
  public String toString() {
    return "ReadRequestModification [\n    opcode=" + Arrays.toString(opcode) + ",\n    filename="
        + Arrays.toString(filename) + ",\n    zeroByteAfterFilename=" + Arrays.toString(zeroByteAfterFilename)
        + ",\n    mode=" + Arrays.toString(mode) + ",\n    zeroByteAfterMode=" + Arrays.toString(zeroByteAfterMode)
        + ",\n    packetNumber=" + packetNumber + ",\n    appendToEnd=" + Arrays.toString(appendToEnd)
        + ",\n    tidModification=" + tidModification + ",\n    delayModification=" + delayModification
        + ",\n    dropModification=" + dropModification + ",\n    duplicatePacketModification="
        + duplicatePacketModification + "\n]";
  }
}
