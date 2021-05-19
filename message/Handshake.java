package message;

import peer.PeerProcess;

import java.io.UnsupportedEncodingException;

public class Handshake implements MessageConstants
{
    private byte[] handshakeHeader = new byte[HEADERLength];
    private byte[] peerID = new byte[PEER_IDLength];
    private byte[] zeroBits = new byte[ZERO_BITSLength];
    private String messageHeader;
    private String messagePeerID;

    public Handshake(){}

    public Handshake(String handshakeHeader, String PeerId) {
        try {
            this.messageHeader = handshakeHeader;
            this.handshakeHeader = handshakeHeader.getBytes(CHARSET);
            if (this.handshakeHeader.length > HEADERLength)
                throw new Exception("message.Handshake header length is out of bounds");

            this.messagePeerID = PeerId;
            this.peerID = PeerId.getBytes(CHARSET);
            if (this.peerID.length > HEADERLength)
                throw new Exception("PeerID length is out of bounds");

            this.zeroBits = "0000000000".getBytes(CHARSET);
        } catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
        }
    }

    public void setHandshakeHeader(byte[] handShakeHeader) {
        try {
            this.messageHeader = (new String(handShakeHeader, CHARSET)).trim();
            this.handshakeHeader = this.messageHeader.getBytes();
        } catch (UnsupportedEncodingException encodingException) {
            PeerProcess.displayLog(encodingException.toString());
        }
    }

    public void setPeerID(byte[] peerID) {
        try {
            this.messagePeerID = (new String(peerID, CHARSET)).trim();
            this.peerID = this.messagePeerID.getBytes();
        } catch (UnsupportedEncodingException encodingException) {
            PeerProcess.displayLog(encodingException.toString());
        }
    }

    public byte[] getHandshakeHeader() {
        return handshakeHeader;
    }

    public byte[] getPeerID() {
        return peerID;
    }

    public byte[] getZeroBits() {
        return zeroBits;
    }

    public String getHeaderString() {
        return messageHeader;
    }

    public String getPeerIDString() {
        return messagePeerID;
    }

    public String toString() {
        return (String.format("message.Handshake : Peer Id - [%s], Header - [%s]", this.messagePeerID, this.messageHeader));
    }

    public static Handshake byteArrayToHandshake(byte[] receivedMessage) {
        Handshake handshake;
        byte[] msgHeader;
        byte[] msgPeerID;

        try {
            if (receivedMessage.length != HANDSHAKELength)
                throw new Exception("Length of byte array is not valid");

            handshake = new Handshake();
            msgHeader = new byte[HEADERLength];
            msgPeerID = new byte[PEER_IDLength];

            System.arraycopy(receivedMessage, 0, msgHeader, 0,
                    HEADERLength);
            System.arraycopy(receivedMessage, HEADERLength
                            + ZERO_BITSLength, msgPeerID, 0,
                    PEER_IDLength);

            handshake.setHandshakeHeader(msgHeader);
            handshake.setPeerID(msgPeerID);

        } catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
            handshake = null;
        }
        return handshake;
    }

    public static byte[] handshakeToByteArray(Handshake handshake) {
        byte[] sendMessage = new byte[HANDSHAKELength];

        try {
            if (handshake.getHandshakeHeader() == null)
                throw new Exception("message.Handshake header is not valid");

            if (handshake.getHandshakeHeader().length > HEADERLength || handshake.getHandshakeHeader().length == 0)
                throw new Exception("message.Handshake header is not valid");
            else
                System.arraycopy(handshake.getHandshakeHeader(), 0, sendMessage,
                        0, handshake.getHandshakeHeader().length);

            if (handshake.getZeroBits() == null)
                throw new Exception("message.Handshake zero bits is not valid");

            if (handshake.getZeroBits().length > ZERO_BITSLength
                    || handshake.getZeroBits().length == 0)
                throw new Exception("message.Handshake zero bits is not valid");
            else
                System.arraycopy(handshake.getZeroBits(), 0,
                        sendMessage, HEADERLength,
                        ZERO_BITSLength - 1);

            if (handshake.getPeerID() == null)
                throw new Exception("message.Handshake peerID is not valid");
            else if (handshake.getPeerID().length > PEER_IDLength
                    || handshake.getPeerID().length == 0)
                throw new Exception("message.Handshake peerID is not valid");
            else
                System.arraycopy(handshake.getPeerID(), 0, sendMessage,
                        HEADERLength + ZERO_BITSLength,
                        handshake.getPeerID().length);
        }
        catch (Exception exception) {
            PeerProcess.displayLog(exception.toString());
            sendMessage = null;
        }
        return sendMessage;
    }
}
