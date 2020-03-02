package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.channel.ReceiverChannel;
import com.rozsa.network.channel.SenderChannel;
import com.rozsa.network.channel.UnreliableSenderChannel;
import com.rozsa.network.message.outgoing.OutgoingMessage;
import com.rozsa.network.message.outgoing.ConnectRequestMessage;

import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ConnectionState.*;

public class Connection {
    private final PeerConfig config;
    private final Address address;
    private final PacketSender sender;
    private final ConnectionHeartbeat heartbeat;
    private final long maximumHandshakeWaitingTime;

    private long lastHandshakeAttemptTime;
    private int totalHandshakesAttempts;
    private long connectRequestReceivedTime;
    private ConnectionState state;
    private DisconnectReason disconnectReason;

    // TODO: testing purpose
    private ConcurrentHashMap<DeliveryMethod, SenderChannel> senderChannels;
    private ConcurrentHashMap<DeliveryMethod, ReceiverChannel> receiverChannels;


    Connection(PeerConfig config, Address address, PacketSender sender) {
        this.config = config;
        this.address = address;
        this.sender = sender;

        maximumHandshakeWaitingTime = config.getMaximumHandshakeAttempts() * config.getIntervalBetweenHandshakes();
        state = ConnectionState.DISCONNECTED;
        disconnectReason = DisconnectReason.NONE;

        heartbeat = new ConnectionHeartbeat(config, this, sender);
        senderChannels = new ConcurrentHashMap<>();
        receiverChannels = new ConcurrentHashMap<>();
    }

    public long getId() {
        return address.getId();
    }

    public Address getAddress() {
        return address;
    }

    public long getSRtt() {
        return heartbeat.getSRtt();
    }

    void setConnected() {
        state = CONNECTED;
        heartbeat.reset();
    }

    void setState(ConnectionState state) {
        this.state = state;
    }

    public ConnectionState getState() {
        return state;
    }

    DisconnectReason getDisconnectReason() {
        return disconnectReason;
    }

    void setDisconnectReason(DisconnectReason disconnectReason) {
        this.disconnectReason = disconnectReason;
    }

    void setAwaitingConnectEstablished() {
        setState(AWAITING_CONNECT_ESTABLISHED);
        connectRequestReceivedTime = Clock.getCurrentTime();
    }

    void sendConnectRequest() {
        setState(SEND_CONNECT_REQUEST);
        lastHandshakeAttemptTime = Clock.getCurrentTime();
        totalHandshakesAttempts = 0;
    }

    boolean isHandshakeExpired() {
        return totalHandshakesAttempts >= config.getMaximumHandshakeAttempts() &&
                !isLastHandshakeInProgress();
    }

    boolean isAwaitingConnectionEstablishedExpired() {
        boolean isWaitingConnectionEstablishedExpired = Clock.getTimePassedSince(connectRequestReceivedTime) > maximumHandshakeWaitingTime;
        if (state == AWAITING_CONNECT_ESTABLISHED && isWaitingConnectionEstablishedExpired) {
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

    void pingReceived(short seqNumber) {
        heartbeat.pingReceived(seqNumber);
    }

    void pongReceived(short seqNumber) {
        heartbeat.pongReceived(seqNumber);
    }

    void handleTimeout() {
        disconnectReason = DisconnectReason.TIMEOUT;
        setState(DISCONNECTED);
    }

    void update() {
        receiverChannels.values().forEach(ReceiverChannel::update);
        senderChannels.values().forEach(SenderChannel::update);
        heartbeat.update();
    }

    void handshake() {
        switch (state) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                handleSendConnectRequest();
                break;
            case AWAITING_CONNECT_ESTABLISHED:
                handleAwaitingConnectEstablished();
                break;
            // should not be in any of the states bellow if it is a handshake.
            case DISCONNECTED:
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
        byte[] data = connReq.getData();
        sender.send(address, data, connReq.getDataLength());

        state = AWAITING_CONNECT_RESPONSE;

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
                ", ctrlState=" + state +
                '}';
    }
}
