package intermediate_host;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

import packet.Packet;

public class ForwardRequest {
  private DatagramPacket datagramPacket;
  private InetAddress localAddress;
  private int receivingPort;
  private byte[] data;


  public ForwardRequest(DatagramPacket datagramPacket, InetAddress localAddress, int receivingPort) {
    this.datagramPacket = datagramPacket;
    this.localAddress = localAddress;
    this.receivingPort = receivingPort;

    int len = datagramPacket.getLength();
    this.data = new byte[len];
    System.arraycopy(datagramPacket.getData(), 0, this.data, 0, len);
  }

  public ForwardRequest(byte[] data, InetAddress localAddress, int receivingPort) {
    this.datagramPacket = null;
    this.data = data;
    this.localAddress = localAddress;
    this.receivingPort = receivingPort;
  }

  public DatagramPacket getDatagramPacket() {
    return datagramPacket;
  }

  public void setDatagramPacket(DatagramPacket datagramPacket) {
    this.datagramPacket = datagramPacket;
  }

  public byte[] getData() {
    if (this.data == null) {
      return this.datagramPacket.getData();
    }
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public InetAddress getLocalAddress() {
    return localAddress;
  }

  public void setLocalAddress(InetAddress localAddress) {
    this.localAddress = localAddress;
  }

  public int getReceivingPort() {
    return receivingPort;
  }

  public void setReceivingPort(int receivingPort) {
    this.receivingPort = receivingPort;
  }

  @Override
  public String toString() {
    return "ForwardRequest [\n" +
        "\n    localAddress=" + localAddress +
        "\n    receivingPort=" + receivingPort +
        "\n    data=" + Arrays.toString(data) +
        ']';
  }
}
