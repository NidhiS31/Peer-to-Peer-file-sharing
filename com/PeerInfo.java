package com;

public class PeerInfo {

  private int peerID;
  private String hostName;
  private int listeningPort;
  private int hasFile;

  public PeerInfo(int peerID, String hostName, int listeningPort, int hasFile) {
    this.peerID = peerID;
    this.hostName = hostName;
    this.listeningPort = listeningPort;
    this.hasFile = hasFile;
  }

  public int getPeerID() {
    return peerID;
  }

  public String getHostName() {
    return hostName;
  }

  public int getListeningPort() {
    return listeningPort;
  }

  public int getHasFile() {
    return hasFile;
  }
}
