package com.rozsa.network.proto;

import com.rozsa.network.message.outgoing.OutgoingMessage;

public class ConnectionRequestMessage extends OutgoingMessage {
    public ConnectionRequestMessage() {
        super(MessageType.CONNECTION_REQUEST);
    }
}
