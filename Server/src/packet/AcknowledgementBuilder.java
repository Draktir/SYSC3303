package packet;

import java.util.Arrays;

public class AcknowledgementBuilder extends PacketBuilder {
  private int blockNumber = 0;
  
  public Acknowledgement buildAcknowledgement() {
    return new Acknowledgement(remoteHost, remotePort, blockNumber);
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public AcknowledgementBuilder setBlockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
    return this;
  }

  @Override
  public String toString() {
    return "AcknowledgementBuilder [blockNumber=" + blockNumber + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
