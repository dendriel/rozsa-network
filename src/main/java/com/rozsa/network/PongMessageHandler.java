package com.rozsa.network;

public class PongMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;

    public PongMessageHandler(ConnectionHolder connHolder) {
        this.connHolder = connHolder;
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received pong from unconnected source %s.", addr);
            return;
        }

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
                break;
            default:
                break;
        }
    }
}
