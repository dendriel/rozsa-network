package com.rozsa.network;

public class PingMessageHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;

    public PingMessageHandler(ConnectionHolder connHolder) {
        this.connHolder = connHolder;
    }


    @Override
    public void handle(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received ping from unconnected source %s.", addr);
            return;
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                Logger.warn("Received ping while in an invalid state. Source %s.", addr);
                break;
            case CONNECTED:
                conn.pingReceived();
                break;
            case DISCONNECTED:
                Logger.warn("Already disconnected from source %s. Won't process the ping.", addr);
                break;
            case CLOSED:
            default:
                break;
        }
    }
}