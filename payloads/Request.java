package payloads;

public class Request extends Payload {

  private int index;

  public Request(int index) {
    super();
    this.index = index;
  }

  public int getIndex() {
    return index;
  }
}
