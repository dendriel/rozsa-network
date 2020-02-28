package com.rozsa.network.message.incoming;

import com.rozsa.network.Connection;

public class ConnectedMessage extends IncomingMessage {
    public ConnectedMessage(Connection connection, byte[] data, int dataLen) {
        super(IncomingMessageType.CONNECTED, connection, data, dataLen);
    }
}
