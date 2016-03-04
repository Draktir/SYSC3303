package modification;

public class DuplicatePacketModification {
  private int duplications = 1;
  private int delay = 0;

  public DuplicatePacketModification(int delay, int duplications) {
    this.delay = delay >= 0 ? delay : 0;
    this.duplications = duplications > 0 ? duplications : 1;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }
  
  public int getDuplications() {
    return duplications;
  }

  public void setDuplications(int duplications) {
    this.duplications = duplications;
  }

  @Override
  public String toString() {
    return "DuplicatePacketModification [\n    duplications=" + duplications + ",\n    delay=" + delay + "\n]";
  }
}
