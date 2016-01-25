package packet;

import java.util.Arrays;

public class AcknowledgementBuilder extends PacketBuilder {
  private int blockNumber = 0;
  
  public Acknowledgement buildAckowledgement() {
    return new Acknowledgement(remoteHost, remotePort, localHost, localPort, blockNumber);
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
  }

  @Override
  public String toString() {
    return "AcknowledgementBuilder [blockNumber=" + blockNumber + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + ", localHost=" + localHost + ", localPort=" + localPort + ", packetData="
        + Arrays.toString(packetData) + "]";
  }
}
