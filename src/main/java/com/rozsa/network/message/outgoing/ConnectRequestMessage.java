package com.rozsa.network.message.outgoing;

public class ConnectRequestMessage extends OutgoingMessage {
    public ConnectRequestMessage() {
        super(MessageType.CONNECT_REQUEST);
    }
}
