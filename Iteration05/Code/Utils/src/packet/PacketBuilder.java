package packet;


import java.net.InetAddress;

public abstract class PacketBuilder<T> {
  protected InetAddress remoteHost = null;
  protected int remotePort = -1;
    
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
