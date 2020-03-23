package com.rozsa.network.message;

import com.rozsa.network.Connection;
import com.rozsa.network.MessageType;

public class IncomingUserDataMessage extends IncomingMessage {
    public IncomingUserDataMessage(Connection connection, short seqNumber, byte[] data, int length, MessageType type, boolean isFrag) {
        super(IncomingMessageType.USER_DATA, connection, seqNumber, data, length, type, isFrag);
    }
}
