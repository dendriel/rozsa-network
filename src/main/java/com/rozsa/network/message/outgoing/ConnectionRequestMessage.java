package com.rozsa.network.message.outgoing;

public class ConnectionRequestMessage extends OutgoingMessage {
    public ConnectionRequestMessage() {
        super(MessageType.CONNECTION_REQUEST);
    }
}
