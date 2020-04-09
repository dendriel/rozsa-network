import com.rozsa.network.*;
import com.rozsa.network.message.*;

import java.io.IOException;
import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        int serverPort = 9090;
        int clientPort = 8989;
        isServer = true;

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

        System.out.println("Press any key to terminate.");
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

        // set some bits
        BitSet bits1 = new BitSet(20);
        BitSet bits2 = new BitSet(20);
        for(int i = 0; i < 20; i++) {
            if((i % 2) == 0) bits1.set(i);
            if((i % 5) != 0) bits2.set(i);
        }

        System.out.println("Bits1: " + bits1);
        System.out.println("Bits2: " + bits2);

        OutgoingMessage outgoingMsg = peer.createOutgoingMessage(10);
        outgoingMsg.writeByte(Byte.MAX_VALUE);
        outgoingMsg.writeShort(Short.MAX_VALUE);
        outgoingMsg.writeInt(Integer.MAX_VALUE);
        outgoingMsg.writeLong(Long.MAX_VALUE);
        outgoingMsg.writeFloat(Float.MAX_VALUE);
        outgoingMsg.writeBoolean(true);
        outgoingMsg.writeBitSet(bits1);
        outgoingMsg.writeString("Vitor");
        outgoingMsg.writeByte(Byte.MIN_VALUE);
        outgoingMsg.writeShort(Short.MIN_VALUE);
        outgoingMsg.writeInt(Integer.MIN_VALUE);
        outgoingMsg.writeLong(Long.MIN_VALUE);
        outgoingMsg.writeFloat(Float.MIN_VALUE);
        outgoingMsg.writeBoolean(false);
        outgoingMsg.writeBitSet(bits2);
        outgoingMsg.writeString("Rozsa, Vitor");
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

            System.out.println(msg.readByte());
            System.out.println(msg.readShort());
            System.out.println(msg.readInt());
            System.out.println(msg.readLong());
            System.out.println(msg.readFloat());
            System.out.println(msg.readBoolean());
            System.out.println(msg.readBitSet());
            System.out.println(msg.readString());
            System.out.println(msg.readByte());
            System.out.println(msg.readShort());
            System.out.println(msg.readInt());
            System.out.println(msg.readLong());
            System.out.println(msg.readFloat());
            System.out.println(msg.readBoolean());
            System.out.println(msg.readBitSet());
            System.out.println(msg.readString());

            peer.recycle(msg);
        }
    }
}
