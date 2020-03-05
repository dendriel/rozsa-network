package com.rozsa.network;

import com.rozsa.network.message.incoming.PingUpdatedMessage;
import com.rozsa.network.message.outgoing.PingMessage;
import com.rozsa.network.message.outgoing.PongMessage;

class ConnectionHeartbeat {
    private final Connection conn;
    private final PacketSender sender;
    private final IncomingMessagesQueue incomingMessages;
    private final long pingInterval;
    private final long connectionTimeout;
    private final boolean isPingUpdatedEventEnabled;

    private long lastReceivedPingTime;
    private long lastSentPingTime;

    private short lastPingSentSeqNumber;
    private short currSeqNumber;

    private long sRtt;

    ConnectionHeartbeat(Connection conn, PacketSender sender, IncomingMessagesQueue incomingMessages, PeerConfig config) {
        this.conn = conn;
        this.sender = sender;
        this.incomingMessages = incomingMessages;

        this.pingInterval = Clock.secondsToNanos(config.getPingInterval());
        this.connectionTimeout = Clock.secondsToNanos(config.getConnectionTimeout());
        isPingUpdatedEventEnabled = config.isPingUpdatedEventEnabled();
        currSeqNumber = 0;
    }

    void reset() {
        lastReceivedPingTime = Clock.getCurrentTimeInNanos();
        lastSentPingTime = Clock.getCurrentTimeInNanos();
    }

    void update() {
        if (isTimeout()) {
            conn.handleTimeout();
            return;
        }

        if (!isTimeToSendPing()) {
            return;
        }

        sendPing();
    }

    public long getSRtt() {
        return Clock.nanosToMillis(sRtt);
    }

    private void sendPing() {
        lastSentPingTime = Clock.getCurrentTimeInNanos();
        System.out.println("SendPing: " + Clock.getCurrentTimeInNanos());
        lastPingSentSeqNumber = currSeqNumber++;
        PingMessage ping = new PingMessage(lastPingSentSeqNumber);
        sender.send(conn.getAddress(), ping.getData(), ping.getDataLength());
    }

    void pingReceived(short seqNumber) {
        lastReceivedPingTime = Clock.getCurrentTimeInNanos();
        sendPong(seqNumber);
    }

    private void sendPong(short seqNumber) {
        PongMessage pong = new PongMessage(seqNumber);
        sender.send(conn.getAddress(), pong.getData(), pong.getDataLength());
    }

    void pongReceived(short seqNumber) {
        if (lastPingSentSeqNumber != seqNumber) {
            // pong must be an outlier.
            Logger.warn("Unexpected pong sequence number %d. Expected %d", seqNumber, lastPingSentSeqNumber);
            return;
        }
        System.out.println("pongReceived: " + Clock.getCurrentTimeInNanos());
        long lastRtt = Clock.getTimePassedSinceInNanos(lastSentPingTime);
        sRtt = (long)(sRtt * 0.875 + lastRtt * 0.125);
//        Logger.info("New SRTT %dus - lastRtt %dus", Clock.nanosToMicros(sRtt), Clock.nanosToMicros(lastRtt));
        if (isPingUpdatedEventEnabled) {
            PingUpdatedMessage pingUpdatedMessage = new PingUpdatedMessage(conn, Clock.nanosToMicros(sRtt));
            incomingMessages.enqueue(pingUpdatedMessage);
        }
    }

    private boolean isTimeout() {
        long timeSinceLastPingReceived = Clock.getTimePassedSinceInNanos(lastReceivedPingTime);
        return timeSinceLastPingReceived > connectionTimeout;
    }

    private boolean isTimeToSendPing() {
        long timeSinceLastPingSent = Clock.getTimePassedSinceInNanos(lastSentPingTime);
        return timeSinceLastPingSent > pingInterval;
    }
}
