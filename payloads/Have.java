package payloads;

public class Have extends Payload {

  private int index;
  private static final long serialVersionUID = 3777628630171683471L;
  public Have(int index) {
    super();
    this.index = index;
  }

  public int getIndex() {
    return index;
  }
}
