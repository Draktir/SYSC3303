package packet;


import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class PacketBuilder<T> {
  protected InetAddress remoteHost = findLocalHost();
  protected int remotePort = 69;
  
  private InetAddress findLocalHost() {
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public InetAddress getRemoteHost() {
    return remoteHost;
  }
  
  @SuppressWarnings("unchecked")
  public T setRemoteHost(InetAddress remoteHost) {
    this.remoteHost = remoteHost;
    return (T) this;
  }
  
  public int getRemotePort() {
    return remotePort;
  }
  
  @SuppressWarnings("unchecked")
  public T setRemotePort(int remotePort) {
    this.remotePort = remotePort;
    return (T) this;
  }

  @Override
  public String toString() {
    return "PacketBuilder [remoteHost=" + remoteHost + ", remotePort=" + remotePort + "]";
  }
}
