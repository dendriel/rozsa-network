package com.rozsa.network.message.outgoing;

import com.rozsa.network.proto.MessageType;

public class UserDataMessage extends OutgoingMessage {
    public UserDataMessage(int size) {
        super(MessageType.USER_DATA, size);
    }
}
