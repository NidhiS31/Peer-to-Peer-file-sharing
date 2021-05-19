package message;

import com.CommonProperties;
import payloads.BitFieldPayload;
import payloads.PiecePayload;
import peer.PeerProcess;
import peer.RemotePeerInfo;
import util.ConversionUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;


public class MessageProcessor implements Runnable, MessageConstants
{
    private static String currentPeerID = null;
    RandomAccessFile randomAccessFile;

    public MessageProcessor(String currentPeerID) {
        MessageProcessor.currentPeerID = currentPeerID;
    }

    public void run() {
        Message message;
        MessageAttributes messageAttributes;
        String messageType;
        String runningPeerID;

        while(true)
        {
            messageAttributes  = PeerProcess.updateMessageQueueOnRemoval();
            while(messageAttributes == null) {
                Thread.currentThread();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                messageAttributes  = PeerProcess.updateMessageQueueOnRemoval();
            }

            message = messageAttributes.getMessage();

            messageType = message.getMessageTypeString();
            runningPeerID = messageAttributes.getSenderPeerID();
            int currentState = PeerProcess.remotePeerInfoTable.get(runningPeerID).state;
            if(messageType.equals(HAVEPayload) && currentState != 14)
            {
                // receive HAVE message
                PeerProcess.displayLog(String.format("[%s] received the 'have' message from Peer [%s]", PeerProcess.peerID, runningPeerID));
                if(compareBitfield(message, runningPeerID)) {
                    sendInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                  PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 9;
                }
                else {
                    sendNotInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                  PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 13;
                }
            }
            else {
                switch (currentState)
                {
                    case 2:
                        if (messageType.equals(BITFIELDPayload)) {
                            PeerProcess.displayLog(String.format("[%s] received the 'bitfield' message from Peer [%s]", PeerProcess.peerID, runningPeerID));
                            sendBitFieldPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 3;
                        }
                        break;

                    case 3:
                        if (messageType.equals(NOT_INTERESTEDPayload)) {
                            //receive NOT INTERESTED message
                            PeerProcess
                                .displayLog(String.format("[%s] received the 'not interested' message from Peer [%s]", PeerProcess.peerID, runningPeerID));
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).isInterested = 0;
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 5;
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).isHandShake = 1;
                        }
                        else if (messageType.equals(INTERESTEDPayload)) {
                            // receive INTERESTED message
                            PeerProcess.displayLog(String.format("[%s] received the 'interested' message from Peer [%s]", PeerProcess.peerID, runningPeerID));
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).isInterested = 1;
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).isHandShake = 1;

                            if(!PeerProcess.preferredNeighboursTable.containsKey(runningPeerID) && !PeerProcess.unchokedNeighboursTable.containsKey(runningPeerID)) {
                                sendChokePayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).isChoked = 1;
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state  = 6;
                            }
                            else {
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).isChoked = 0;
                                sendUnChokePayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 4 ;
                            }
                        }
                        break;

                    case 4:
                        if (messageType.equals(REQUESTPayload)) {
                            transferPiece(PeerProcess.peerIDTable.get(runningPeerID), message, runningPeerID);
                            // CHOKE/UNCHOKE
                            if(!PeerProcess.preferredNeighboursTable.containsKey(runningPeerID) && !PeerProcess.unchokedNeighboursTable.containsKey(runningPeerID)) {
                                sendChokePayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).isChoked = 1;
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 6;
                            }
                        }
                        break;

                    case 8:
                        if (messageType.equals(BITFIELDPayload)) {
                            //INTERESTED/ NOT INTERESTED
                            if(compareBitfield(message,runningPeerID)) {
                                sendInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 9;
                            }
                            else {
                                sendNotInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 13;
                            }
                        }
                        break;

                    case 9:
                        if (messageType.equals(CHOKEPayload)) {
                            // receive CHOKED message
                            PeerProcess.displayLog(String.format("[%s] is CHOKED by Peer [%s]", PeerProcess.peerID, runningPeerID));
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 14;
                        }
                        else if (messageType.equals(UNCHOKEPayload)) {
                            // receive UNCHOKED message
                            PeerProcess.displayLog(String.format("[%s] is UNCHOKED by Peer [%s]", PeerProcess.peerID, runningPeerID));
                            int initialMismatch = PeerProcess.currentBitFieldPayload.findFirstRequirement(
                                PeerProcess.remotePeerInfoTable.get(runningPeerID).bitFieldPayload);
                            if(initialMismatch != -1) {
                                sendRequest(PeerProcess.peerIDTable.get(runningPeerID), initialMismatch);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 11;
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).startTime = new Date();
                            }
                            else
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 13;
                        }
                        break;

                    case 11:
                        if (messageType.equals(PIECEPayload)) {
                            byte[] payloadArray = message.getPayloadArray();
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).finishTime = new Date();
                            long timeLapse = PeerProcess.remotePeerInfoTable.get(runningPeerID).finishTime.getTime() -
                                             PeerProcess.remotePeerInfoTable.get(runningPeerID).startTime.getTime() ;
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).dataRate = ((double)(payloadArray.length + MESSAGELength + MESSAGEType) / (double)timeLapse) * 100;
                            PiecePayload p = PiecePayload.pieceArrayToPiece(payloadArray);
                            PeerProcess.currentBitFieldPayload.updatePieceAndBitfield(runningPeerID, p);
                            int fetchPieceIndex = PeerProcess.currentBitFieldPayload.findFirstRequirement(
                                PeerProcess.remotePeerInfoTable.get(runningPeerID).bitFieldPayload);
                            if(fetchPieceIndex != -1) {
                                sendRequest(PeerProcess.peerIDTable.get(runningPeerID), fetchPieceIndex);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state  = 11;
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).startTime = new Date();
                            }
                            else
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 13;
                            PeerProcess.nextReadPeerInfo();

                            Enumeration<String> keys = PeerProcess.remotePeerInfoTable.keys();
                            while(keys.hasMoreElements())
                            {
                                String nextElement = keys.nextElement();
                                RemotePeerInfo pref = PeerProcess.remotePeerInfoTable.get(nextElement);
                                if(nextElement.equals(PeerProcess.peerID))continue;
                                if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShake == 1) {
                                    sendHavePayload(PeerProcess.peerIDTable.get(nextElement), nextElement);
                                  PeerProcess.remotePeerInfoTable.get(nextElement).state = 3;
                                }
                            }
                        }
                        else if (messageType.equals(CHOKEPayload)) {
                            PeerProcess.displayLog(String.format("[%s] is CHOKED by Peer [%s]", PeerProcess.peerID, runningPeerID));
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 14;
                        }
                        break;

                    case 14:
                        if (messageType.equals(HAVEPayload)) {
                            if(compareBitfield(message,runningPeerID)) {
                                sendInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 9;
                            }
                            else {
                                sendNotInterestedPayload(PeerProcess.peerIDTable.get(runningPeerID), runningPeerID);
                              PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 13;
                            }
                        }
                        else if (messageType.equals(UNCHOKEPayload)) {
                            PeerProcess.displayLog(String.format("[%s] is UNCHOKED by Peer [%s]", PeerProcess.peerID, runningPeerID));
                          PeerProcess.remotePeerInfoTable.get(runningPeerID).state = 14;
                        }
                        break;
                }
            }

        }
    }

    private void sendRequest(Socket serverSocket, int pieceNumber) {
        byte[] pieceArray = new byte[MessageConstants.PIECELength];
        for (int index = 0; index < MessageConstants.PIECELength; index++)
            pieceArray[index] = 0;

        byte[] pieceIndexArray = ConversionUtil.integerToByteConverter(pieceNumber);
        System.arraycopy(pieceIndexArray, 0, pieceArray, 0,
                pieceIndexArray.length);
        Message message = new Message(REQUESTPayload, pieceArray);
        byte[] messageArray = Message.messageToByteArray(message);
        sendMessage(serverSocket, messageArray);
    }

    private void transferPiece(Socket serverSocket, Message requestMessage, String remotePeerID)
    {
        byte[] bytePieceIndex = requestMessage.getPayloadArray();
        int pieceIndex = ConversionUtil.byteArrayToIntegerConverter(bytePieceIndex);
        byte[] readBytes = new byte[CommonProperties.sizeOfPiece];
        int numberOfBytesRead = 0;
        File currentFile = new File(PeerProcess.peerID, CommonProperties.fileToBeShared);

        PeerProcess.displayLog(String.format("[%s] is sending a PIECE message for piece [%s] to Peer [%s]",
                                             PeerProcess.peerID, pieceIndex, remotePeerID));
        try {
            randomAccessFile = new RandomAccessFile(currentFile,"r");
            randomAccessFile.seek((long) pieceIndex *CommonProperties.sizeOfPiece);
            numberOfBytesRead = randomAccessFile.read(readBytes, 0, CommonProperties.sizeOfPiece);
        }
        catch (IOException ioException) {
            PeerProcess.displayLog(String.format("[%s] error in reading the file: [%s]", PeerProcess.peerID, ioException.toString()));
        }
        if( numberOfBytesRead == 0)
            PeerProcess.displayLog(String.format("[%s] Zero bytes read from the file", PeerProcess.peerID));
        else if (numberOfBytesRead < 0)
            PeerProcess.displayLog(String.format("[%s] File could not be read properly.", PeerProcess.peerID));

        byte[] bytesBuffer = new byte[numberOfBytesRead + MessageConstants.PIECELength];
        System.arraycopy(bytePieceIndex, 0, bytesBuffer, 0, MessageConstants.PIECELength);
        System.arraycopy(readBytes, 0, bytesBuffer, MessageConstants.PIECELength, numberOfBytesRead);

        Message sendMessage = new Message(PIECEPayload, bytesBuffer);
        byte[] messageToByteArray =  Message.messageToByteArray(sendMessage);
        sendMessage(serverSocket, messageToByteArray);
        try{randomAccessFile.close();}
        catch(Exception ignored){}
    }

    private boolean compareBitfield(Message message, String remotePeerID) {
        BitFieldPayload bitFieldPayload = BitFieldPayload.decodeBitfield(message.getPayloadArray());
      PeerProcess.remotePeerInfoTable.get(remotePeerID).bitFieldPayload = bitFieldPayload;
        return PeerProcess.currentBitFieldPayload.compareBitfieldPayloads(bitFieldPayload);
    }

    private void sendNotInterestedPayload(Socket serverSocket, String remotePeerID) {
        PeerProcess.displayLog(String.format("[%s] is sending a 'not interested' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        Message message =  new Message(NOT_INTERESTEDPayload);
        byte[] messageToByteArray = Message.messageToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendInterestedPayload(Socket serverSocket, String remotePeerID) {
        PeerProcess.displayLog(String.format("[%s] is sending an 'interested' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        Message message =  new Message(INTERESTEDPayload);
        byte[] msgByte = Message.messageToByteArray(message);
        sendMessage(serverSocket,msgByte);
    }

    private void sendUnChokePayload(Socket serverSocket, String remotePeerID) {
        PeerProcess.displayLog(String.format("[%s] is sending 'unchoke' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        Message message = new Message(UNCHOKEPayload);
        byte[] messageToByteArray = Message.messageToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendChokePayload(Socket serverSocket, String remotePeerID) {
        PeerProcess
            .displayLog(String.format("[%s] is sending 'choke' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        Message message = new Message(CHOKEPayload);
        byte[] messageToByteArray = Message.messageToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendBitFieldPayload(Socket serverSocket, String remotePeerID) {
        PeerProcess.displayLog(String.format("[%s] is sending 'bitfield' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        byte[] encodedBitField = PeerProcess.currentBitFieldPayload.encodeBitField();
        Message message = new Message(BITFIELDPayload, encodedBitField);
        sendMessage(serverSocket, Message.messageToByteArray(message));
    }

    private void sendHavePayload(Socket serverSocket, String remotePeerID) {

        PeerProcess
            .displayLog(String.format("[%s] is sending 'have' message to Peer [%s]", PeerProcess.peerID, remotePeerID));
        byte[] encodedBitField = PeerProcess.currentBitFieldPayload.encodeBitField();
        Message message = new Message(HAVEPayload, encodedBitField);
        sendMessage(serverSocket, Message.messageToByteArray(message));
    }

    private void sendMessage(Socket serverSocket, byte[] encodedBitField) {
        try {
            OutputStream outputStream = serverSocket.getOutputStream();
            outputStream.write(encodedBitField);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}