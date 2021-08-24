package com.rozsa.network.message;

import com.rozsa.network.Connection;

public class ConnectionRequestMessage extends IncomingMessage {
    private final IncomingUserDataMessage hailMessage;

    public ConnectionRequestMessage(Connection connection, IncomingUserDataMessage hailMessage) {
        super(IncomingMessageType.CONNECTION_REQUEST, connection);
        this.hailMessage = hailMessage;
    }

    public IncomingUserDataMessage getHailMessage() {
        return hailMessage;
    }
}
