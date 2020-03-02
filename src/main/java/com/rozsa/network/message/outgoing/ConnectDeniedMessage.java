package com.rozsa.network.message.outgoing;

public class ConnectDeniedMessage extends OutgoingMessage {
    public ConnectDeniedMessage() {
        super(MessageType.CONNECTION_DENIED, (short)0);
    }
}
