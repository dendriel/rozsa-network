package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;
import com.rozsa.network.message.ConnectionRequestMessage;
import com.rozsa.network.message.IncomingUserDataMessage;

import java.util.Arrays;

class ConnectionRequestHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final  CachedMemory cachedMemory;
    private final IncomingMessagesQueue incomingMessages;
    private final PacketSender packetSender;
    private final boolean isApprovalRequired;

    ConnectionRequestHandler(
            ConnectionHolder connHolder,
            CachedMemory cachedMemory,
            IncomingMessagesQueue incomingMessages,
            PacketSender packetSender,
            boolean isApprovalRequired
    ) {
        this.connHolder = connHolder;
        this.cachedMemory = cachedMemory;
        this.incomingMessages = incomingMessages;
        this.packetSender = packetSender;
        this.isApprovalRequired = isApprovalRequired;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getState()) {
            case AWAITING_APPROVAL:
            case DISCONNECTED:
                if (!isApprovalRequired) {
                    // if disconnected, send connect response and await for connect established.
                    conn.setAwaitingConnectEstablished();
                    packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_RESPONSE, (short)0);
                    cachedMemory.freeBuffer(data);
                }
                else {
                    conn.setAwaitingApproval();
                    IncomingUserDataMessage hailMessage = new IncomingUserDataMessage(conn, seqNumber, data, length, type, isFrag);
                    incomingMessages.enqueue(new ConnectionRequestMessage(conn, hailMessage));
                }
                break;
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
                // if received connect request while sending connect request itself, establish the connection right away.
                incomingMessages.enqueue(new ConnectedMessage(conn));
                connHolder.promoteConnection(conn);
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_ESTABLISHED, (short)0);
                cachedMemory.freeBuffer(data);
                break;

            case CONNECTED:
                // already connected to peer. Resend connect response.
                packetSender.sendProtocol(conn.getAddress(), MessageType.CONNECTION_RESPONSE, (short)0);
                cachedMemory.freeBuffer(data);
                break;
            default:
                cachedMemory.freeBuffer(data);
                break;
        }
    }
}
