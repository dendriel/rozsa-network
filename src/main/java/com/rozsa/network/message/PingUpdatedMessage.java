package com.rozsa.network.message;

import com.rozsa.network.Connection;

public final class PingUpdatedMessage extends IncomingMessage {
    private long ping;

    public PingUpdatedMessage(Connection connection, long ping) {
        super(IncomingMessageType.PING_UPDATED, connection);
        this.ping = ping;
    }

    public long getPing() {
        return ping / 1000;
    }

    public long getPingMicros() {
        return ping;
    }
}
