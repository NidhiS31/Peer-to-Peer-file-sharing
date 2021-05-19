package payloads;

import com.CommonProperties;
import message.MessageConstants;
import util.ConversionUtil;

public class PiecePayload
{
    public int containedPiece;
    public String senderPeerID;
    public byte[] pieceArray;
    public int pieceIndex;


    public int getContainedPiece() { return containedPiece;}

    public void setContainedPiece(int containedPiece) { this.containedPiece = containedPiece; }

    public void setSenderPeerID(String senderPeerID) { this.senderPeerID = senderPeerID;}

    public PiecePayload() {
        pieceArray = new byte[CommonProperties.sizeOfPiece];
        pieceIndex = -1;
        containedPiece = 0;
        senderPeerID = null;
    }

    public static PiecePayload pieceArrayToPiece(byte []payload) {
        byte[] byteIndex = new byte[MessageConstants.PIECELength];
        PiecePayload piecePayload = new PiecePayload();
        System.arraycopy(payload, 0, byteIndex, 0, MessageConstants.PIECELength);
        piecePayload.pieceIndex = ConversionUtil.byteArrayToIntegerConverter(byteIndex);
        piecePayload.pieceArray = new byte[payload.length-MessageConstants.PIECELength];
        System.arraycopy(payload, MessageConstants.PIECELength, piecePayload.pieceArray, 0, payload.length - MessageConstants.PIECELength);
        return piecePayload;
    }
}