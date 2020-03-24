package com.rozsa.network.message;

import com.rozsa.network.Connection;

public class ConnectionRequestMessage extends IncomingMessage {
    private final IncomingMessage hailMessage;

    public ConnectionRequestMessage(Connection connection, IncomingMessage hailMessage) {
        super(IncomingMessageType.CONNECTION_REQUEST, connection);
        this.hailMessage = hailMessage;
    }

    public IncomingMessage getHailMessage() {
        return hailMessage;
    }
}
