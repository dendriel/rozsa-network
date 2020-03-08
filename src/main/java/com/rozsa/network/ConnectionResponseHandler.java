package com.rozsa.network;

import com.rozsa.network.message.ConnectedMessage;

public class ConnectionResponseHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final PacketSender packetSender;

    public ConnectionResponseHandler(
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
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        byte[] buf;
        switch (conn.getState()) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                incomingMessages.enqueue(new ConnectedMessage(conn));
                buf = MessageSerializer.serialize(MessageType.CONNECTION_ESTABLISHED);
                packetSender.send(conn.getAddress(), buf, buf.length);
                break;
            case CONNECTED:
                Logger.debug("Already connected to %s. Resend connect established.", conn);
                buf = MessageSerializer.serialize(MessageType.CONNECTION_ESTABLISHED);
                packetSender.send(conn.getAddress(), buf, buf.length);
                break;
            case DISCONNECTED:
            default:
                break;
        }
    }
}
