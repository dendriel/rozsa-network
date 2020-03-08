import com.rozsa.network.Connection;
import com.rozsa.network.NetworkPeer;
import com.rozsa.network.PeerConfig;
import com.rozsa.network.DeliveryMethod;
import com.rozsa.network.message.ConnectedMessage;
import com.rozsa.network.message.DisconnectedMessage;
import com.rozsa.network.message.IncomingUserDataMessage;
import com.rozsa.network.message.PingUpdatedMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, NotActiveException, UnknownHostException, InterruptedException {
        int serverPort = 9090;
        int clientPort = 8989;
        isServer = true;

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
        System.out.println("> Ping updated to " + msg.getPingMicros() + "us");
    }

    static void onConnectedEvent(ConnectedMessage msg) {
        System.out.println("> Connected to " + msg.getConnection());

        if (!isServer) {
            sendReliable(msg.getConnection());
        }
    }

    static void sendReliable(Connection conn) {
        int count = 2048;

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0;  i < count; i++) {
            String myName = String.valueOf(i);
            OutgoingMessage outgoingMsg = new OutgoingMessage(myName.length());
            outgoingMsg.writeString(myName);
//            outgoingMsg.writeString(String.format(" - extra text!"));
            peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE);
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

//            System.out.println("Received message \"" + new String(msg.getData()) + "\" " + " from " + msg.getConnection());
            System.out.println("Received message \"" + new String(msg.getData()) + "\"");
//            peer.disconnect(msg.getConnection());
        }
    }
}
