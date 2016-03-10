package server;

import java.util.Arrays;

import packet.Acknowledgement;
import packet.DataPacket;
import packet.Request;

public class TransferState {
  public final Request request;
  public final int blockNumber;
  public final byte[] blockData;
  public final DataPacket dataPacket;
  public final Acknowledgement acknowledgement;
  public final ClientConnection connection;

  public TransferState(Request request, int blockNumber, byte[] blockData,
                       DataPacket dataPacket, Acknowledgement acknowledgement, ClientConnection connection) {
    this.request = request;
    this.blockNumber = blockNumber;
    this.blockData = blockData;
    this.dataPacket = dataPacket;
    this.acknowledgement = acknowledgement;
    this.connection = connection;
  }

  @Override
  public String toString() {
    return "TransferState{" +
            "\n    request=" + request +
            ",\n    blockNumber=" + blockNumber +
            ",\n    blockData=" + Arrays.toString(blockData) +
            ",\n    dataPacket=" + dataPacket +
            ",\n    acknowledgement=" + acknowledgement +
            ",\n    connection=" + connection +
            "\n}";
  }
}
