package payloads;

public class BitField extends Payload {

  private byte[] bitfield;

  public BitField(byte[] bitfield) {
    super();
    this.bitfield = bitfield;
  }

  public byte[] getBitField() {
    return bitfield;
  }
}
