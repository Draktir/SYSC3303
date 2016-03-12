package tftp_transfer;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class TransferId {
  public final InetAddress address;
  public final int port;

  public TransferId(InetAddress address, int port) {
    this.address = address;
    this.port = port;
  }

  public TransferId(DatagramPacket datagramPacket) {
    this.address = datagramPacket.getAddress();
    this.port = datagramPacket.getPort();
  }

  public boolean equals(TransferId other) {
    return this.address.equals(other.address) &&
            this.port == other.port;
  }

  @Override
  public String toString() {
    return "TransferId{" +
            "\n    address=" + address +
            ",\n    port=" + port +
            "\n}";
  }
}
