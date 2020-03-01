package com.rozsa.network;

public class ConnectionClosedHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;

    public ConnectionClosedHandler(ConnectionHolder connHolder) {
        this.connHolder = connHolder;
    }

    @Override
    public void handle(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received closed message from unconnected source %s.", addr);
            return;
        }

        switch (conn.getState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                Logger.warn("Received closed message from %s while not being connected.", addr);
                break;
            case CONNECTED:
                conn.setDisconnectReason(DisconnectReason.REMOTE_CLOSE);
                conn.setState(ConnectionState.DISCONNECTED);
                break;
            case DISCONNECTED:
                Logger.info("Already disconnected from %s.", conn);
            default:
                break;
        }
    }
}
