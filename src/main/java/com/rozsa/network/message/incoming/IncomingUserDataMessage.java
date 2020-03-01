package com.rozsa.network.message.incoming;

import com.rozsa.network.Connection;

public class IncomingUserDataMessage extends IncomingMessage {
    public IncomingUserDataMessage(Connection connection, byte[] data, int length) {
        super(IncomingMessageType.USER_DATA, connection, data, length);
    }
}
