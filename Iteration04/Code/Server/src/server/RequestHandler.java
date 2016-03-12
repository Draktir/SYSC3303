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
import tftp_transfer.LocalOperations;
import tftp_transfer.NetworkOperations;
import tftp_transfer.TransferState;
import tftp_transfer.TransferStateBuilder;
import packet.ErrorPacket.ErrorCode;
import packet.Request.RequestType;
import utils.IrrecoverableError;
import utils.Logger;
import utils.PacketPrinter;

import java.util.function.Function;

/**
 * The RequestHandler class handles requests received by the Listener.
 * 
 * @author Loktin Wong
 * @author Philip Klostermann
 * @version 1.0.1
 * @since 25-01-2016
 */
class RequestHandler implements Runnable {
  private final Logger logger = new Logger("RequestHandler");
  private final DatagramPacket requestPacket;
  
  /**
   * Default RequestHandler constructor instantiates requestPacket to
   * the packet passed down from the Listener class.
   * 
   * @param requestPacket
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
    
    logger.logAlways("Incoming request");
    PacketPrinter.print(requestPacket);
    
    TransferState transferState = new TransferStateBuilder()
        .connection(connection)
        .build();
    
    ROP<Request, TransferState, IrrecoverableError> rop = new ROP<>();
    
    Result<TransferState, IrrecoverableError> result =
        LocalOperations.parseRequest
        .andThen(rop.bind((request) -> {
          TransferState state = TransferStateBuilder.clone(transferState)
              .request(request)
              .build();
          
          if (request.type() == RequestType.READ) {
            // initiate ReadRequest
            logger.logAlways("Received ReadRequest, initiating file transfer.");
            TftpReadTransfer.start(state);
          } else if (request.type() == RequestType.WRITE) {
            // initiate WriteRequest
            logger.logAlways("Received WriteRequest, initiating file transfer.");
            TftpWriteTransfer.start(state);
          } else {
            // should never really get here
            logger.logError("Could not identify request type, but it was parsed.");
            return Result.failure(new IrrecoverableError(
                ErrorCode.ILLEGAL_TFTP_OPERATION, "Invalid Request. Not a RRQ or WRQ."));
          }
          
          return Result.success(state);
        }))
        .apply(requestPacket);
    
    
    if (result.FAILURE) {
      NetworkOperations.sendError.accept(transferState, result.failure);
    } else {
      logger.logAlways("Transfer has ended. Terminating connection thread.");
    }
  }
}