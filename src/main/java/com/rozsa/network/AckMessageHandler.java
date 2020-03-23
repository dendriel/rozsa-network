package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

class AckMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;

    AckMessageHandler(ConnectionHolder connHolder, CachedMemory cachedMemory) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            cachedMemory.freeBuffer(data);
            Logger.warn("Received ack from unconnected source %s.", addr);
            return;
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                cachedMemory.freeBuffer(data);
                Logger.warn("Received ack while in an invalid state. Source %s.", addr);
                break;
            case CONNECTED:
                if (length == 0) {
                    Logger.error("Invalid ack format.");
                    return;
                }
                MessageType messageType = MessageType.from(data[0]);
                IncomingMessage ackMsg = new IncomingMessage(IncomingMessageType.ACK, conn, seqNumber, data, length, messageType, false);
                conn.ackReceived(ackMsg, messageType);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the ack.", addr);
            default:
                cachedMemory.freeBuffer(data);
                break;
        }
    }
}
