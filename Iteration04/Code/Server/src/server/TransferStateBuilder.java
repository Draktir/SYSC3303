package server;

import packet.Acknowledgement;
import packet.DataPacket;
import packet.Request;

import java.util.Iterator;

/**
 * Created by phil on 05/03/16.
 */
public class TransferStateBuilder {
  private Request request = null;
  private int blockNumber = 0;
  private byte[] blockData = null;
  private Iterator<byte[]> fileIterator = null;
  private DataPacket dataPacket = null;
  private Acknowledgement acknowledgement = null;
  private ClientConnection connection = null;

  public TransferStateBuilder() {}

  public TransferState build() {
    return new TransferState(request, blockNumber, blockData, fileIterator, dataPacket, acknowledgement, connection);
  }

  public TransferStateBuilder clone(TransferState state) {
    this.request = state.request;
    this.blockNumber = state.blockNumber;
    this.blockData = state.blockData;
    this.fileIterator = state.fileIterator;
    this.dataPacket = state.dataPacket;
    this.acknowledgement = state.acknowledgement;
    this.connection = state.connection;
    return this;
  }

  public TransferStateBuilder request(Request request) {
    this.request = request;
    return this;
  }

  public TransferStateBuilder blockNumber(int blockNumber) {
    this.blockNumber = blockNumber;
    return this;
  }

  public TransferStateBuilder blockData(byte[] blockData) {
    this.blockData = blockData;
    return this;
  }

  public TransferStateBuilder fileIterator(Iterator<byte[]> fileIterator) {
    this.fileIterator = fileIterator;
    return this;
  }

  public TransferStateBuilder dataPacket(DataPacket dataPacket) {
    this.dataPacket = dataPacket;
    return this;
  }

  public TransferStateBuilder acknowledgement(Acknowledgement acknowledgement) {
    this.acknowledgement = acknowledgement;
    return this;
  }

  public TransferStateBuilder connection(ClientConnection connection) {
    this.connection = connection;
    return this;
  }
}
