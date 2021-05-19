package payloads;

import java.io.Serializable;

public class Payload implements Serializable {

  private static final long serialVersionUID = -5424746124279275035L;
  private int messageLength = 0;

  public Payload() {
    super();
  }

  /**
   * @param messageLength
   */
  public Payload(int messageLength) {
    super();
    this.messageLength = messageLength;
  }

  /**
   * @return the messageLength
   */
  public int getMessageLength() {
    return messageLength;
  }

  /**
   * @param messageLength
   *            the messageLength to set
   */
  public void setMessageLength(int messageLength) {
    this.messageLength = messageLength;
  }
}
