package com.rozsa.network.message.outgoing;

public class PingMessage extends OutgoingMessage {
    public PingMessage(short seqNumber) {
        super(MessageType.PING, seqNumber);
    }
}
