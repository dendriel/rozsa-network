package com.rozsa.network.message.incoming;

import com.rozsa.network.Connection;

public class ConnectedMessage extends IncomingMessage {
    public ConnectedMessage(Connection connection) {
        super(IncomingMessageType.CONNECTED, connection);
    }
}
