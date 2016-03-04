package modification;

public class DelayPacketModification {
  private int delay = 0;

  public DelayPacketModification(int delay) {
    this.delay = delay >= 0 ? delay : 0;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  @Override
  public String toString() {
    return "DelayPacketModification [delay=" + delay + "]";
  }
}
