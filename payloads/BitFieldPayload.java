package payloads;

import com.CommonProperties;
import message.MessageConstants;
import peer.PeerProcess;

import java.io.*;

public class BitFieldPayload implements MessageConstants
{
    public PiecePayload[] piecePayloadArray;
    public int bitfieldSize;

    public BitFieldPayload()
    {
        bitfieldSize = (int) Math.ceil(((double) CommonProperties.sizeOfFile / (double) CommonProperties.sizeOfPiece));
        this.piecePayloadArray = new PiecePayload[bitfieldSize];
        for (int i = 0; i < this.bitfieldSize; i++)
            this.piecePayloadArray[i] = new PiecePayload();
    }

    public int getBitfieldSize() {return bitfieldSize;}


    public PiecePayload[] getPieceArray() {return piecePayloadArray;}

    public byte[] encodeBitField(){return this.getBytes();}

    public static BitFieldPayload decodeBitfield(byte[] b) {
        BitFieldPayload returnPayload = new BitFieldPayload();
        for(int pieceIndex = 0 ; pieceIndex < b.length; pieceIndex ++) {
            int pieceCount = 7;
            while(pieceCount >= 0) {
                int pieceNumber = 1 << pieceCount;
                if(pieceIndex * 8 + (8-pieceCount-1) < returnPayload.bitfieldSize) {
                    if((b[pieceIndex] & (pieceNumber)) != 0)
                        returnPayload.piecePayloadArray[pieceIndex * 8 + (8 - pieceCount - 1)].containedPiece = 1;
                    else
                        returnPayload.piecePayloadArray[pieceIndex * 8 + (8 - pieceCount - 1)].containedPiece = 0;
                }
                pieceCount--;
            }
        }
        return returnPayload;
    }

    //Method to compare whether two bitfields are equal or not
    public synchronized boolean compareBitfieldPayloads(BitFieldPayload cmpBitfield) {
        int cmpSize = cmpBitfield.getBitfieldSize();
        for (int index = 0; index < cmpSize; index++) {
            if (cmpBitfield.getPieceArray()[index].getContainedPiece() == 1
                    && this.getPieceArray()[index].getContainedPiece() == 0) {
                return true;
            }
        }
        return false;
    }

    // Method to fetch the number of the first bitfield that the given peer does not have
    public synchronized int findFirstRequirement(BitFieldPayload cmpBitfield) {
        if (this.getBitfieldSize() >= cmpBitfield.getBitfieldSize()) {
            for (int index = 0; index < cmpBitfield.getBitfieldSize(); index++) {
                if (cmpBitfield.getPieceArray()[index].getContainedPiece() == 1
                        && this.getPieceArray()[index].getContainedPiece() == 0) {
                    return index;
                }
            }
        } else {
            for (int index = 0; index < this.getBitfieldSize(); index++) {
                if (cmpBitfield.getPieceArray()[index].getContainedPiece() == 1
                        && this.getPieceArray()[index].getContainedPiece() == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    public byte[] getBytes()
    {
        int size = this.bitfieldSize / 8;
        if (bitfieldSize % 8 != 0){size += 1;}

        byte[] byteArray = new byte[size];
        int tempVar = 0;
        int byteIndex = 0;
        int index;
        for (index = 1; index <= this.bitfieldSize; index++) {
            int tempP = this.piecePayloadArray[index-1].containedPiece;
            tempVar = tempVar << 1;
            if (tempP == 1){tempVar = tempVar + 1;}
            if (index % 8 == 0) {
                byteArray[byteIndex] = (byte) tempVar;
                byteIndex++;
                tempVar = 0;
            }
        }
        if ((index - 1) % 8 != 0) {
            int shiftBit = ((bitfieldSize) - (bitfieldSize / 8) * 8);
            tempVar = tempVar << (8 - shiftBit);
            byteArray[byteIndex] = (byte) tempVar;
        }
        return byteArray;
    }

    public int piecesContained() {
        int pieceCount = 0;
        for (int index = 0; index < this.bitfieldSize; index++)
            if (this.piecePayloadArray[index].containedPiece == 1)
                pieceCount++;
        return pieceCount;
    }

    public boolean allPiecesObtained() {
        for (int index = 0; index < this.bitfieldSize; index++)
            if (this.piecePayloadArray[index].containedPiece == 0)
                return false;
        return true;
    }

    public void initializeBitfield(String currPeerID, int hasFileFlag) {

        if (hasFileFlag != 1) {
            // If current peer does not have the file
            for (int index = 0; index < this.bitfieldSize; index++) {
                this.piecePayloadArray[index].setContainedPiece(0);
                this.piecePayloadArray[index].setSenderPeerID(currPeerID);
            }
        } else {
            // If current peer has the file
            for (int i = 0; i < this.bitfieldSize; i++) {
                this.piecePayloadArray[i].setContainedPiece(1);
                this.piecePayloadArray[i].setSenderPeerID(currPeerID);
            }
        }
    }

    // Method to update piece and bitfield payloads
    public synchronized void updatePieceAndBitfield(String senderPeer, PiecePayload piecePayload) {
        try {
            if (PeerProcess.currentBitFieldPayload.piecePayloadArray[piecePayload.pieceIndex].containedPiece == 1) {
                PeerProcess.displayLog(senderPeer + " This piece is already present");
            }
            else {
                byte[] writeArray;
                int pieceOffset = piecePayload.pieceIndex * CommonProperties.sizeOfPiece;

                File fileToShare = new File(PeerProcess.peerID, CommonProperties.fileToBeShared);
                RandomAccessFile randomAccessFile = new RandomAccessFile(fileToShare, "rw");

                writeArray = piecePayload.pieceArray;
                randomAccessFile.seek(pieceOffset);
                randomAccessFile.write(writeArray);

                this.piecePayloadArray[piecePayload.pieceIndex].setContainedPiece(1);
                this.piecePayloadArray[piecePayload.pieceIndex].setSenderPeerID(senderPeer);
                randomAccessFile.close();

                PeerProcess.displayLog(String.format("[%s] has downloaded the piece [%s] from Peer [%s]. " +
                                                     "Now the number of pieces  it has is [%s]",
                                                     PeerProcess.peerID, piecePayload.pieceIndex, senderPeer,
                                                     PeerProcess.currentBitFieldPayload.piecesContained()));

                if (PeerProcess.currentBitFieldPayload.allPiecesObtained()) {
                  PeerProcess.remotePeerInfoTable.get(PeerProcess.peerID).isInterested = 0;
                  PeerProcess.remotePeerInfoTable.get(PeerProcess.peerID).isCompleted = 1;
                  PeerProcess.remotePeerInfoTable.get(PeerProcess.peerID).isChoked = 0;
                    updatePeerInfoConfigFile(PeerProcess.peerID, 1);

                    PeerProcess.displayLog(String.format("[%s] has DOWNLOADED the complete file.", PeerProcess.peerID));
                }
            }
        } catch (Exception e) {
            PeerProcess
                .displayLog(String.format("[%s] ERROR in updating bitfield: [%s]", PeerProcess.peerID, e.getMessage()));
        }

    }

    public void updatePeerInfoConfigFile(String clientID, int hasFile) {
        StringBuilder tempStringBuilder = new StringBuilder();
        BufferedWriter outputFileWriter;
        BufferedReader inputFileReader;
        String line;

        try {
            inputFileReader= new BufferedReader(new FileReader("com/PeerInfo.cfg"));

            while((line = inputFileReader.readLine()) != null) {
                if(line.trim().split("\\s+")[0].equals(clientID))
                    tempStringBuilder.append(line.trim().split("\\s+")[0]).append(" ").append(line.trim().split("\\s+")[1]).append(" ").append(line.trim().split("\\s+")[2]).append(" ").append(hasFile);
                else
                    tempStringBuilder.append(line);

                tempStringBuilder.append("\n");
            }

            inputFileReader.close();
            outputFileWriter= new BufferedWriter(new FileWriter("com/PeerInfo.cfg"));
            outputFileWriter.write(tempStringBuilder.toString());
            outputFileWriter.close();
        }
        catch (Exception e) {
            PeerProcess
                .displayLog(String.format("[%s] Error in updating the PeerInfo.cfg [%s]", clientID, e.getMessage()));
        }
    }

}