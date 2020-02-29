package com.rozsa.network.message.incoming;

import com.rozsa.network.Connection;
import com.rozsa.network.DisconnectReason;

public class DisconnectedMessage extends IncomingMessage {
    private final DisconnectReason reason;

    public DisconnectedMessage(Connection connection, DisconnectReason reason) {
        super(IncomingMessageType.DISCONNECTED, connection);
        this.reason = reason;
    }

    public DisconnectReason getReason() {
        return reason;
    }
}
