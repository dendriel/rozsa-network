package com.rozsa.network.message.outgoing;

public class ConnectionResponseMessage extends OutgoingMessage {
    public ConnectionResponseMessage()  {
        super(MessageType.CONNECTION_RESPONSE);
    }
}
