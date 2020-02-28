package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.channel.ReceiverChannel;
import com.rozsa.network.channel.SenderChannel;
import com.rozsa.network.channel.UnreliableSenderChannel;
import com.rozsa.network.message.outgoing.OutgoingMessage;
import com.rozsa.network.proto.ConnectionRequestMessage;

import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ControlConnectionState.AWAITING_CONNECT_RESPONSE;

public class Connection {
    private final Address address;

    private ConnectionState state;
    private ControlConnectionState ctrlState;

    private PacketSender sender;

    // TODO: testing purpose
    private ConcurrentHashMap<DeliveryMethod, SenderChannel> senderChannels;
    private ConcurrentHashMap<DeliveryMethod, ReceiverChannel> receiverChannels;


    Connection(Address address, PacketSender sender) {
        this.address = address;
        this.sender = sender;

        state = ConnectionState.DISCONNECTED;
        ctrlState = ControlConnectionState.DISCONNECTED;

        senderChannels = new ConcurrentHashMap<>();
        receiverChannels = new ConcurrentHashMap<>();
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

    ControlConnectionState getCtrlState() {
        return ctrlState;
    }

    void enqueueMessage(OutgoingMessage msg, DeliveryMethod deliveryMethod) {
        SenderChannel channel = getOrCreateChannel(deliveryMethod);
        channel.enqueue(msg);
    }

    private SenderChannel getOrCreateChannel(DeliveryMethod deliveryMethod) {
        senderChannels.computeIfAbsent(deliveryMethod, this::create);
        return senderChannels.get(deliveryMethod);
    }

    private SenderChannel create(DeliveryMethod deliveryMethod) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableSenderChannel(address, sender);
            default:
                System.out.println("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableSenderChannel(address, sender);
        }
    }

    void update() {
        receiverChannels.values().forEach(ReceiverChannel::update);
        senderChannels.values().forEach(SenderChannel::update);
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

        byte[] data = connReq.serialize();
        sender.send(address, data, connReq.getDataLength());
        ctrlState = AWAITING_CONNECT_RESPONSE;
    }
}
