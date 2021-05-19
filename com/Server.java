package com;

import peer.PeerProcess;
import peer.RemotePeerHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable
{
    private final ServerSocket serverSocket;
    private final String peerID;
    Socket remoteSocket;
    Thread clientThread;

    public Server(ServerSocket socket, String peerID) {
        this.serverSocket = socket;
        this.peerID = peerID;
    }

    public void run()
    {
        while(true)
        {
            try {
                remoteSocket = serverSocket.accept();
                clientThread = new Thread(new RemotePeerHandler(remoteSocket, 0, peerID));
                PeerProcess.displayLog(String.format("[%s] Connection is established", peerID));
                PeerProcess.senderThread.add(clientThread);
                clientThread.start();
            }
            catch(Exception exception) {
                PeerProcess
                    .displayLog(String.format("[%s] Exception in connection: [%s]", this.peerID, exception.toString()));
            }
        }
    }
}