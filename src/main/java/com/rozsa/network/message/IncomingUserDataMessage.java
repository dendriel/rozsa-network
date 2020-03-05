package com.rozsa.network.message;

import com.rozsa.network.Connection;

public final class IncomingUserDataMessage extends IncomingMessage {
    public IncomingUserDataMessage(Connection connection, byte[] data, int length) {
        super(IncomingMessageType.USER_DATA, connection, data, length);
    }
}
