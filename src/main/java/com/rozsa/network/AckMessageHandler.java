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
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
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
                IncomingMessage ackMsg = new IncomingMessage(IncomingMessageType.ACK, conn, seqNumber, data, length);
                conn.ackReceived(ackMsg, deliveryMethod);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the ack.", addr);
            default:
                cachedMemory.freeBuffer(data);
                break;
        }
    }
}
