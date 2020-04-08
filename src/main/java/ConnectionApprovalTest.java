import com.rozsa.network.*;
import com.rozsa.network.message.*;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

public class ConnectionApprovalTest {
    public static void ConnectionApprovalTest(String[] args) throws SocketException, NotActiveException, UnknownHostException, InterruptedException {
        int serverPort = 9090;
        int clientPort = 8989;
//        isServer = true;

        int targetPort = isServer ? serverPort : clientPort;

        PeerConfig config = new PeerConfig(targetPort);
        config.setPingUpdatedEventEnabled(true);
        config.setPingInterval(1f);
        config.setMtu(50);
        config.setConnectionApprovalRequired(true);

        peer = new NetworkPeer(config);
        peer.addOnConnectionRequestEventListeners(ConnectionApprovalTest::onConnectionRequest);
        peer.addOnConnectedEventListener(ConnectionApprovalTest::onConnectedEvent);
        peer.addOnDisconnectedEventListener(ConnectionApprovalTest::onDisconnectedEvent);
        peer.addOnPingUpdatedEventListener(ConnectionApprovalTest::onPingUpdatedEvent);
        peer.initialize();

        if (!isServer) {
            String password = "Vitor Rozsa Password";
            OutgoingMessage msg = peer.createOutgoingMessage(password.length());
            msg.writeString(password);
            peer.connect("localhost", serverPort, msg);
        }

        loop();
    }

    static boolean isServer;
    static NetworkPeer peer;

    static boolean deny = true;

    static void onConnectionRequest(ConnectionRequestMessage msg) {
        System.out.println("Received a connection request! Data: " + new String(msg.getHailMessage().getData()));

        if (!deny) {
            peer.approve(msg.getConnection());
            deny = !deny;
        }
        else {
            peer.deny(msg.getConnection());
            deny = !deny;
        }
    }

    static void onPingUpdatedEvent(PingUpdatedMessage msg) {
        System.out.println("> Ping updated to " + msg.getPingMicros() + "us");
    }

    static void onConnectedEvent(ConnectedMessage msg) {
        System.out.println("> Connected to " + msg.getConnection());

        if (!isServer) {
            sendReliable(msg.getConnection());
        }

        expectedOrder = 0;
    }

    static void sendReliable(Connection conn) {
//        int count = 3072;
//        int count = 2048;
        int count = 96;
//        int count = 1;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Random random = new Random();

        for (int i = 0;  i < count; i++) {

            // 1000
//            String msg = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
//            msg = msg.concat(msg);

            byte[] msg = new byte[100];
            Arrays.fill(msg, (byte)i);

//            String msg = String.valueOf(i);
            OutgoingMessage outgoingMsg = peer.createOutgoingMessage(msg.length);
//            outgoingMsg.writeString(msg);
            outgoingMsg.writeBytes(msg);
//            outgoingMsg.writeString(String.format(" - extra text!"));
//            peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_ORDERED, random.nextInt(32));
            peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_SEQUENCED, 0);
        }

    }

    static void onDisconnectedEvent(DisconnectedMessage msg) {
        System.out.println("Disconnected from " + msg.getConnection() + " reason: " + msg.getReason());
    }

    static int messagesReceived = 0;
    static short expectedOrder;
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

            byte[] buf = new byte[msg.getLength()];
            System.arraycopy(msg.getData(), 0, buf, 0, buf.length);
//            String val = new String(buf[0]);
            Logger.info("Received message \"" + buf[0] + "\" - total: " + messagesReceived + " " + msg.getDeliveryMethod() + " " + msg.getChannel());

//            if (Integer.parseInt(val) != expectedOrder) {
//                System.out.printf("Received out of order message: %s expected: %d\n", val, expectedOrder);
//            }
            expectedOrder++;
            messagesReceived++;

//            short seqNumber = Short.parseShort(val);
//            if (seqNumber < expectedOrder) {
//                System.out.printf("Received out of sequence message: %d expected >= %d\n", seqNumber, expectedOrder);
//            }
//            expectedOrder = (short)((seqNumber + 1) % 1024);
//            messagesReceived++;

//            Logger.info("Received message \"" + val + "\" - total: " + messagesReceived + " " + msg.getDeliveryMethod() + " " + msg.getChannel());

            peer.recycle(msg);
        }
    }
}
