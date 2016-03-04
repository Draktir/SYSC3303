package intermediate_host;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Simple Unbounded Buffer class  for incoming requests.
 * Requests are stored in a queue (FIFO). Some operations
 * are non-blocking, since threads are meant to do other
 * tasks as well. takeRequestTimeout will wait for a
 * specified duration
 */

public class RequestBuffer {
  Deque<ForwardRequest> requests = new LinkedList<>();

  public synchronized boolean hasRequest() {
    return !requests.isEmpty();
  }

  public synchronized void putRequest(ForwardRequest request) {
    requests.addLast(request);
  }

  public synchronized ForwardRequest takeRequest() {
    return requests.isEmpty() ? null : requests.removeFirst();
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
        e.printStackTrace();
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
