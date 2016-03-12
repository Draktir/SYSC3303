package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import configuration.Configuration;
import file_io.FileReader;
import packet.Acknowledgement;
import packet.DataPacket;
import packet.DataPacketBuilder;
import packet.ErrorPacket;
import packet.ErrorPacket.ErrorCode;
import packet.ErrorPacketBuilder;
import packet.InvalidAcknowledgementException;
import packet.InvalidErrorPacketException;
import packet.PacketParser;
import rop.ROP;
import rop.Result;
import tftp_transfer.*;
import utils.IrrecoverableError;
import utils.Logger;
import utils.RecoverableError;
import utils.Recursive;

public class TftpReadTransfer {
  private static final Logger logger = new Logger("TFTP-READ");
  
  public static void start(TransferState transferState) {
    // open the requested file as a lazy Stream
    final Result<Stream<byte[]>, IrrecoverableError> streamResult =
        FileOperations.createFileReadStream.apply(transferState.request.getFilename());

    if (streamResult.FAILURE) {
      NetworkOperations.sendError.accept(transferState, streamResult.failure);
      return;
    }

    final Stream<byte[]> fileBlockStream = streamResult.success;

    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();

    // create a file reader function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> readFileBlock =
        fileReader(fileBlockStream.iterator());

    // send file, block by block
    do {
      Result<TransferState, IrrecoverableError> stepResult =
          readFileBlock
          .andThen(rop.map(LocalOperations.buildDataPacket))
          .andThen(rop.bind(NetworkOperations.sendDataPacket))
          .andThen(rop.bind(NetworkOperations.receiveValidAck))
          .apply(transferState);

      if (stepResult.SUCCESS) {
        transferState = stepResult.success;
      } else {
        logger.logError("Error encountered during file transfer: " + stepResult.failure.message);
        if (stepResult.failure.errorCode != null) {
          NetworkOperations.sendError.accept(transferState, stepResult.failure);
        }
        break;
      }
    } while(transferState.blockData.length == Configuration.BLOCK_SIZE);

    logger.logAlways("Transfer complete. Terminating thread");
    // closing the stream also closes the file
    fileBlockStream.close();
  }

  // returns a file reader function using the provided stream iterator
  private static Function<TransferState, Result<TransferState, IrrecoverableError>> fileReader(Iterator<byte[]> fileBlockIterator) {
    // reads one block at a time using the iterator
    return (state) -> {
      byte[] fileBlock = null;
      if (fileBlockIterator.hasNext()) {
        fileBlock = fileBlockIterator.next();
      } else {
        // in case we need to send a last data packet with no data
        fileBlock = new byte[0];
      }

      return Result.success(TransferStateBuilder.clone(state)
          .blockNumber(state.blockNumber + 1)
          .blockData(fileBlock)
          .build()
      );
    };
  }
}
