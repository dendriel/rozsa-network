package com.rozsa.network;

import com.rozsa.network.channel.BaseChannel;
import com.rozsa.network.channel.ChannelType;
import com.rozsa.network.proto.ConnectionRequestMessage;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.rozsa.network.ControlConnectionState.AWAITING_CONNECT_RESPONSE;

public class Connection {
    private final Address address;

    private ConnectionState state;
    private ControlConnectionState ctrlState;

    private PacketSender sender;

    // TODO: testing purpose
    private EnumMap<ChannelType, BaseChannel> senderChannels;
    private EnumMap<ChannelType, BaseChannel> receiverChannels;
    private ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    private ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;

    Connection(Address address, PacketSender sender) {
        this.address = address;
        this.sender = sender;

        state = ConnectionState.DISCONNECTED;
        ctrlState = ControlConnectionState.DISCONNECTED;

        senderChannels = new EnumMap<>(ChannelType.class);
        receiverChannels = new EnumMap<>(ChannelType.class);
        outgoingMessages = new ConcurrentLinkedQueue<>();
        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public long getId() {
        return address.getId();
    }

    public Address getAddress() {
        return address;
    }

    public boolean isClosed() {
        return ctrlState.equals(ControlConnectionState.CLOSED);
    }

    public ConnectionState getState() {
        return state;
    }

    void setCtrlState(ControlConnectionState ctrlState) {
        this.ctrlState = ctrlState;
    }

    public ControlConnectionState getCtrlState() {
        return ctrlState;
    }

    void handshake() {
        switch (ctrlState) {
            case SEND_CONNECT_REQUEST:
                handleSendConnectRequest();
                break;
            case AWAITING_CONNECT_RESPONSE:
            case DISCONNECTED:
            case CLOSED:
            case CONNECTED:
            default:
                break;
        }

        // handle incoming messages
    }

    private void handleSendConnectRequest() {
        ConnectionRequestMessage connReq = new ConnectionRequestMessage();

        byte[] data = new byte[1];
        connReq.serialize(data, data.length);
        sender.send(address, data, data.length);
        ctrlState = AWAITING_CONNECT_RESPONSE;
    }
}
