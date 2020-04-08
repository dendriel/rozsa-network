import com.rozsa.network.*;
import com.rozsa.network.message.*;

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
        config.setPingInterval(1f);

        peer = new NetworkPeer(config);
        peer.addOnConnectedEventListener(Main::onConnectedEvent);
        peer.addOnDisconnectedEventListener(Main::onDisconnectedEvent);
        peer.initialize();

        if (!isServer) {
            peer.connect("localhost", serverPort);
        }

        loop();
    }

    static boolean isServer;
    static NetworkPeer peer;

    static void onConnectedEvent(ConnectedMessage msg) {
        System.out.println("> Connected to " + msg.getConnection());

        if (!isServer) {
            sendReliable(msg.getConnection());
        }
    }

    static void sendReliable(Connection conn) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        OutgoingMessage outgoingMsg = peer.createOutgoingMessage(10);
        outgoingMsg.writeByte((byte)101);
        outgoingMsg.writeInt(900000);
        outgoingMsg.writeString("Vitor");
        outgoingMsg.writeByte((byte)102);
        outgoingMsg.writeInt(910001);
        outgoingMsg.writeString("Rozsa");
        peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_SEQUENCED, 0);
    }

    static void onDisconnectedEvent(DisconnectedMessage msg) {
        System.out.println("Disconnected from " + msg.getConnection() + " reason: " + msg.getReason());
    }

    private static boolean keepLooping = true;
    private static void loop() throws InterruptedException {
        while (keepLooping) {
            if (peer.getIncomingMessagesCount() == 0) {
                Thread.sleep(1);
                continue;
            }

            IncomingUserDataMessage msg = peer.read();
            if (msg == null) {
                // read an internal message.
                continue;
            }

//            byte[] firstPart = msg.readBytes();
//            byte[] secondPart = msg.readBytes();
//            System.out.println(new String(firstPart));
//            System.out.println(new String(secondPart));

            System.out.println(msg.readByte());
            System.out.println(msg.readInt());
            System.out.println(msg.readString());
            System.out.println(msg.readByte());
            System.out.println(msg.readInt());
            System.out.println(msg.readString());

            peer.recycle(msg);
        }
    }
}
