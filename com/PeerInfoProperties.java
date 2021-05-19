package com;

import util.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class PeerInfoProperties {

  private ArrayList<PeerInfo> peers = new ArrayList<>();

  public void read(Reader reader) throws IOException {
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(Constants.PEER_CONFIG_FILE));
      while (bufferedReader.readLine() != null) {
        String line = bufferedReader.readLine();
        String[] tokens = line.split(" ");
        peers.add(new PeerInfo(Integer.valueOf(tokens[0]), tokens[1], Integer.valueOf(tokens[2]),
                               Integer.valueOf(tokens[3])));
      }
      bufferedReader.close();
    } catch(Exception e) {
      System.out.println("Invalid peerInfo file name "+e);
    }
  }

  public ArrayList<PeerInfo> getPeers() {
    return peers;
  }
}
