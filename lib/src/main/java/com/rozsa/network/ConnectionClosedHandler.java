package com.rozsa.network;

class ConnectionClosedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;

    ConnectionClosedHandler(ConnectionHolder connHolder, CachedMemory cachedMemory) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received closed message from unconnected source %s.", addr);
            return;
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
            case AWAITING_APPROVAL:
            case CONNECTION_APPROVED:
            case CONNECTION_DENIED:
                Logger.warn("Received closed message from %s while not being connected.", addr);
                break;
            case CONNECTED:
                conn.setDisconnectReason(DisconnectReason.REMOTE_CLOSE);
                conn.setState(ConnectionState.DISCONNECTED);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from %s.", conn);
            default:
                break;
        }
    }
}
