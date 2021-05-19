package peer;

import message.Handshake;
import message.Message;
import message.MessageAttributes;
import message.MessageConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RemotePeerHandler implements Runnable, MessageConstants
{
    final int ACTIVEConnection = 1;
    private Socket peerSocket = null;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int connectionType;
    String currentPeerID;
    String remotePeerID;

    public RemotePeerHandler(Socket peerSocket, int connectionType, String ownPeerID) {
        this.peerSocket = peerSocket;
        this.connectionType = connectionType;
        this.currentPeerID = ownPeerID;
        try {
            inputStream = peerSocket.getInputStream();
            outputStream = peerSocket.getOutputStream();
        }
        catch (Exception exception) {
            PeerProcess.displayLog(String.format("[%s] Error : [%s]", this.currentPeerID, exception.getMessage()));
        }
    }

    public RemotePeerHandler(String host, int peerPort, int connectionType, String currPeerID) {
        try {
            this.connectionType = connectionType;
            this.currentPeerID = currPeerID;
            this.peerSocket = new Socket(host, peerPort);
        } catch (IOException ioException) {
            PeerProcess.displayLog(String.format("[%s] peer.RemotePeerHandler : [%s]", currPeerID, ioException.getMessage()));
        }
        this.connectionType = connectionType;

        try {
            inputStream = peerSocket.getInputStream();
            outputStream = peerSocket.getOutputStream();
        }
        catch (Exception exception) {
            PeerProcess.displayLog(String.format("[%s] peer.RemotePeerHandler : [%s]", currPeerID, exception.getMessage()));
        }
    }

    public boolean SendHandshake() {
        try {
            outputStream.write(Handshake.handshakeToByteArray(new Handshake(MessageConstants.HEADERName, this.currentPeerID)));
        }
        catch (IOException ioException) {
            PeerProcess
                .displayLog(String.format("[%s] SendHandshake : [%s]", this.currentPeerID, ioException.getMessage()));
            return true;
        }
        return false;
    }

    public void run() {
        MessageAttributes messageAttributes = new MessageAttributes();
        byte[] handshakeArray = new byte[32];
        byte[] messageBufferSansPayload = new byte[MESSAGELength + MESSAGEType];
        byte[] messageLengthArray;
        byte[] messageTypeArray;

        try {
            if(this.connectionType == ACTIVEConnection) {
                if(SendHandshake()) {
                    PeerProcess.displayLog(String.format("[%s] HANDSHAKE sending failed.", currentPeerID));
                    System.exit(0);
                }
                else
                    PeerProcess.displayLog(String.format("[%s] HANDSHAKE has been sent...", currentPeerID));

                updatePeerInfoTable(handshakeArray);

                Message message = new Message(BITFIELDPayload, PeerProcess.currentBitFieldPayload.encodeBitField());
                byte[] messageToByteArray = Message.messageToByteArray(message);
                outputStream.write(messageToByteArray);
              PeerProcess.remotePeerInfoTable.get(remotePeerID).state = 8;
            }
            else {
                updatePeerInfoTable(handshakeArray);
                if(SendHandshake()) {
                    PeerProcess.displayLog(String.format("[%s] HANDSHAKE message sending failed.", currentPeerID));
                    System.exit(0);
                }
                else
                    PeerProcess
                        .displayLog(String.format("[%s] HANDSHAKE message has been sent successfully.", currentPeerID));
              PeerProcess.remotePeerInfoTable.get(remotePeerID).state = 2;
            }
            while(true) {
                int headerBytes = inputStream.read(messageBufferSansPayload);
                if(headerBytes == -1)
                    break;
                messageLengthArray = new byte[MESSAGELength];
                messageTypeArray = new byte[MESSAGEType];
                System.arraycopy(messageBufferSansPayload, 0, messageLengthArray, 0, MESSAGELength);
                System.arraycopy(messageBufferSansPayload, MESSAGELength, messageTypeArray, 0, MESSAGEType);
                Message message = new Message();
                message.setMessageLength(messageLengthArray);
                message.setMessageType(messageTypeArray);
                if(message.getMessageTypeString().equals(MessageConstants.CHOKEPayload)
                        || message.getMessageTypeString().equals(MessageConstants.UNCHOKEPayload)
                        || message.getMessageTypeString().equals(MessageConstants.INTERESTEDPayload)
                        || message.getMessageTypeString().equals(MessageConstants.NOT_INTERESTEDPayload)){
                    messageAttributes.message = message;
                }
                else {
                    int bytesAlreadyRead = 0;
                    int bytesRead;
                    byte[] messagePayloadBuffer = new byte[message.getMessageLengthInt()-1];
                    while(bytesAlreadyRead < message.getMessageLengthInt()-1){
                        bytesRead = inputStream.read(messagePayloadBuffer, bytesAlreadyRead, message.getMessageLengthInt()-1-bytesAlreadyRead);
                        if(bytesRead == -1)
                            return;
                        bytesAlreadyRead += bytesRead;
                    }

                    byte []dataBuffWithPayload = new byte [message.getMessageLengthInt()+ MESSAGELength];
                    System.arraycopy(messageBufferSansPayload, 0, dataBuffWithPayload, 0, MESSAGELength + MESSAGEType);
                    System.arraycopy(messagePayloadBuffer, 0, dataBuffWithPayload, MESSAGELength + MESSAGEType, messagePayloadBuffer.length);

                    messageAttributes.message = Message.byteArrayToMessage(dataBuffWithPayload);
                }
                messageAttributes.senderPeerID = this.remotePeerID;
                PeerProcess.updateMessageQueue(messageAttributes);
            }
        }
        catch(IOException ioException){
            PeerProcess.displayLog(currentPeerID + " run exception: " + ioException);
        }

    }

    private void updatePeerInfoTable(byte[] handshakeArray) throws IOException {
        while(true) {
            inputStream.read(handshakeArray);
            Handshake handshake = Handshake.byteArrayToHandshake(handshakeArray);
            if(handshake.getHeaderString().equals(MessageConstants.HEADERName)) {
                remotePeerID = handshake.getPeerIDString();
                PeerProcess
                    .displayLog(String.format("[%s] makes a connection to Peer [%s]", currentPeerID, remotePeerID));
                PeerProcess.displayLog(String.format("[%s] Received a HANDSHAKE message from Peer [%s]", currentPeerID, remotePeerID));
                PeerProcess.peerIDTable.put(remotePeerID, this.peerSocket);
                break;
            }
        }
    }

}