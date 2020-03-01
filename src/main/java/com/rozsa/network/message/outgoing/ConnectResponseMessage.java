package com.rozsa.network.message.outgoing;

public class ConnectResponseMessage extends OutgoingMessage {
    public ConnectResponseMessage()  {
        super(MessageType.CONNECT_RESPONSE);
    }
}
