package intermediate_host;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Simple Unbounded Buffer class  for incoming requests.
 * Requests are stored in a queue (FIFO). Operations
 * are blocking or have a timeout.
 */

public class RequestBuffer {
  Deque<ForwardRequest> requests = new LinkedList<>();

  public synchronized boolean hasRequest() {
    return !requests.isEmpty();
  }

  public synchronized void putRequest(ForwardRequest request) {
    requests.addLast(request);
    notifyAll();
  }

  public synchronized ForwardRequest takeRequest() {
    while (requests.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        System.out.println("[RequestBuffer] takeRequest cancelled");
      }
    }
    return requests.removeFirst();
  }

  /*
   * Waits for a while if necessary, if still nothing after timeout
   * returns null.
   */
  public synchronized ForwardRequest takeRequest(long timeout) {
    while (requests.isEmpty()) {
      try {
        wait(timeout);
      } catch (InterruptedException e) {
        System.out.println("[RequestBuffer] takeRequest cancelled");
      }
      if (requests.isEmpty()) {
        return null;
      }
    }
    return requests.removeFirst();
  }

  @Override
  public String toString() {
    return "RequestBuffer{" +
        "requests=" + requests +
        '}';
  }
}
