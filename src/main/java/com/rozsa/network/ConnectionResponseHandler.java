package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;

class ConnectionResponseHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final CachedMemory cachedMemory;
    private final IncomingMessagesQueue incomingMessages;
    private final PacketSender packetSender;

    ConnectionResponseHandler(
            ConnectionHolder connHolder,
            CachedMemory cachedMemory,
            IncomingMessagesQueue incomingMessages,
            PacketSender packetSender
    ) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
        this.incomingMessages = incomingMessages;
        this. packetSender = packetSender;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                incomingMessages.enqueue(new ConnectedMessage(conn));
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_ESTABLISHED, (short)0);
                break;
            case CONNECTED:
                // already connected to peer. Resend connect established.
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_ESTABLISHED, (short)0);
                break;
            case DISCONNECTED:
            default:
                break;
        }
    }
}
