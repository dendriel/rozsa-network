import com.rozsa.network.Connection;
import com.rozsa.network.Logger;
import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.incoming.IncomingMessage;
import com.rozsa.network.Peer;
import com.rozsa.network.PeerConfig;
import com.rozsa.network.message.incoming.IncomingMessageType;
import com.rozsa.network.message.outgoing.UserDataMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

// Testing class.
class NetworkPeer {
    private int port;
    private Peer peer;

    NetworkPeer(int port) throws SocketException {
        this.port = port;
        peer = new Peer(new PeerConfig(port));
        peer.initialize();
    }

    public void start() {
        new Thread(this::loop).start();
        Logger.info("Peer started at port " + port);
    }

    public void connect(int targetPort) throws NotActiveException, UnknownHostException {
        peer.connect("localhost", targetPort);
        sendMsg = true;
        isClient = true;
        timeSinceDataMsg = System.currentTimeMillis();
    }

    private void loop() {
        while(true) {
            try {
                recvIncomingMessages();
            } catch (InterruptedException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    boolean isClient;
    boolean sendMsg;
    Connection peerConn = null;
    long timeSinceDataMsg = 0;
    boolean disconnected = false;

    private void recvIncomingMessages() throws InterruptedException {

        if (isClient && !disconnected && (System.currentTimeMillis() - timeSinceDataMsg) > 3000) {
            System.out.printf("Disconnect peer\n");
            disconnected = true;
            peer.disconnect(peerConn);
        }

        IncomingMessage incomingMsg = peer.read();
        if (incomingMsg == null) {
            Thread.sleep(1);
            return;
        }

//        Logger.info("INCOMING " + incomingMsg);

        if (incomingMsg.getType() == IncomingMessageType.CONNECTED) {
            peerConn = incomingMsg.getConnection();
        }
        else if (incomingMsg.getType() == IncomingMessageType.DISCONNECTED) {
            DisconnectedMessage disc = (DisconnectedMessage)incomingMsg;
            System.out.printf("Disconnected from %s. Reason: %s\n", incomingMsg.getConnection(), disc.getReason());
            return;
        }

        if (sendMsg) {
            sendMsg = false;
            timeSinceDataMsg = System.currentTimeMillis();
            UserDataMessage outgoingMsg = new UserDataMessage(incomingMsg.getDataLen());
            peer.sendMessage(peerConn, outgoingMsg, DeliveryMethod.UNRELIABLE);
        }
    }
}
