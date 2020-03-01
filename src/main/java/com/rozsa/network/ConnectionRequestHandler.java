package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.outgoing.ConnectEstablishedMessage;
import com.rozsa.network.message.outgoing.ConnectResponseMessage;
import com.rozsa.network.message.outgoing.OutgoingMessage;

public class ConnectionRequestHandler implements IncomingMessageHandler {
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue messageQueue;
    private final PacketSender packetSender;

    public ConnectionRequestHandler(
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
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getState()) {
            case DISCONNECTED:
                // if disconnected, send connect response and await for connect established.
                conn.setAwaitingConnectEstablished();
                send(conn, new ConnectResponseMessage());
                break;

            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
                // if received connect request while sending connect request itself, establish the connection right away.
                messageQueue.enqueue(new ConnectedMessage(conn));
                connHolder.promoteConnection(conn);
                send(conn, new ConnectEstablishedMessage());
                break;

            case CONNECTED:
                Logger.info("Already connected to %s. Resend connect response.", conn);
                send(conn, new ConnectEstablishedMessage());
                break;
            default:
                break;
        }
    }
}
