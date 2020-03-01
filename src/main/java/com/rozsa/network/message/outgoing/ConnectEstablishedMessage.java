package com.rozsa.network.message.outgoing;

public class ConnectEstablishedMessage extends OutgoingMessage {
    public ConnectEstablishedMessage() {
        super(MessageType.CONNECT_ESTABLISHED);
    }
}
