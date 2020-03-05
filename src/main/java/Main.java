import com.rozsa.network.NetworkPeer;
import com.rozsa.network.PeerConfig;
import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.incoming.IncomingUserDataMessage;
import com.rozsa.network.message.incoming.PingUpdatedMessage;
import com.rozsa.network.message.outgoing.OutgoingUserDataMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, NotActiveException, UnknownHostException, InterruptedException {
        int serverPort = 9090;
        int clientPort = 8989;
//        isServer = true;

        int targetPort = isServer ? serverPort : clientPort;

        PeerConfig config = new PeerConfig(targetPort);
        config.setPingUpdatedEventEnabled(true);

        peer = new NetworkPeer(config);
        peer.addOnConnectedEventListener(Main::onConnectedEvent);
        peer.addOnDisconnectedEventListener(Main::onDisconnectedEvent);
        peer.addOnPingUpdatedEventListener(Main::onPingUpdatedEvent);
        peer.initialize();

        if (!isServer) {
            peer.connect("localhost", serverPort);
        }

        loop();
    }

    static boolean isServer;
    static NetworkPeer peer;

    static void onPingUpdatedEvent(PingUpdatedMessage msg) {
        System.out.println("Ping updated to " + msg.getPingMicros());
    }

    static void onConnectedEvent(ConnectedMessage msg) {
        System.out.println("Connected to " + msg.getConnection());

        if (!isServer) {
            String myName = "Vitor Rozsa";
            OutgoingUserDataMessage outgoingMsg = new OutgoingUserDataMessage(myName.length());
            outgoingMsg.writeString(myName);
            peer.sendMessage(msg.getConnection(), outgoingMsg, DeliveryMethod.UNRELIABLE);
        }
    }

    static void onDisconnectedEvent(DisconnectedMessage msg) {
        System.out.println("Disconnected from " + msg.getConnection() + " reason: " + msg.getReason());
    }

    private static void loop() throws InterruptedException {
        while (true) {
            if (peer.getIncomingMessagesCount() == 0) {
                Thread.sleep(1);
                continue;
            }

            IncomingUserDataMessage msg = peer.read();
            if (msg == null) {
                // read an internal message.
                continue;
            }

            System.out.println("Received message \"" + new String(msg.getData()) + "\" " + " from " + msg.getConnection());
//            peer.disconnect(msg.getConnection());
        }
    }
}
