package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;

public class ConnectionEstablishedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue messageQueue;

    public ConnectionEstablishedHandler(
            ConnectionHolder connHolder,
            IncomingMessagesQueue messageQueue
    ) {
        this.connHolder = connHolder;
        this.messageQueue = messageQueue;
    }

    @Override
    public void handle(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                break;
            case CONNECTED:
                Logger.info("Already connected to %s.", conn);
                break;
            case DISCONNECTED:
                Logger.info("Didn't approved connection with %s.", conn);
            case CLOSED:
            default:
                break;
        }
    }
}
