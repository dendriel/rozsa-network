package com.rozsa.network;

import com.rozsa.network.message.PingUpdatedMessage;

import static com.rozsa.network.DeliveryMethod.UNRELIABLE;

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
        // we need the ping asap so resend functionality may work correctly.
        lastSentPingTime = 0;
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

    long getResendDelay() {
        if (sRtt <= 0) {
            return Clock.secondsToNanos(0.1f); // 100 ms default srtt.
        }
        return (long)(Clock.secondsToNanos(0.025f) + (sRtt * 2.1)); // 25 ms + double rtt
    }

    long getSRtt() {
        return sRtt;
    }

    private void sendPing() {
        lastSentPingTime = Clock.getCurrentTimeInNanos();
        lastPingSentSeqNumber = currSeqNumber++;
        sender.sendProtocol(conn.getAddress(), MessageType.PING, lastPingSentSeqNumber);
    }

    void pingReceived(short seqNumber) {
        lastReceivedPingTime = Clock.getCurrentTimeInNanos();
        sender.sendProtocol(conn.getAddress(), MessageType.PONG, seqNumber);
    }

    void pongReceived(short seqNumber) {
        if (lastPingSentSeqNumber != seqNumber) {
            // pong must be an outlier.
            Logger.warn("Unexpected pong sequence number %d. Expected %d", seqNumber, lastPingSentSeqNumber);
            return;
        }

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
