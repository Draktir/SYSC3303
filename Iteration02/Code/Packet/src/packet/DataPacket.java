package packet;


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataPacket extends Packet {
  int blockNumber;
  byte[] fileData;

  public DataPacket(InetAddress remoteHost, int remotePort, int blockNumber, byte[] fileData) {
    super(remoteHost, remotePort);
    this.blockNumber = blockNumber;
    this.fileData = fileData;
  }

  @Override
  public byte[] getPacketData() {
    int len = 4 + fileData.length;
    ByteBuffer requestBuffer = ByteBuffer.allocate(len);
    byte[] blockNumberBytes = ByteBuffer.allocate(4).putInt(blockNumber).array();
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 3);
    requestBuffer.put(blockNumberBytes[2]);
    requestBuffer.put(blockNumberBytes[3]);
    requestBuffer.put(fileData);
    
    return requestBuffer.array();
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
    return "DataPacket [blockNumber=" + blockNumber + ", fileData=" + Arrays.toString(fileData) + ", remoteHost="
        + remoteHost + ", remotePort=" + remotePort + "]";
  }
}
