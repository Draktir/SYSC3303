package packet;


import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataPacket extends Packet {
  int blockNumber;
  byte[] fileData;

  public DataPacket(InetAddress remoteHost, int remotePort, int blockNumber, 
      byte[] fileData) {
    super(remoteHost, remotePort);
    
    // if block number is bigger than what 2 bytes can represent
    if (blockNumber > Math.pow(2, 16)) {
      throw new RuntimeException("Block number is too big (max " 
            + Math.pow(2,  16) + "): " + blockNumber);
    }
    
    this.blockNumber = blockNumber;
    this.fileData = fileData;
  }

  @Override
  public byte[] getPacketData() {
    int len = 4 + fileData.length;
    ByteBuffer requestBuffer = ByteBuffer.allocate(len);
    
    // convert block number from int to byte array
    BigInteger bigInt = BigInteger.valueOf(blockNumber);      
    byte[] blockNumberBytes = bigInt.toByteArray();
    if (blockNumberBytes.length == 1) {
      blockNumberBytes = new byte[]{0, blockNumberBytes[0]};
    }
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 3);
    requestBuffer.put(blockNumberBytes[0]);
    requestBuffer.put(blockNumberBytes[1]);
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
