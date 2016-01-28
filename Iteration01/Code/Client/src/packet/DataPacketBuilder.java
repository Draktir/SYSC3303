package packet;

import java.util.Arrays;

public class DataPacketBuilder extends PacketBuilder {
  private int blockNumber = 0;
  private byte[] fileData = new byte[0];
  
  public DataPacket buildDataPacket() {
    return new DataPacket(remoteHost, remotePort, blockNumber, fileData);
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
  }

  public byte[] getFileData() {
    return fileData;
  }

  public void setFileData(byte[] fileData) {
    this.fileData = fileData;
  }

  @Override
  public String toString() {
    return "DataPacketBuilder [blockNumber=" + blockNumber + ", fileData=" + Arrays.toString(fileData) + ", remoteHost="
        + remoteHost + ", remotePort=" + remotePort + ", localHost=" + localHost + ", localPort=" + localPort
        + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
