package com.rozsa.network.message;

import com.rozsa.network.Connection;

public final class ConnectedMessage extends IncomingMessage {
    public ConnectedMessage(Connection connection) {
        super(IncomingMessageType.CONNECTED, connection);
    }
}
