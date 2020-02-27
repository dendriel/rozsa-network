package com.rozsa.network.proto;

import com.rozsa.network.OutgoingMessage;

public class ConnectionResponseMessage extends OutgoingMessage {
    public ConnectionResponseMessage()  {
        super(MessageType.CONNECTION_RESPONSE);
    }
}
