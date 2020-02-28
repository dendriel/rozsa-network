import com.rozsa.network.Connection;
import com.rozsa.network.channel.DeliveryMethod;
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
        System.out.println("Peer started at port " + port);
    }

    public void connect(int targetPort) throws NotActiveException, UnknownHostException {
        peer.connect("localhost", targetPort);
        sendMsg = true;
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

    boolean sendMsg = false;

    Connection peerConn = null;

    private void recvIncomingMessages() throws InterruptedException {
        IncomingMessage incomingMsg = peer.read();
        if (incomingMsg == null) {
            Thread.sleep(1);
            return;
        }

        System.out.println("Received message " + incomingMsg);

        if (incomingMsg.getType() == IncomingMessageType.CONNECTED) {
            peerConn = incomingMsg.getConnection();
        }


        if (sendMsg) {
            sendMsg = false;
            UserDataMessage outgoingMsg = new UserDataMessage(incomingMsg.getDataLen());
            peer.sendMessage(peerConn, outgoingMsg, DeliveryMethod.UNRELIABLE);
        }
    }
}
