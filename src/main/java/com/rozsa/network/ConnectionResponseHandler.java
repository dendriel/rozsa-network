package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.outgoing.ConnectEstablishedMessage;
import com.rozsa.network.message.outgoing.OutgoingMessage;

public class ConnectionResponseHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue messageQueue;
    private final PacketSender packetSender;

    public ConnectionResponseHandler(
            ConnectionHolder connHolder,
            IncomingMessagesQueue messageQueue,
            PacketSender packetSender
    ) {
        this.connHolder = connHolder;
        this.messageQueue = messageQueue;
        this. packetSender = packetSender;
    }

    private void send(Connection conn, OutgoingMessage msg) {
        packetSender.send(conn.getAddress(), msg.serialize(), msg.getDataLength());
    }

    @Override
    public void handle(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getState()) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                send(conn, new ConnectEstablishedMessage());
                break;
            case CONNECTED:
                Logger.info("Already connected to %s. Resend connect established.", conn);
                send(conn, new ConnectEstablishedMessage());
                break;
            case DISCONNECTED:
            default:
                break;
        }
    }
}
