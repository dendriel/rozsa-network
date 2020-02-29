package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.channel.ReceiverChannel;
import com.rozsa.network.channel.SenderChannel;
import com.rozsa.network.channel.UnreliableSenderChannel;
import com.rozsa.network.message.outgoing.OutgoingMessage;
import com.rozsa.network.message.outgoing.ConnectionRequestMessage;

import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ControlConnectionState.AWAITING_CONNECT_RESPONSE;

public class Connection {
    private final PeerConfig config;
    private final Address address;
    private final PacketSender sender;

    private long lastHandshakeAttemptTime;
    private int totalHandshakesAttempts;

    private ConnectionState state;
    private ControlConnectionState ctrlState;

    // TODO: testing purpose
    private ConcurrentHashMap<DeliveryMethod, SenderChannel> senderChannels;
    private ConcurrentHashMap<DeliveryMethod, ReceiverChannel> receiverChannels;


    Connection(PeerConfig config, Address address, PacketSender sender) {
        this.config = config;
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

    public boolean isHandshakeExpired() {
        return totalHandshakesAttempts >= config.getMaximumHandshakeAttempts();
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
                Logger.info("Unhandled delivery method!! " + deliveryMethod);
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
            case AWAITING_CONNECT_RESPONSE:
                handleSendConnectRequest();
                break;
            case DISCONNECTED:
            case CLOSED:
            case CONNECTED:
            default:
                break;
        }
    }

    private void handleSendConnectRequest() {
        if (isHandshakeExpired() || isLastHandshakeInProgress()) {
            return;
        }

        ConnectionRequestMessage connReq = new ConnectionRequestMessage();

        byte[] data = connReq.serialize();
        sender.send(address, data, connReq.getDataLength());
        ctrlState = AWAITING_CONNECT_RESPONSE;

        lastHandshakeAttemptTime = Clock.getCurrentTime();
        totalHandshakesAttempts++;
    }

    private boolean isLastHandshakeInProgress() {
        if (totalHandshakesAttempts == 0) {
            return false;
        }

        return Clock.getTimePassedSince(lastHandshakeAttemptTime) <= config.getIntervalBetweenHandshakes();
    }

    @Override
    public String toString() {
        return "Connection{" +
                "address=" + address +
                ", lastHandshakeAttemptTime=" + lastHandshakeAttemptTime +
                ", totalHandshakesAttempts=" + totalHandshakesAttempts +
                ", state=" + state +
                ", ctrlState=" + ctrlState +
                '}';
    }
}
