package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.InvalidRequestException;
import packet.PacketParser;
import packet.Request;
import rop.ROP;
import rop.Result;
import packet.ErrorPacket.ErrorCode;
import packet.Request.RequestType;
import utils.PacketPrinter;

import java.util.function.Function;



/*
 * TODO
 *   - Create a Logger class that we can use for writing to console
 *     - It should accept a flag in the constructor that indicates TEST mode or SILENT mode
 *     - Include the timestamp with each log (new Date().getTime())
 *      - e.g. 6583234546551 [REQUEST-HANDLER] ...
 */


/**
 * The RequestHandler class handles requests received by the Listener.
 * 
 * @author Loktin Wong
 * @author Philip Klostermann
 * @version 1.0.1
 * @since 25-01-2016
 */
class RequestHandler implements Runnable {
  private DatagramPacket requestPacket;
  
  /**
   * Default RequestHandler constructor instantiates requestPacket to
   * the packet passed down from the Listener class.
   * 
   * @param packet
   */
  public RequestHandler(DatagramPacket requestPacket) {
    this.requestPacket = requestPacket;
  }
  
  /**
   * Processes the received request and initiates file transfer.
   */
  public void run() {
    ClientConnection connection = null;
    try {
      connection = new ClientConnection(requestPacket);
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }
    
    log("Incoming request");
    PacketPrinter.print(requestPacket);
    
    TransferState transferState = new TransferStateBuilder()
        .connection(connection)
        .build();
    
    ROP<Request, TransferState, IrrecoverableError> rop = new ROP<>();
    
    Result<TransferState, IrrecoverableError> result = 
        parseRequest
        .andThen(rop.bind((request) -> {
          TransferState state = TransferStateBuilder.clone(transferState)
              .request(request)
              .build();
          
          if (request.type() == RequestType.READ) {
            // initiate ReadRequest
            log("Received ReadRequest, initiating file transfer.");
            TftpReadTransfer readTransfer = new TftpReadTransfer();
            readTransfer.start(state);
          } else if (request.type() == RequestType.WRITE) {
            // initiate WriteRequest
            log("Received WriteRequest, initiating file transfer.");
            TftpWriteTransfer writeTransfer = new TftpWriteTransfer();
            writeTransfer.start(state);
          } else {
            // should never really get here
            log("Could not identify request type, but it was parsed.");
            return Result.failure(new IrrecoverableError(
                ErrorCode.ILLEGAL_TFTP_OPERATION, "Invalid Request. Not a RRQ or WRQ."));
          }
          
          return Result.success(state);
        }))
        .apply(requestPacket);
    
    
    if (result.FAILURE) {
      sendError(transferState, result.failure);
    } else {
      log("Transfer has ended. Terminating connection thread.");
    }
  }
  
  private Function<DatagramPacket, Result<Request, IrrecoverableError>> parseRequest = (datagram) -> {
    PacketParser parser = new PacketParser();
    Request req = null;
    try {
      req = parser.parseRequest(datagram);
    } catch (InvalidRequestException e) {
      return Result.failure(new IrrecoverableError(ErrorCode.ILLEGAL_TFTP_OPERATION, e.getMessage()));
    }
    return Result.success(req);
  };
  
  private static void sendError(TransferState state, IrrecoverableError error) {
    log("Sending error to client " + error.errorCode + ": " + error.message);
    ErrorPacket err = new ErrorPacketBuilder()
        .setErrorCode(error.errorCode)
        .setMessage(error.message)
        .buildErrorPacket();

    try {
      state.connection.sendPacket(err);
    } catch (IOException e) {
      e.printStackTrace();
      log("Error occured while sending error packet. We're done!");
    }
  };
  
  private static void log(String msg) {
    String name = Thread.currentThread().getName();
    System.out.println("[RequestHandler] " + name + ": " + msg);
  }
}