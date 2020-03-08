package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;

public class ConnectionRequestHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final PacketSender packetSender;

    public ConnectionRequestHandler(
            ConnectionHolder connHolder,
            IncomingMessagesQueue incomingMessages,
            PacketSender packetSender
    ) {
        this.connHolder = connHolder;
        this.incomingMessages = incomingMessages;
        this. packetSender = packetSender;
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        byte[] buf;
        switch (conn.getState()) {
            case DISCONNECTED:
                // if disconnected, send connect response and await for connect established.
                conn.setAwaitingConnectEstablished();
                buf = MessageSerializer.serialize(MessageType.CONNECTION_RESPONSE);
                packetSender.send(conn.getAddress(), buf, buf.length);
                break;

            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
                // if received connect request while sending connect request itself, establish the connection right away.
                incomingMessages.enqueue(new ConnectedMessage(conn));
                connHolder.promoteConnection(conn);
                buf = MessageSerializer.serialize(MessageType.CONNECTION_ESTABLISHED);
                packetSender.send(conn.getAddress(), buf, buf.length);
                break;

            case CONNECTED:
                Logger.debug("Already connected to %s. Resend connect response.", conn);
                buf = MessageSerializer.serialize(MessageType.CONNECTION_RESPONSE);
                packetSender.send(conn.getAddress(), buf, buf.length);
                break;
            default:
                break;
        }
    }
}
