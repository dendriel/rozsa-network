package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;

import java.util.Arrays;

class ConnectionRequestHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final  CachedMemory cachedMemory;
    private final IncomingMessagesQueue incomingMessages;
    private final PacketSender packetSender;

    ConnectionRequestHandler(
            ConnectionHolder connHolder,
            CachedMemory cachedMemory,
            IncomingMessagesQueue incomingMessages,
            PacketSender packetSender
    ) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
        this.incomingMessages = incomingMessages;
        this.packetSender = packetSender;
    }

    @Override
    public void handle(Address addr, DeliveryMethod method, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        cachedMemory.freeBuffer(data);

        switch (conn.getState()) {
            case DISCONNECTED:
                // if disconnected, send connect response and await for connect established.
                conn.setAwaitingConnectEstablished();
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_RESPONSE, method, (short)0);
                break;

            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
                // if received connect request while sending connect request itself, establish the connection right away.
                incomingMessages.enqueue(new ConnectedMessage(conn));
                connHolder.promoteConnection(conn);
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_ESTABLISHED, method, (short)0);
                break;

            case CONNECTED:
                // already connected to peer. Resend connect response.
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_RESPONSE, method, (short)0);
                break;
            default:
                break;
        }
    }
}
