package tftp_transfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

import packet.Packet;
import packet.Request;

/**
 * Interface implemented by Connection classes in Server and Client
 * 
 */

public interface Connection {
  void sendRequest(Request request) throws IOException;
  void sendPacket(Packet packet) throws IOException;
  DatagramPacket receive(int timeout) throws SocketTimeoutException;
}
