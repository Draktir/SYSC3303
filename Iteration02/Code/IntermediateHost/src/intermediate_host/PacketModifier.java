package intermediate_host;

import modification.AcknowledgementModification;
import modification.DataPacketModification;
import modification.ReadRequestModification;
import modification.WriteRequestModification;
import packet.Acknowledgement;
import packet.DataPacket;
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
  
  public byte[] process(ReadRequest readRequest) {
    packetCount++;
    
    if (rrqModification == null) {
      return readRequest.getPacketData();
    }
    return rrqModification.apply(readRequest);
  }
  
  public byte[] process(WriteRequest writeRequest) {
    packetCount++;
    
    if (wrqModification == null) {
      return writeRequest.getPacketData();
    }
    return wrqModification.apply(writeRequest);
  }
  
  public byte[] process(DataPacket dataPacket) {
    packetCount++;
    dataCount++;
    
    if (dataModification == null) {
      return dataPacket.getPacketData();
    }
    
    if (dataModification.getPacketNumber() == dataCount) {
      return dataModification.apply(dataPacket);
    }
    return dataPacket.getPacketData();
  }
  
  public byte[] process(Acknowledgement ackPacket) {
    packetCount++;
    ackCount++;

    if (ackModification == null) {
      return ackPacket.getPacketData();
    }
    
    if (ackModification.getPacketNumber() == ackCount) {
      return ackModification.apply(ackPacket);
    }
    return ackPacket.getPacketData();
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
