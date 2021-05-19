package peer;

import java.util.Comparator;

public class PeerDataRateComparator implements Comparator<RemotePeerInfo> {

    private final boolean comparator;

    public PeerDataRateComparator(boolean comp) {
        this.comparator = comp;
    }

    public int compare(RemotePeerInfo remotePeer1, RemotePeerInfo remotePeer2) {
        if (remotePeer1 == null && remotePeer2 == null)
            return 0;
        if (remotePeer1 == null)
            return 1;
        if (remotePeer2 == null)
            return -1;

        if (comparator) {
            return remotePeer1.compareTo(remotePeer2);
        } else {
            return remotePeer2.compareTo(remotePeer1);
        }
    }

}