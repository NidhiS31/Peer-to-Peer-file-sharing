package message;

public class MessageAttributes {
    public Message message;
    public String senderPeerID;

    public MessageAttributes() {
        message = new Message();
        senderPeerID = null;
    }

    public Message getMessage() {
        return message;
    }

    public String getSenderPeerID() {
        return senderPeerID;
    }
}