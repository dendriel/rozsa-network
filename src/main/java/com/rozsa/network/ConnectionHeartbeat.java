package com.rozsa.network;

import com.rozsa.network.message.outgoing.PingMessage;
import com.rozsa.network.message.outgoing.PongMessage;

class ConnectionHeartbeat {
    private Connection conn;
    private final PacketSender sender;
    private final long pingInterval;
    private final long connectionTimeout;

    private long lastReceivedPingTime;
    private long lastSentPingTime;

    private short lastPingSentSeqNumber;
    private short currSeqNumber;

    private long sRtt;

    ConnectionHeartbeat(PeerConfig config, Connection conn, PacketSender sender) {
        this.conn = conn;
        this.sender = sender;

        this.pingInterval = Clock.secondsToNanos(config.getPingInterval());
        this.connectionTimeout = Clock.secondsToNanos(config.getConnectionTimeout());
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

        long lastRtt = Clock.getTimePassedSinceInNanos(lastSentPingTime);
        sRtt = (long)(0.875 * sRtt + lastRtt * 0.125);
//        Logger.info("New SRTT %dus - lastRtt %dus", Clock.nanosToMicros(sRtt), Clock.nanosToMicros(lastRtt));
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
