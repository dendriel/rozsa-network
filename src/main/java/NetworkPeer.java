import com.rozsa.network.Connection;
import com.rozsa.network.Logger;
import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.incoming.IncomingMessage;
import com.rozsa.network.Peer;
import com.rozsa.network.PeerConfig;
import com.rozsa.network.message.incoming.IncomingMessageType;
import com.rozsa.network.message.outgoing.OutgoingUserDataMessage;

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
    }

    public void connect(int targetPort) throws NotActiveException, UnknownHostException {
        peer.connect("localhost", targetPort);
        sendMsg = true;
        isClient = true;
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

    private void recvIncomingMessages() throws InterruptedException {

        IncomingMessage incomingMsg = peer.read();
        if (incomingMsg == null) {
            Thread.sleep(1);
            return;
        }

        if (incomingMsg.getType() == IncomingMessageType.CONNECTED) {
            peerConn = incomingMsg.getConnection();
        }
        else if (incomingMsg.getType() == IncomingMessageType.DISCONNECTED) {
            DisconnectedMessage disc = (DisconnectedMessage)incomingMsg;
            System.out.printf("Disconnected from %s. Reason: %s\n", incomingMsg.getConnection(), disc.getReason());
            return;
        }
        else if (incomingMsg.getType() == IncomingMessageType.USER_DATA) {
            System.out.println("Received message \"" + new String(incomingMsg.getData()) + "\"");

        }

        if (sendMsg) {
            sendMsg = false;
            String myName = "Vitor Rozsa";
            OutgoingUserDataMessage outgoingMsg = new OutgoingUserDataMessage(myName.length());
            outgoingMsg.writeString(myName);
            peer.sendMessage(peerConn, outgoingMsg, DeliveryMethod.UNRELIABLE);
        }
    }
}
