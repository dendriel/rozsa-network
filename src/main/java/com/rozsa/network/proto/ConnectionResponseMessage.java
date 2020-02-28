package com.rozsa.network.proto;

import com.rozsa.network.message.outgoing.OutgoingMessage;

public class ConnectionResponseMessage extends OutgoingMessage {
    public ConnectionResponseMessage()  {
        super(MessageType.CONNECTION_RESPONSE);
    }
}
