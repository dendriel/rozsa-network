package com.rozsa.network.proto;

import com.rozsa.network.OutgoingMessage;

public class ConnectionRequestMessage extends OutgoingMessage {
    public ConnectionRequestMessage() {
        super(MessageType.CONNECTION_REQUEST);
    }
}
