package com.rozsa.network;

import com.rozsa.network.message.outgoing.PingMessage;

class ConnectionHeartbeat {
    private Connection conn;
    private final PacketSender sender;
    private final long pingInterval;
    private final long connectionTimeout;

    private long lastReceivedPingTime;
    private long lastSentPingTime;

    ConnectionHeartbeat(PeerConfig config, Connection conn, PacketSender sender) {
        this.conn = conn;
        this.sender = sender;

        this.pingInterval = Clock.secondsToMillis(config.getPingInterval());
        this.connectionTimeout = Clock.secondsToMillis(config.getConnectionTimeout());
    }

    void reset() {
        lastReceivedPingTime = Clock.getCurrentTime();
        lastSentPingTime = Clock.getCurrentTime();
    }

    void update() {
        if (isTimeout()) {
            conn.handleTimeout();
            return;
        }

        if (!isTimeToSendPing()) {
            return;
        }

        lastSentPingTime = Clock.getCurrentTime();
        sendPing();
    }

    private void sendPing() {
        PingMessage ping = new PingMessage();
        sender.send(conn.getAddress(), ping.serialize(), ping.getDataLength());
    }

    void pingReceived() {
        lastReceivedPingTime = Clock.getCurrentTime();
    }

    private boolean isTimeout() {
        long timeSinceLastPingReceived = Clock.getTimePassedSince(lastReceivedPingTime);
        return timeSinceLastPingReceived > connectionTimeout;
    }

    private boolean isTimeToSendPing() {
        long timeSinceLastPingSent = Clock.getTimePassedSince(lastSentPingTime);
        return timeSinceLastPingSent > pingInterval;
    }
}
