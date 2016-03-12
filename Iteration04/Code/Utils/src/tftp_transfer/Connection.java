package tftp_transfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

import packet.Packet;

// TODO: this could probably be a class since the implementors do almost the same thing

public interface Connection {
  void sendPacket(Packet packet) throws IOException;
  DatagramPacket receive(int timeout) throws SocketTimeoutException;
}
