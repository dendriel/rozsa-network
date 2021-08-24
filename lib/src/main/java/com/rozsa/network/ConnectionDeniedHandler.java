package com.rozsa.network;

import com.rozsa.network.message.DisconnectedMessage;

public class ConnectionDeniedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;
    private final IncomingMessagesQueue incomingMessages;

    public ConnectionDeniedHandler(
            ConnectionHolder connHolder,
            CachedMemory cachedMemory,
            IncomingMessagesQueue incomingMessages
    ) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
        this.incomingMessages = incomingMessages;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.getConnection(addr.getId());
            if (conn == null) {
                return;
            }
        }

        DisconnectReason reason = DisconnectReason.from(data[0]);
        conn.setDisconnected(reason);
        DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, reason);
        incomingMessages.enqueue(disconnectedMessage);

        cachedMemory.freeBuffer(data);
    }
}
