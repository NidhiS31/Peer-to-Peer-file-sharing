package payloads;

public class Piece extends Payload {

  private int index;
  private byte[] content;

  public Piece(byte[] content, int index) {
    super();
    this.index = index;
    this.content = content;
  }

  public int getIndex() {
    return index;
  }

  public byte[] getContent() {
    return content;
  }

}
