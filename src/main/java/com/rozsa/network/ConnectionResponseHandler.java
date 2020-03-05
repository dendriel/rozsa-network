package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.outgoing.ConnectEstablishedMessage;
import com.rozsa.network.message.outgoing.OutgoingMessage;

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

    private void send(Connection conn, OutgoingMessage msg) {
        packetSender.send(conn.getAddress(), msg.getData(), msg.getDataLength());
    }

    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getState()) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                conn.setConnected();
                connHolder.promoteConnection(conn);
                incomingMessages.enqueue(new ConnectedMessage(conn));
                send(conn, new ConnectEstablishedMessage());
                break;
            case CONNECTED:
                Logger.debug("Already connected to %s. Resend connect established.", conn);
                send(conn, new ConnectEstablishedMessage());
                break;
            case DISCONNECTED:
            default:
                break;
        }
    }
}
