package com.rozsa.network.message.outgoing;

public class PingMessage extends OutgoingMessage {
    public PingMessage() {
        super(MessageType.PING);
    }
}
