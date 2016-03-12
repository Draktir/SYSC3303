package utils;

import packet.ErrorPacket;

public class IrrecoverableError {
  public final ErrorPacket.ErrorCode errorCode;
  public final String message;

  public IrrecoverableError(String message) {
    errorCode = null;
    this.message = message;
  }

  public IrrecoverableError(ErrorPacket.ErrorCode errorCode, String message) {
    this.errorCode = errorCode;
    this.message = message;
  }
}