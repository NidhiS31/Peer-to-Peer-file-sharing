package peer;

import payloads.BitFieldPayload;

import java.util.Date;

public class RemotePeerInfo implements Comparable<RemotePeerInfo>
{
    public String peerId;
    public String peerAddress;
    public String peerPort;
    public int isFirstPeer;
    public double dataRate = 0;
    public int isInterested = 1;
    public int isPreferredNeighbor = 0;
    public int isOptUnchokedNeighbor = 0;
    public int isChoked = 1;
    public BitFieldPayload bitFieldPayload;
    public int state = -1;
    public int peerIndex;
    public int isCompleted = 0;
    public int isHandShake = 0;
    public Date startTime;
    public Date finishTime;

    public RemotePeerInfo(String pId, String pAddress, String pPort, int pIndex)
    {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        bitFieldPayload = new BitFieldPayload();
        peerIndex = pIndex;
    }
    public RemotePeerInfo(String pId, String pAddress, String pPort, int pIsFirstPeer, int pIndex)
    {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        isFirstPeer = pIsFirstPeer;
        bitFieldPayload = new BitFieldPayload();
        peerIndex = pIndex;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public String getPeerPort() {
        return peerPort;
    }

    public int getIsFirstPeer() {
        return isFirstPeer;
    }

    public int compareTo(RemotePeerInfo remotePeerInfo) {
        return Double.compare(this.dataRate, remotePeerInfo.dataRate);
    }

}