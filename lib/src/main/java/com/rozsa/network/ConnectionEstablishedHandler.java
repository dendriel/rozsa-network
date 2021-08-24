package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;

class ConnectionEstablishedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;
    private final IncomingMessagesQueue incomingMessages;

    ConnectionEstablishedHandler(
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
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                incomingMessages.enqueue(new ConnectedMessage(conn));
                break;
            case CONNECTED:
            case CONNECTION_APPROVED:
                Logger.debug("Already connected to %s.", conn);
                break;
            case DISCONNECTED:
            case AWAITING_APPROVAL:
            case CONNECTION_DENIED:
                Logger.debug("Didn't approved connection with %s.", conn);
            default:
                break;
        }
    }
}
