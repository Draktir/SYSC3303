package intermediate_host;

import modification.AcknowledgementModification;
import modification.DataPacketModification;
import modification.ReadRequestModification;
import modification.WriteRequestModification;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.Packet;
import packet.ReadRequest;
import packet.WriteRequest;

public class PacketModifier {
  ReadRequestModification rrqModification = null;
  WriteRequestModification wrqModification = null;
  DataPacketModification dataModification = null;
  AcknowledgementModification ackModification = null;
  
  int packetCount = 0;
  int dataCount = 0;
  int ackCount = 0;
  
  public byte[] process(Packet packet, int localReceivePort, int remoteReceivePort) {
    packetCount++;
    // we don't make any modifications hede since this is an
    // unexpected packet type and an error has already been
    // introduced (either delay or dropped packet).
    return packet.getPacketData();    
  }
  
  public byte[] process(ReadRequest readRequest, int localReceivePort, int remoteReceivePort) {
    packetCount++;
    
    if (rrqModification == null) {
      return readRequest.getPacketData();
    }
    byte[] result = rrqModification.apply(readRequest, localReceivePort, remoteReceivePort);
    // we only make a delay/drop modification once
    rrqModification.setDelayModification(null);
    rrqModification.setDropModification(null);
    return result;
  }
  
  public byte[] process(WriteRequest writeRequest, int localReceivePort, int remoteReceivePort) {
    packetCount++;
    
    if (wrqModification == null) {
      return writeRequest.getPacketData();
    }
    byte[] result = wrqModification.apply(writeRequest, localReceivePort, remoteReceivePort);
    // we only make a delay/drop modification once
    wrqModification.setDelayModification(null);
    wrqModification.setDropModification(null);
    return result;
  }
  
  public byte[] process(DataPacket dataPacket, int localReceivePort, int remoteReceivePort) {
    packetCount++;
    dataCount++;
    
    if (dataModification == null) {
      return dataPacket.getPacketData();
    }
    
    if (dataModification.getPacketNumber() == dataCount) {
      return dataModification.apply(dataPacket, localReceivePort, remoteReceivePort);
    }
    byte[] result = dataPacket.getPacketData();
    // we only make a delay/drop modification once
    dataModification.setDelayModification(null);
    dataModification.setDropModification(null);
    return result;
  }
  
  public byte[] process(Acknowledgement ackPacket, int localReceivePort, int remoteReceivePort) {
    packetCount++;
    ackCount++;

    if (ackModification == null) {
      return ackPacket.getPacketData();
    }
    
    if (ackModification.getPacketNumber() == ackCount) {
      return ackModification.apply(ackPacket, localReceivePort, remoteReceivePort);
    }
    byte[] result = ackPacket.getPacketData();
    // we only make a delay/drop modification once
    ackModification.setDelayModification(null);
    ackModification.setDropModification(null);
    return result;
  }

  public ReadRequestModification getRrqModification() {
    return rrqModification;
  }

  public void setRrqModification(ReadRequestModification rrqModification) {
    this.rrqModification = rrqModification;
  }

  public WriteRequestModification getWrqModification() {
    return wrqModification;
  }

  public void setWrqModification(WriteRequestModification wrqModification) {
    this.wrqModification = wrqModification;
  }

  public DataPacketModification getDataModification() {
    return dataModification;
  }

  public void setDataModification(DataPacketModification dataModification) {
    this.dataModification = dataModification;
  }

  public AcknowledgementModification getAckModification() {
    return ackModification;
  }

  public void setAckModification(AcknowledgementModification ackModification) {
    this.ackModification = ackModification;
  }

  public int getPacketCount() {
    return packetCount;
  }

  public void setPacketCount(int packetCount) {
    this.packetCount = packetCount;
  }

  public int getDataCount() {
    return dataCount;
  }

  public void setDataCount(int dataCount) {
    this.dataCount = dataCount;
  }

  public int getAckCount() {
    return ackCount;
  }

  public void setAckCount(int ackCount) {
    this.ackCount = ackCount;
  }

  @Override
  public String toString() {
    return "PacketModifier [rrqModification=" + rrqModification + ", wrqModification=" + wrqModification
        + ", dataModification=" + dataModification + ", ackModification=" + ackModification + ", packetCount="
        + packetCount + ", dataCount=" + dataCount + ", ackCount=" + ackCount + "]";
  }
}
