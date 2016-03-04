package intermediate_host;

import java.util.function.Consumer;

import modification.AcknowledgementModification;
import modification.DataPacketModification;
import modification.ErrorPacketModification;
import modification.ReadRequestModification;
import modification.WriteRequestModification;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.Packet;
import packet.ReadRequest;
import packet.WriteRequest;

public class PacketModifier {
  ReadRequestModification rrqModification = null;
  WriteRequestModification wrqModification = null;
  DataPacketModification dataModification = null;
  AcknowledgementModification ackModification = null;
  ErrorPacketModification errorModification = null;
  
  int packetCount = 0;
  int rrqCount = 0;
  int wrqCount = 0;
  int dataCount = 0;
  int ackCount = 0;
  int errCount = 0;
  
  public byte[] process(Packet packet, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    // we don't make any modifications here since this is an
    // unexpected packet type and an error has already been
    // introduced (either delay or dropped packet).
    return packet.getPacketData();    
  }
  
  public byte[] process(ReadRequest readRequest, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    rrqCount++;
    
    if (rrqModification == null) {
      return readRequest.getPacketData();
    }
    
    if (wrqModification.getPacketNumber() == wrqCount) {
      return rrqModification.apply(readRequest, localReceivePort, remoteReceivePort, delayedPacketConsumer);
    }
    return readRequest.getPacketData();
  }
  
  public byte[] process(WriteRequest writeRequest, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    wrqCount++;
    
    if (wrqModification == null) {
      return writeRequest.getPacketData();
    }
    
    if (wrqModification.getPacketNumber() == wrqCount) {
      return wrqModification.apply(writeRequest, localReceivePort, remoteReceivePort, delayedPacketConsumer);
    }
    return writeRequest.getPacketData();
  }
  
  public byte[] process(DataPacket dataPacket, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    dataCount++;
    
    if (dataModification == null) {
      return dataPacket.getPacketData();
    }
    
    if (dataModification.getPacketNumber() == dataCount) {
      return dataModification.apply(dataPacket, localReceivePort, remoteReceivePort, delayedPacketConsumer);
    }
    return dataPacket.getPacketData();
  }
  
  public byte[] process(Acknowledgement ackPacket, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    ackCount++;

    if (ackModification == null) {
      return ackPacket.getPacketData();
    }
    
    if (ackModification.getPacketNumber() == ackCount) {
      return ackModification.apply(ackPacket, localReceivePort, remoteReceivePort, delayedPacketConsumer);
    }
    return ackPacket.getPacketData();
  }

  public byte[] process(ErrorPacket errPacket, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer) {
    packetCount++;
    errCount++;

    if (errorModification == null) {
      return errPacket.getPacketData();
    }
    
    if (errorModification.getPacketNumber() == errCount) {
      return errorModification.apply(errPacket, localReceivePort, remoteReceivePort, delayedPacketConsumer);
    }
    return errPacket.getPacketData();
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

  public ErrorPacketModification getErrorModification() {
    return errorModification;
  }

  public void setErrorModification(ErrorPacketModification errorModification) {
    this.errorModification = errorModification;
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
