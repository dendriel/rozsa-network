package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

class AckMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;

    AckMessageHandler(ConnectionHolder connHolder) {
        this.connHolder = connHolder;
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received ack from unconnected source %s.", addr);
            return;
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                Logger.warn("Received ack while in an invalid state. Source %s.", addr);
                break;
            case CONNECTED:
                IncomingMessage ack = new IncomingMessage(IncomingMessageType.ACK, conn, seqNumber, data, length);
                conn.ackReceived(ack, deliveryMethod);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the ack.", addr);
                break;
            default:
                break;
        }
    }
}
