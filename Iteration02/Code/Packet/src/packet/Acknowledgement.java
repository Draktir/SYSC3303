package packet;


import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Acknowledgement extends Packet {
  int blockNumber;

  public Acknowledgement(InetAddress remoteHost, int remotePort, int blockNumber) {
    super(remoteHost, remotePort);
    
    // if block number is bigger than what 2 bytes can represent
    if (blockNumber > Math.pow(2, 16)) {
      throw new RuntimeException("Block number is too big (max " 
            + Math.pow(2, 16) + "): " + blockNumber);
    }
    this.blockNumber = blockNumber;
  }

  @Override
  public byte[] getPacketData() {
    ByteBuffer requestBuffer = ByteBuffer.allocate(4);
 
    // convert block number from int to byte array
    BigInteger bigInt = BigInteger.valueOf(blockNumber);      
    byte[] blockNumberBytes = bigInt.toByteArray();
    if (blockNumberBytes.length == 1) {
      blockNumberBytes = new byte[]{0, blockNumberBytes[0]};
    }
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 4);
    requestBuffer.put(blockNumberBytes[0]);
    requestBuffer.put(blockNumberBytes[1]);
    
    return requestBuffer.array();
  }

  @Override
  public byte[] getOpcode() {
    return new byte[] {0, 4};
  }
  
  public int getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
  }

  @Override
  public String toString() {
    return "Acknowledgement [blockNumber=" + blockNumber + ", remoteHost=" + remoteHost + ", remotePort=" + remotePort
        + "]";
  }
}
