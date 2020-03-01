package com.rozsa.network.message.outgoing;

public class ConnectionClosedMessage extends OutgoingMessage {
    public ConnectionClosedMessage() {
        super(MessageType.CONNECTION_CLOSED);
    }
}
