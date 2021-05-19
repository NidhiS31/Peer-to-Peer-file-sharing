import peer.RemotePeerInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

/*
 * The StartRemotePeers class begins remote peer processes.
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 */
public class StartRemotePeers
{
    public Vector<RemotePeerInfo> peerInfoVector = new Vector<>();
    public Vector<Process> peerProcesses = new Vector<>();

    public void getConfiguration()
    {
        String st;
        try
        {
            BufferedReader in = new BufferedReader(new FileReader("com/PeerInfo.cfg"));
            int i =0;
            while((st = in.readLine()) != null)
            {
                String[] tokens = st.split("\\s+");
                peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2], i));
                i++;
            }
            in.close();
        }
        catch (Exception ex)
        {
            System.out.println("Exception:" +ex.toString());
        }
    }
    /**
     * Checks if all peer has down loaded the file
     */
    public static synchronized boolean isFinished() {

        String line;
        int hasFileCount = 1;

        try {
            BufferedReader in = new BufferedReader(new FileReader(
                "com/PeerInfo.cfg"));

            while ((line = in.readLine()) != null) {
                hasFileCount = hasFileCount
                        * Integer.parseInt(line.trim().split("\\s+")[3]);
            }
            in.close();
            return hasFileCount != 0;

        } catch (Exception e) {

            return false;
        }

    }
    /**
     * @param args - input args
     **/
    public static void main(String[] args) {
        try
        {
            StartRemotePeers myStart = new StartRemotePeers();
            myStart.getConfiguration();

            String path = System.getProperty("user.dir");

            for (int index = 0; index < myStart.peerInfoVector.size(); index++)
            {
                RemotePeerInfo pInfo = myStart.peerInfoVector.elementAt(index);

                System.out.println("Start remote peer " + pInfo.peerId +  " at " + pInfo.peerAddress );
                String command = "ssh " + pInfo.peerAddress + " cd " + path + "; java peer.PeerProcess " + pInfo.peerId;
                myStart.peerProcesses.add(Runtime.getRuntime().exec(command));
                System.out.println(command);
            }

            System.out.println("Waiting for remote peers to terminate.." );

            boolean isFinished;
            while(true)
            {

                isFinished = isFinished();
                if (isFinished)
                {
                    System.out.println("All peers are terminated!");
                    break;
                }
                else
                {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

        }
        catch (Exception ex)
        {
            System.out.println("Exception: "+ex.toString());
        }
    }
}