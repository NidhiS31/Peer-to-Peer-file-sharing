package peer;

import com.CommonProperties;
import com.Server;
import logger.Logger;
import message.Message;
import message.MessageAttributes;
import message.MessageConstants;
import message.MessageProcessor;
import payloads.BitFieldPayload;
import util.DateUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
public class PeerProcess implements MessageConstants
{
    public static volatile Hashtable<String, RemotePeerInfo> preferredNeighboursTable = new Hashtable<>();
    public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighboursTable = new Hashtable<>();
    public static volatile Hashtable<String, RemotePeerInfo> remotePeerInfoTable = new Hashtable<>();
    public static Hashtable<String, Socket> peerIDTable = new Hashtable<>();
    public static volatile Queue<MessageAttributes> messageQueue = new LinkedList<>();
    public static BitFieldPayload currentBitFieldPayload = null;
    public ServerSocket clientSocket = null;
    public static volatile Timer timer;
    public static boolean finishedFlag = false;
    public static String peerID;
    public int currentPeerID;
    public int CLIENTPort;
    public static Thread messageProcessor;
    public Thread clientThread;
    public static Vector<Thread> receiverThread = new Vector<>();
    public static Vector<Thread> senderThread = new Vector<>();

    public static synchronized void updateMessageQueue(MessageAttributes msg)
    {
        messageQueue.add(msg);
    }

    public static synchronized MessageAttributes updateMessageQueueOnRemoval() {
        MessageAttributes message = null;
        if(!messageQueue.isEmpty())
            message = messageQueue.remove();
        return message;
    }

    public static void nextReadPeerInfo() {
        try {
            String peerInfoDetails;
            BufferedReader bufferedReader = new BufferedReader(new FileReader("com/PeerInfo.cfg"));
            while ((peerInfoDetails = bufferedReader.readLine()) != null) {
                String[]peerDetails = peerInfoDetails.trim().split("\\s+");
                String peerID = peerDetails[0];
                int finishedIndicator = Integer.parseInt(peerDetails[3]);
                if(finishedIndicator == 1) {
                    remotePeerInfoTable.get(peerID).isCompleted = 1;
                    remotePeerInfoTable.get(peerID).isInterested = 0;
                    remotePeerInfoTable.get(peerID).isChoked = 0;
                }
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            displayLog(peerID + exception.toString());
        }
    }

    public static class PreferredNeighbours extends TimerTask {
        public void run() {
            nextReadPeerInfo();
            Enumeration<String> remotePeerInfoKeys = remotePeerInfoTable.keys();
            int interestedPeers = 0;
            StringBuilder prefix = new StringBuilder();
            while(remotePeerInfoKeys.hasMoreElements()) {
                String currentRemotePeerInfo = remotePeerInfoKeys.nextElement();
                RemotePeerInfo preferredPeers = remotePeerInfoTable.get(currentRemotePeerInfo);
                if(currentRemotePeerInfo.equals(peerID)) continue;
                if (preferredPeers.isCompleted == 0 && preferredPeers.isHandShake == 1)
                    interestedPeers++;
                else if(preferredPeers.isCompleted == 1) {
                    try {
                        preferredNeighboursTable.remove(currentRemotePeerInfo);
                    }
                    catch (Exception ignored) { }
                }
            }
            if(interestedPeers > CommonProperties.preferredNeighbourNumber) {
                boolean preferredNeighboursFlag = preferredNeighboursTable.isEmpty();
                if(!preferredNeighboursFlag)
                    preferredNeighboursTable.clear();

                List<RemotePeerInfo> remotePeerInfosList = new ArrayList<>(remotePeerInfoTable.values());
                remotePeerInfosList.sort(new PeerDataRateComparator(false));
                int neighboursCount = 0;

                for (RemotePeerInfo remotePeerInfo : remotePeerInfosList) {
                    if (neighboursCount > CommonProperties.preferredNeighbourNumber - 1) break;

                    if (remotePeerInfo.isHandShake == 1 && !remotePeerInfo.peerId.equals(peerID)
                            && remotePeerInfoTable.get(remotePeerInfo.peerId).isCompleted == 0) {
                        remotePeerInfoTable.get(remotePeerInfo.peerId).isPreferredNeighbor = 1;
                        preferredNeighboursTable.put(remotePeerInfo.peerId, remotePeerInfoTable.get(remotePeerInfo.peerId));
                        neighboursCount++;
                        prefix.append(remotePeerInfo.peerId).append(", ");

                        if (remotePeerInfoTable.get(remotePeerInfo.peerId).isChoked == 1) {
                            sendUnChokePayload(PeerProcess.peerIDTable.get(remotePeerInfo.peerId), remotePeerInfo.peerId);
                            PeerProcess.remotePeerInfoTable.get(remotePeerInfo.peerId).isChoked = 0;
                            sendHavePayload(PeerProcess.peerIDTable.get(remotePeerInfo.peerId), remotePeerInfo.peerId);
                            PeerProcess.remotePeerInfoTable.get(remotePeerInfo.peerId).state = 3;
                        }
                    }
                }
            }
            else
            {
                remotePeerInfoKeys = remotePeerInfoTable.keys();
                while(remotePeerInfoKeys.hasMoreElements())
                {
                    String nextElement = remotePeerInfoKeys.nextElement();
                    RemotePeerInfo nextRemotePeerInfo = remotePeerInfoTable.get(nextElement);
                    if(nextElement.equals(peerID)) continue;

                    if (nextRemotePeerInfo.isCompleted == 0 && nextRemotePeerInfo.isHandShake == 1) {
                        if(!preferredNeighboursTable.containsKey(nextElement)) {
                            prefix.append(nextElement).append(", ");
                            preferredNeighboursTable.put(nextElement, remotePeerInfoTable.get(nextElement));
                            remotePeerInfoTable.get(nextElement).isPreferredNeighbor = 1;
                        }
                        if (nextRemotePeerInfo.isChoked == 1) {
                            sendUnChokePayload(PeerProcess.peerIDTable.get(nextElement), nextElement);
                            PeerProcess.remotePeerInfoTable.get(nextElement).isChoked = 0;
                            sendHavePayload(PeerProcess.peerIDTable.get(nextElement), nextElement);
                            PeerProcess.remotePeerInfoTable.get(nextElement).state = 3;
                        }
                    }
                }
            }
            if (!prefix.toString().equals(""))
                PeerProcess.displayLog(String.format("[%s] has selected the preferred neighbors [%s]", PeerProcess.peerID, prefix));
        }
    }

    private static void sendUnChokePayload(Socket serverSocket, String remotePeerID) {
        displayLog(String.format("[%s] is sending 'unchoke' message to Peer [%s]", peerID, remotePeerID));
        Message message = new Message(UNCHOKEPayload);
        byte[] messageToByteArray = Message.messageToByteArray(message);
        sendMessage(serverSocket, messageToByteArray);
    }

    private static void sendHavePayload(Socket socket, String remotePeerID) {
        byte[] encodedBitField = PeerProcess.currentBitFieldPayload.encodeBitField();
        displayLog(String.format("[%s] is sending 'have' message to Peer [%s]", peerID, remotePeerID));
        Message message = new Message(HAVEPayload, encodedBitField);
        sendMessage(socket, Message.messageToByteArray(message));
    }

    private static void sendMessage(Socket serverSocket, byte[] encodedBitField) {
        try {
            OutputStream outputStream = serverSocket.getOutputStream();
            outputStream.write(encodedBitField);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static class UnChokedNeighbors extends TimerTask {

        public void run() {
            nextReadPeerInfo();
            if(!unchokedNeighboursTable.isEmpty())
                unchokedNeighboursTable.clear();
            Enumeration<String> remotePeerInfosKeys = remotePeerInfoTable.keys();
            Vector<RemotePeerInfo> remotePeersList = new Vector<>();
            while(remotePeerInfosKeys.hasMoreElements()) {
                String key = remotePeerInfosKeys.nextElement();
                RemotePeerInfo pref = remotePeerInfoTable.get(key);
                if (pref.isChoked == 1
                        && !key.equals(peerID)
                        && pref.isCompleted == 0
                        && pref.isHandShake == 1)
                    remotePeersList.add(pref);
            }

            if (remotePeersList.size() > 0) {
                Collections.shuffle(remotePeersList);
                RemotePeerInfo firstRemotePeer = remotePeersList.firstElement();
                remotePeerInfoTable.get(firstRemotePeer.peerId).isOptUnchokedNeighbor = 1;
                unchokedNeighboursTable.put(firstRemotePeer.peerId, remotePeerInfoTable.get(firstRemotePeer.peerId));
                PeerProcess.displayLog(String.format("[%s] has the optimistically unchoked neighbor [%s]",
                                                     PeerProcess.peerID, firstRemotePeer.peerId));

                if (remotePeerInfoTable.get(firstRemotePeer.peerId).isChoked == 1) {
                    PeerProcess.remotePeerInfoTable.get(firstRemotePeer.peerId).isChoked = 0;
                    sendUnChokePayload(PeerProcess.peerIDTable.get(firstRemotePeer.peerId), firstRemotePeer.peerId);
                    sendHavePayload(PeerProcess.peerIDTable.get(firstRemotePeer.peerId), firstRemotePeer.peerId);
                    PeerProcess.remotePeerInfoTable.get(firstRemotePeer.peerId).state = 3;
                }
            }
        }
    }

    public static void initializeUnChokedNeighbors() {
        timer = new Timer();
        timer.schedule(new UnChokedNeighbors(),
                0,CommonProperties.optimumIntervalForUnchoking * 1000L);
    }

    public static void abortUnChokedNeighbors() {
        timer.cancel();
    }

    public static void initializePreferredNeighbors() {
        timer = new Timer();
        timer.schedule(new PreferredNeighbours(),
                0,CommonProperties.IntervalForUnchoking * 1000L);
    }

    public static void stopPreferredNeighbors() {
        timer.cancel();
    }

    public static void displayLog(String message)
    {
        Logger.writeLog(DateUtil.getDateAndTime() + ": Peer " + message);
        System.out.println(DateUtil.getDateAndTime() + ": Peer " + message);
    }

    public static void readCommonConfig() {
        String commonConfigs;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("com/Common.cfg"));
            while ((commonConfigs = bufferedReader.readLine()) != null) {
                String[] commonPropTokens = commonConfigs.split("\\s+");
                if (commonPropTokens[0].equalsIgnoreCase("NumberOfPreferredNeighbors"))
                    CommonProperties.preferredNeighbourNumber = Integer
                            .parseInt(commonPropTokens[1]);
                else if (commonPropTokens[0].equalsIgnoreCase("UnchokingInterval"))
                    CommonProperties.IntervalForUnchoking = Integer
                            .parseInt(commonPropTokens[1]);
                else if (commonPropTokens[0]
                        .equalsIgnoreCase("OptimisticUnchokingInterval"))
                    CommonProperties.optimumIntervalForUnchoking = Integer
                            .parseInt(commonPropTokens[1]);
                else if (commonPropTokens[0].equalsIgnoreCase("FileName"))
                    CommonProperties.fileToBeShared = commonPropTokens[1];
                else if (commonPropTokens[0].equalsIgnoreCase("FileSize"))
                    CommonProperties.sizeOfFile = Integer.parseInt(commonPropTokens[1]);
                else if (commonPropTokens[0].equalsIgnoreCase("PieceSize"))
                    CommonProperties.sizeOfPiece = Integer.parseInt(commonPropTokens[1]);
            }

            bufferedReader.close();
        } catch (Exception ex) {
            displayLog(peerID + ex.toString());
        }
    }

    public static void readPeerInfoConfig() {
        String peerInfos;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("com/PeerInfo.cfg"));
            int index = 0;
            while ((peerInfos = bufferedReader.readLine()) != null) {
                String[] tokens = peerInfos.split("\\s+");
                remotePeerInfoTable.put(tokens[0], new RemotePeerInfo(tokens[0],
                        tokens[1], tokens[2], Integer.parseInt(tokens[3]), index));
                index++;
            }
            bufferedReader.close();
        } catch (Exception ex) {
            displayLog(peerID + ex.toString());
        }
    }

    public static void createEmptyFile() {
        try {
            File dir = new File(peerID);
            dir.mkdir();

            File newFileName = new File(peerID, CommonProperties.fileToBeShared);
            OutputStream fileOutputStream = new FileOutputStream(newFileName, true);
            byte currentByte = 0;

            for (int index = 0; index < CommonProperties.sizeOfFile; index++)
                fileOutputStream.write(currentByte);
            fileOutputStream.close();
        }
        catch (Exception exception) {
            displayLog(String.format("[%s] error in creating the file [%s]", peerID, exception.getMessage()));
        }
    }

    private static void initializePreferredNeighbours() {
        Enumeration<String> remotePeerInfos = remotePeerInfoTable.keys();
        while(remotePeerInfos.hasMoreElements()) {
            String nextElement = remotePeerInfos.nextElement();
            if(!nextElement.equals(peerID))
                preferredNeighboursTable.put(nextElement, remotePeerInfoTable.get(nextElement));
        }
    }

    public static synchronized boolean isCompleted() {
        String peerInfoDetail;
        int hasFileCount = 1;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(
                "com/PeerInfo.cfg"));
            while ((peerInfoDetail = bufferedReader.readLine()) != null) {
                hasFileCount = hasFileCount
                        * Integer.parseInt(peerInfoDetail.trim().split("\\s+")[3]);
            }
            bufferedReader.close();
            return hasFileCount != 0;
        } catch (Exception e) {
            displayLog(e.toString());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args)
    {
        PeerProcess pProcess = new PeerProcess();
        peerID = args[0];
        try
        {
            boolean firstPeerFlag = false;
            Logger.initializer("log_peer_" + peerID +".log");
            displayLog(String.format("[%s] is started", peerID));
            readCommonConfig();
            readPeerInfoConfig();
            initializePreferredNeighbours();
            Enumeration<String> remotePeers = remotePeerInfoTable.keys();

            while(remotePeers.hasMoreElements()) {
                RemotePeerInfo remotePeerInfo = remotePeerInfoTable.get(remotePeers.nextElement());
                if(remotePeerInfo.peerId.equals(peerID)) {
                    pProcess.CLIENTPort = Integer.parseInt(remotePeerInfo.peerPort);
                    pProcess.currentPeerID = remotePeerInfo.peerIndex;
                    if(remotePeerInfo.getIsFirstPeer() == 1) {
                        firstPeerFlag = true;
                        break;
                    }
                }
            }

            currentBitFieldPayload = new BitFieldPayload();
            currentBitFieldPayload.initializeBitfield(peerID, firstPeerFlag?1:0);
            messageProcessor = new Thread(new MessageProcessor(peerID));
            messageProcessor.start();

            if(firstPeerFlag) {
                try {
                    pProcess.clientSocket = new ServerSocket(pProcess.CLIENTPort);
                    pProcess.clientThread = new Thread(new Server(pProcess.clientSocket, peerID));
                    pProcess.clientThread.start();
                }
                catch(SocketTimeoutException timeoutException) {
                    displayLog(String.format("[%s] gets time out exception: [%s]", peerID, timeoutException.toString()));
                    Logger.abort();
                    System.exit(0);
                }
                catch(IOException ioException) {
                    displayLog(String.format("[%s] gets exception in starting Client thread: [%s] [%s]",
                            peerID, pProcess.CLIENTPort, ioException.toString()));
                    Logger.abort();
                    System.exit(0);
                }
            }
            else {
                createEmptyFile();
                remotePeers = remotePeerInfoTable.keys();
                while(remotePeers.hasMoreElements()) {
                    RemotePeerInfo remotePeerInfo = remotePeerInfoTable.get(remotePeers.nextElement());
                    if(pProcess.currentPeerID > remotePeerInfo.peerIndex) {
                        Thread tempThread = new Thread(new RemotePeerHandler(
                                remotePeerInfo.getPeerAddress(), Integer
                                .parseInt(remotePeerInfo.getPeerPort()), 1,
                                peerID));
                        receiverThread.add(tempThread);
                        tempThread.start();
                    }
                }

                try {
                    pProcess.clientSocket = new ServerSocket(pProcess.CLIENTPort);
                    pProcess.clientThread = new Thread(new Server(pProcess.clientSocket, peerID));
                    pProcess.clientThread.start();
                }
                catch(SocketTimeoutException timeoutException) {
                    displayLog(String.format("[%s] gets time out exception in Starting the client thread: [%s]",
                            peerID, timeoutException.toString()));
                    Logger.abort();
                    System.exit(0);
                }
                catch(IOException ioException) {
                    displayLog(String.format("[%s] gets exception in Starting the listening thread: [%s] [%s]",
                            peerID, pProcess.CLIENTPort, ioException.toString()));
                    Logger.abort();
                    System.exit(0);
                }
            }

            initializePreferredNeighbors();
            initializeUnChokedNeighbors();

            while(true) {
                finishedFlag = isCompleted();
                if (finishedFlag) {
                    displayLog("All peers have completed downloading the file.");

                    stopPreferredNeighbors();
                    abortUnChokedNeighbors();

                    try {
                        Thread.currentThread();
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }

                    if (pProcess.clientThread.isAlive())
                        pProcess.clientThread.stop();

                    if (messageProcessor.isAlive())
                        messageProcessor.stop();

                    for (Thread thread : receiverThread)
                        if (thread.isAlive())
                            thread.stop();

                    for (Thread thread : senderThread)
                        if (thread.isAlive())
                            thread.stop();

                    break;
                } else {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        catch(Exception exception) {
            displayLog(String.format("[%s] Exception in ending : [%s]", peerID, exception.getMessage()));
        }
        finally {
            displayLog(String.format("[%s] Peer process is exiting", peerID));
            Logger.abort();
            System.exit(0);
        }
    }


}