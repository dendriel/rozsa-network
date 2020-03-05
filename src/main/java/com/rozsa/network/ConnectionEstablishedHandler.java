package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.ConnectedMessage;

public class ConnectionEstablishedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;

    public ConnectionEstablishedHandler(
            ConnectionHolder connHolder,
            IncomingMessagesQueue incomingMessages
    ) {
        this.connHolder = connHolder;
        this.incomingMessages = incomingMessages;
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
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
                incomingMessages.enqueue(new ConnectedMessage(conn));
                break;
            case CONNECTED:
                Logger.debug("Already connected to %s.", conn);
                break;
            case DISCONNECTED:
                Logger.debug("Didn't approved connection with %s.", conn);
            default:
                break;
        }
    }
}
