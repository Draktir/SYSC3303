package intermediate_host;

import java.util.Deque;
import java.util.LinkedList;

public class RequestBuffer {
  Deque<ForwardMessage> clientRequests = new LinkedList<>();
  Deque<ForwardMessage> serverRequests = new LinkedList<>();
  
  
}
