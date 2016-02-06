package packet;

import java.util.Arrays;

public class DataPacketBuilder extends PacketBuilder<DataPacketBuilder> {
  private int blockNumber = 0;
  private byte[] fileData = new byte[0];
  
  public DataPacket buildDataPacket() {
    return new DataPacket(remoteHost, remotePort, getBlockNumber(), fileData);
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public DataPacketBuilder setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
    return this;
  }

  public byte[] getFileData() {
    return fileData;
  }

  public DataPacketBuilder setFileData(byte[] fileData) {
    this.fileData = fileData;
    return this;
  }

  @Override
  public String toString() {
    return "DataPacketBuilder [blockNumber=" + getBlockNumber() + ", fileData=" + Arrays.toString(fileData) + ", remoteHost="
        + remoteHost + ", remotePort=" + remotePort + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
