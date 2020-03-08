package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;
import com.rozsa.network.message.IncomingUserDataMessage;

public class UserDataHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;

    public UserDataHandler(
            ConnectionHolder connHolder,
            IncomingMessagesQueue incomingMessages
    ) {
        this.connHolder = connHolder;
        this.incomingMessages = incomingMessages;
    }

    @Override
    public void handle(Address addr, DeliveryMethod method, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received user data from %s but handshake nor connection doesn't even exist!.", addr);
            return;
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_ESTABLISHED:
                // received user data while waiting for connect established. Connect established message must got lost in
                // its way. Consider this user data as a sign of connection established.
                conn.setConnected();
                connHolder.promoteConnection(conn);
                incomingMessages.enqueue(new ConnectedMessage(conn));
            case CONNECTED:
                IncomingUserDataMessage dataMessage = new IncomingUserDataMessage(conn, seqNumber, data, length);
                conn.enqueueIncomingMessage(dataMessage, method);
                break;
            case AWAITING_CONNECT_RESPONSE:
            case SEND_CONNECT_REQUEST:
            case DISCONNECTED:
                Logger.warn("Received user data from %s but isn't connected yet.", conn);
            default:
                break;
        }
    }
}
