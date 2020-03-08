package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ConnectionState.*;

public class Connection {
    private final PeerConfig config;
    private final Address address;
    private final PacketSender sender;
    private final IncomingMessagesQueue incomingMessages;
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


    Connection(PeerConfig config, Address address, PacketSender sender, IncomingMessagesQueue incomingMessages) {
        this.config = config;
        this.address = address;
        this.sender = sender;
        this.incomingMessages = incomingMessages;

        maximumHandshakeWaitingTime = config.getMaximumHandshakeAttempts() * config.getIntervalBetweenHandshakes();
        state = ConnectionState.DISCONNECTED;
        disconnectReason = DisconnectReason.NONE;

        heartbeat = new ConnectionHeartbeat(this, sender, incomingMessages, config);
        senderChannels = new ConcurrentHashMap<>();
        receiverChannels = new ConcurrentHashMap<>();
    }

    public long getId() {
        return address.getId();
    }

    public Address getAddress() {
        return address;
    }

    long getSRtt() {
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

    void enqueueOutgoingMessage(OutgoingMessage msg, DeliveryMethod deliveryMethod) {
        SenderChannel channel = getOrCreateSenderChannel(deliveryMethod);
        channel.enqueue(msg);
    }

    void enqueueIncomingMessage(IncomingMessage msg, DeliveryMethod deliveryMethod) {
        ReceiverChannel channel = getOrCreateReceiverChannel(deliveryMethod);
        channel.enqueue(msg);
    }

    private SenderChannel getOrCreateSenderChannel(DeliveryMethod deliveryMethod) {
        senderChannels.computeIfAbsent(deliveryMethod, this::createSenderChannel);
        return senderChannels.get(deliveryMethod);
    }

    private SenderChannel createSenderChannel(DeliveryMethod deliveryMethod) {
        return SenderChannel.create(deliveryMethod, address, sender, heartbeat::getResendDelay);
    }

    private ReceiverChannel getOrCreateReceiverChannel(DeliveryMethod deliveryMethod) {
        receiverChannels.computeIfAbsent(deliveryMethod, this::createReceiverChannel);
        return receiverChannels.get(deliveryMethod);
    }

    private ReceiverChannel createReceiverChannel(DeliveryMethod deliveryMethod) {
        return ReceiverChannel.create(deliveryMethod, address, sender, incomingMessages);
    }

    void pingReceived(short seqNumber) {
        heartbeat.pingReceived(seqNumber);
    }

    void pongReceived(short seqNumber) {
        heartbeat.pongReceived(seqNumber);
    }

    void ackReceived(IncomingMessage ack, DeliveryMethod method) {
        SenderChannel channel = getOrCreateSenderChannel(method);
        channel.enqueueAck(ack);
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
            // peer loop handles awaiting connect established timeouts.
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

        byte[] data = MessageSerializer.serialize(MessageType.CONNECTION_REQUEST);
        sender.send(address, data, data.length);

        state = AWAITING_CONNECT_RESPONSE;

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
                "address=" + address.getIp() + ":" + address.getPort() +
                ", ctrlState=" + state +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
