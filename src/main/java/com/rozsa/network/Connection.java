package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.channel.ReceiverChannel;
import com.rozsa.network.channel.SenderChannel;
import com.rozsa.network.channel.UnreliableSenderChannel;
import com.rozsa.network.message.outgoing.OutgoingMessage;
import com.rozsa.network.message.outgoing.ConnectRequestMessage;

import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ControlConnectionState.*;

public class Connection {
    private final PeerConfig config;
    private final Address address;
    private final PacketSender sender;

    private long lastHandshakeAttemptTime;
    private int totalHandshakesAttempts;

    private long connectRequestReceivedTime;

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

    void setAwaitingConnectEstablished() {
        setCtrlState(AWAITING_CONNECT_ESTABLISHED);
        connectRequestReceivedTime = Clock.getCurrentTime();
    }

    void sendConnectRequest() {
        setCtrlState(SEND_CONNECT_REQUEST);
        lastHandshakeAttemptTime = Clock.getCurrentTime();
        totalHandshakesAttempts = 0;
    }

    ControlConnectionState getCtrlState() {
        return ctrlState;
    }

    boolean isHandshakeExpired() {
        return totalHandshakesAttempts >= config.getMaximumHandshakeAttempts();
    }

    boolean isAwaitingConnectionEstablishedExpired() {
        boolean isWaitingConnectionEstablishedExpired = Clock.getTimePassedSince(connectRequestReceivedTime) > config.getMaximumHandshakeWaitingTime();
        if (ctrlState == AWAITING_CONNECT_ESTABLISHED && isWaitingConnectionEstablishedExpired) {
            return true;
        }

        return false;
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
            case AWAITING_CONNECT_ESTABLISHED:
                handleAwaitingConnectEstablished();
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

        ConnectRequestMessage connReq = new ConnectRequestMessage();
        byte[] data = connReq.serialize();
        sender.send(address, data, connReq.getDataLength());

        ctrlState = AWAITING_CONNECT_RESPONSE;

        lastHandshakeAttemptTime = Clock.getCurrentTime();
        totalHandshakesAttempts++;
    }

    private void handleAwaitingConnectEstablished() {

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
                "address=" + address.getIp() + ":" + address.getPort() +
                ", state=" + state +
                ", ctrlState=" + ctrlState +
                '}';
    }
}
