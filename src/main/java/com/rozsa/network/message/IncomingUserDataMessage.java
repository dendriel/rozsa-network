package com.rozsa.network.message;

import com.rozsa.network.Connection;

public class IncomingUserDataMessage extends IncomingMessage {
    public IncomingUserDataMessage(Connection connection, short seqNumber, byte[] data, int length) {
        super(IncomingMessageType.USER_DATA, connection, seqNumber, data, length);
    }
}
