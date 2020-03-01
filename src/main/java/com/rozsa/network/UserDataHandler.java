package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;

public class UserDataHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue messageQueue;

    public UserDataHandler(
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
            Logger.warn("Received user data from %s but handshake nor connection doesn't even exist!.", addr);
            return;
        }

        switch (conn.getState()) {
            case CONNECTED:
                Logger.info("Received user data from %s.", conn);
                // enqueue user data.
                break;
            case AWAITING_CONNECT_ESTABLISHED:
                // received user data while waiting for connect established. Connect established message must got lost in
                // its way. Consider this user data as a sign of connection established.
                conn.setState(ConnectionState.CONNECTED);
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                // enqueue user data.
                break;
            case AWAITING_CONNECT_RESPONSE:
            case SEND_CONNECT_REQUEST:
            case DISCONNECTED:
                Logger.warn("Received user data from %s but isn't connected yet.", conn);
            case CLOSED:
                Logger.warn("Received user data from %s but connection is closed!.", addr);
            default:
                break;
        }
    }
}
