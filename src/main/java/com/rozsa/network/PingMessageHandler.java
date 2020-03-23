package com.rozsa.network;

class PingMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;

    PingMessageHandler(ConnectionHolder connHolder, CachedMemory cachedMemory) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received ping from unconnected source %s.", addr);
            return;
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                Logger.warn("Received ping while in an invalid state. Source %s.", addr);
                break;
            case CONNECTED:
                conn.pingReceived(seqNumber);
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the ping.", addr);
            default:
                break;
        }
    }
}
