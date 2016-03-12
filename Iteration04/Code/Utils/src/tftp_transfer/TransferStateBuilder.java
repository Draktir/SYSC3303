package tftp_transfer;

import packet.Acknowledgement;
import packet.DataPacket;
import packet.Request;

/**
 * Created by phil on 05/03/16.
 */
public class TransferStateBuilder {
  private Request request = null;
  private int blockNumber = 0;
  private byte[] blockData = null;
  private DataPacket dataPacket = null;
  private Acknowledgement acknowledgement = null;
  private Connection connection = null;

  public TransferStateBuilder() {}

  public TransferState build() {
    return new TransferState(request, blockNumber, blockData, dataPacket, acknowledgement, connection);
  }

  /**
   * Creates a new instance of the transfer state builder with values
   * from the given state already set.
   * 
   * @param state
   * @return
   */
  public static TransferStateBuilder clone(TransferState state) {
    TransferStateBuilder tsb = new TransferStateBuilder();
    tsb.request = state.request;
    tsb.blockNumber = state.blockNumber;
    tsb.blockData = state.blockData;
    tsb.dataPacket = state.dataPacket;
    tsb.acknowledgement = state.acknowledgement;
    tsb.connection = state.connection;
    return tsb;
  }
  
  
  /**
   * Applies the values from the given state to this builder,
   * but retains values that have already been set.
   * 
   * @param state
   * @return
   */
  public TransferStateBuilder apply(TransferState state) {
    this.request = this.request == null ? state.request : this.request;
    this.blockNumber = this.blockNumber == 0 ? state.blockNumber : this.blockNumber;
    this.blockData = this.blockData == null ? state.blockData : this.blockData;
    this.dataPacket = this.dataPacket == null ? state.dataPacket : this.dataPacket;
    this.acknowledgement = this.acknowledgement == null ? state.acknowledgement : this.acknowledgement;
    this.connection = this.connection == null ? state.connection : this.connection;
    return this;
  }

  /**
   * Setters with 'set' keyword omitted for readability 
   */
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

  public TransferStateBuilder dataPacket(DataPacket dataPacket) {
    this.dataPacket = dataPacket;
    return this;
  }

  public TransferStateBuilder acknowledgement(Acknowledgement acknowledgement) {
    this.acknowledgement = acknowledgement;
    return this;
  }

  public TransferStateBuilder connection(Connection connection) {
    this.connection = connection;
    return this;
  }

  
  /**
   * Getters
   */
  public Request getRequest() {
    return request;
  }

  public int getBlockNumber() {
    return blockNumber;
  }

  public byte[] getBlockData() {
    return blockData;
  }

  public DataPacket getDataPacket() {
    return dataPacket;
  }

  public Acknowledgement getAcknowledgement() {
    return acknowledgement;
  }

  public Connection getConnection() {
    return connection;
  }
}
