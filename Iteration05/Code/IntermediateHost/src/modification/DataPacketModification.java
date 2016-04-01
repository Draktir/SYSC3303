package modification;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import packet.DataPacket;
import packet.Packet;

public class DataPacketModification extends PacketModification {
  byte[] opcode = null;
  byte[] blockNumber = null;
  byte[] data = null;
  
  public DataPacketModification(int packetNumber) {
    super(packetNumber);
  }

  @Override
  public byte[] apply(Packet packet, int localReceivePort, InetAddress receiverAddress, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    if (super.tidModification != null) {
      super.performTidModification(packet, receiverAddress, remoteReceivePort);
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
    
    DataPacket dataPacket = (DataPacket) packet;
    List<Byte> modified = new ArrayList<>();
    
    System.out.println("\n[DATA-Modification] Applying modification: ");
    System.out.println("  > Original:     " + dataPacket.toString());
    System.out.println("  > Modification: " + this.toString() + "\n");
    
    if (opcode != null) {
      modified.addAll(PacketModification.byteArrayToList(opcode));
    } else {
      modified.addAll(PacketModification.byteArrayToList(dataPacket.getOpcode()));
    }
    
    if (blockNumber != null) {
      modified.addAll(PacketModification.byteArrayToList(blockNumber));
    } else {
      BigInteger bInt = BigInteger.valueOf(dataPacket.getBlockNumber());
      byte[] bnBytes = bInt.toByteArray();
      if (bnBytes.length < 2) {
        bnBytes = new byte[] {0, bnBytes[0]};
      } else {
        // we only want the last two bytes
        bnBytes = new byte[] {bnBytes[bnBytes.length - 2], bnBytes[bnBytes.length - 1]};
      }
      modified.addAll(PacketModification.byteArrayToList(bnBytes));
    }
    
    if (data != null) {
      modified.addAll(PacketModification.byteArrayToList(data));
    } else {
      modified.addAll(PacketModification.byteArrayToList(dataPacket.getFileData()));
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
    return "DataPacketModification [\n    opcode=" + Arrays.toString(opcode) + ",\n    blockNumber="
        + Arrays.toString(blockNumber) + ",\n    data=" + Arrays.toString(data) + ",\n    packetNumber=" + packetNumber
        + ",\n    appendToEnd=" + Arrays.toString(appendToEnd) + ",\n    tidModification=" + tidModification
        + ",\n    delayModification=" + delayModification + ",\n    dropModification=" + dropModification
        + ",\n    duplicatePacketModification=" + duplicatePacketModification + "\n]";
  }
}
