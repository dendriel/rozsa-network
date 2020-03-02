package com.rozsa.network.message.outgoing;

public class ConnectEstablishedMessage extends OutgoingMessage {
    public ConnectEstablishedMessage() {
        super(MessageType.CONNECTION_ESTABLISHED, (short)0);
    }
}
