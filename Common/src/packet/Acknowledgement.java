package packet;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Acknowledgement extends Packet {
  int blockNumber;

  public Acknowledgement(InetAddress remoteHost, int remotePort, InetAddress localHost, int localPort, int blockNumber) {
    super(remoteHost, remotePort, localHost, localPort);
    this.blockNumber = blockNumber;
  }

  @Override
  public byte[] getPacketData() {
    ByteBuffer requestBuffer = ByteBuffer.allocate(4);
    byte[] blockNumberBytes = ByteBuffer.allocate(4).putInt(blockNumber).array();
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 4);
    requestBuffer.put(blockNumberBytes[2]);
    requestBuffer.put(blockNumberBytes[3]);
    
    return requestBuffer.array();
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
        + ", localHost=" + localHost + ", localPort=" + localPort + "]";
  }
}
