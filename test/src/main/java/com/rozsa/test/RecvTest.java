package com.rozsa.test;

import com.rozsa.network.*;
import com.rozsa.network.message.*;

import java.io.IOException;

public class RecvTest {
    public static void recvTest(String[] args) throws IOException, InterruptedException {
        int serverPort = 9090;
        int clientPort = 8989;
        isServer = Boolean.getBoolean("server");

        int targetPort = isServer ? serverPort : clientPort;

        PeerConfig config = new PeerConfig(targetPort);
        config.setPingUpdatedEventEnabled(true);
        config.setPingInterval(1f);
        config.setMaximumHandshakeAttempts(10);
        config.setIntervalBetweenHandshakes(1000);

        peer = new NetworkPeer(config);
        peer.addOnConnectedEventListener(RecvTest::onConnectedEvent);
        peer.addOnDisconnectedEventListener(RecvTest::onDisconnectedEvent);
        peer.addOnPingUpdatedEventListener(ConnectionApprovalTest::onPingUpdatedEvent);
        peer.initialize();

        if (!isServer) {
            peer.connect("localhost", serverPort);
        }

        loop();

        System.out.println("Press any key to terminate.");
    }
    static void onConnectedEvent(ConnectedMessage msg) {
        System.out.println("> Connected to " + msg.getConnection());
    }

    static void onDisconnectedEvent(DisconnectedMessage msg) {
        System.out.println("Disconnected from " + msg.getConnection() + " reason: " + msg.getReason());
    }

    static boolean isServer;
    static NetworkPeer peer;

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

            peer.recycle(msg);
        }
    }
}
