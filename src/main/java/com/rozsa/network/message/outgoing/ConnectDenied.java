package com.rozsa.network.message.outgoing;

public class ConnectDenied extends OutgoingMessage {
    public ConnectDenied() {
        super(MessageType.CONNECTION_DENIED);
    }
}
