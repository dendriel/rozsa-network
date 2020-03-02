package com.rozsa.network.message.outgoing;

public class ConnectRequestMessage extends OutgoingMessage {
    public ConnectRequestMessage() {
        super(MessageType.CONNECTION_REQUEST, (short)0);
    }
}
