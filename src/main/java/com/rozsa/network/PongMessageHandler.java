package com.rozsa.network;

class PongMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;

    PongMessageHandler(ConnectionHolder connHolder, CachedMemory cachedMemory) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received pong from unconnected source %s.", addr);
            return;
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                Logger.warn("Received pong while in an invalid state. Source %s.", addr);
                break;
            case CONNECTED:
                conn.pongReceived(seqNumber);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the pong.", addr);
            default:
                break;
        }
    }
}
