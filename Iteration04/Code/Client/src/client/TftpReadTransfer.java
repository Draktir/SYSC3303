package client;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import configuration.Configuration;
import file_io.FileWriter;
import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.InvalidDataPacketException;
import packet.InvalidErrorPacketException;
import packet.PacketParser;
import packet.ErrorPacket.ErrorCode;
import rop.ROP;
import rop.Result;
import sun.font.CreatedFontTracker;
import tftp_transfer.FileOperations;
import tftp_transfer.NetworkOperations;
import tftp_transfer.TransferState;
import tftp_transfer.TransferStateBuilder;
import utils.IrrecoverableError;
import utils.Logger;
import utils.RecoverableError;
import utils.Recursive;

public class TftpReadTransfer {
  private static final Logger logger = new Logger("TFTP-READ");

  public static void start(TransferState transferState) {
    // create the file
    final Result<FileWriter, IrrecoverableError> fileResult = FileOperations.createFile.apply(transferState);

    if (fileResult.FAILURE) {
      NetworkOperations.sendError.accept(transferState, fileResult.failure);
      return;
    }

    final FileWriter fileWriter = fileResult.success;

    // file writer function
    final Function<TransferState, Result<TransferState, IrrecoverableError>> writeFileBlock =
        fileBlockWriter(fileWriter);

    // create an ROP helper (for error handling)
    final ROP<TransferState, TransferState, IrrecoverableError> rop = new ROP<>();


    // send the request
    Result<TransferState, IrrecoverableError> reqResult = NetworkOperations.sendRequest
        .andThen(rop.map((state) -> {
          return TransferStateBuilder.clone(state)
              .blockNumber(1)
              .build();
        }))
        .andThen(rop.bind(NetworkOperations.receiveValidDataPacket))
        .apply(transferState);

    if (reqResult.FAILURE) {
      logger.logError("Sending request to server failed: " + reqResult.failure.message);
      if (reqResult.failure.errorCode != null) {
        NetworkOperations.sendError.accept(transferState, reqResult.failure);
      }
      errorCleanup();
    }
  }

  private static Function<TransferState, Result<TransferState, IrrecoverableError>> fileBlockWriter (
      FileWriter fileWriter) {

    return (state) -> {
      try {
        fileWriter.writeBlock(state.blockData);
      } catch (IOException e) {
        e.printStackTrace();
        IrrecoverableError err = new IrrecoverableError(
            ErrorCode.NOT_DEFINED, "Internal Error while writing file. Please try again.");
        return Result.failure(err);
      }
      return Result.success(state);
    };
  }

  private static void errorCleanup(TransferState transferState) {
    File f = new File(transferState.request.getFilename());
    if (f.exists()) {
      f.delete();
    }
  }
}
