package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.rozsa.network.ConnectionState.*;

public class Connection {
    private final PeerConfig config;
    private final Address address;
    private final PacketSender sender;
    private final IncomingMessagesQueue incomingMessages;
    private final CachedMemory cachedMemory;
    private final ConnectionHeartbeat heartbeat;
    private final long maximumHandshakeWaitingTime;
    private final int mtu;

    private OutgoingMessage hailMessage;

    private long lastHandshakeAttemptTime;
    private int totalHandshakesAttempts;
    private long connectRequestReceivedTime;

    private ConnectionState state;
    private DisconnectReason disconnectReason;

    private ConcurrentHashMap<Byte, SenderChannel> senderChannels;
    private ConcurrentHashMap<Byte, ReceiverChannel> receiverChannels;

    Connection(PeerConfig config, Address address, PacketSender sender, IncomingMessagesQueue incomingMessages, CachedMemory cachedMemory) {
        this.config = config;
        this.address = address;
        this.sender = sender;
        this.incomingMessages = incomingMessages;
        this.cachedMemory = cachedMemory;

        mtu = config.getMtu();

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

    public int getSRtt() {
        return heartbeat.getSRtt();
    }

    public int getSRttMicros() {
        return heartbeat.getSRttMicros();
    }

    void setConnected() {
        state = CONNECTED;
        heartbeat.reset();
    }

    void setState(ConnectionState state) {
        this.state = state;
    }

    void setHailMessage(OutgoingMessage hailMessage) {
        this.hailMessage = hailMessage;
    }

    public ConnectionState getState() {
        return state;
    }

    public boolean isConnecting() {
        return state == SEND_CONNECT_REQUEST ||
                state == AWAITING_CONNECT_RESPONSE ||
                state == AWAITING_CONNECT_ESTABLISHED;
    }

    public boolean isConnected() {
        return state == CONNECTED;
    }

    public boolean isDisconnected() {
        return state == DISCONNECTED;
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

    void setAwaitingApproval() {
        setState(AWAITING_APPROVAL);
        // won't setup a timeout because this action depends on the host.
    }

    void setConnectionApproved() {
        setState(CONNECTION_APPROVED);
    }

    void setConnectionDenied(DisconnectReason reason) {
        disconnectReason = reason;
        setState(CONNECTION_DENIED);
    }

    void setDisconnected(DisconnectReason reason) {
        disconnectReason = reason;
        setState(DISCONNECTED);
    }

    boolean isHandshakeExpired() {
        return totalHandshakesAttempts >= config.getMaximumHandshakeAttempts() &&
                !isLastHandshakeInProgress();
    }

    boolean isAwaitingConnectionEstablishedExpired() {
        boolean isWaitingConnectionEstablishedExpired = Clock.getTimePassedSince(connectRequestReceivedTime) > maximumHandshakeWaitingTime;
        return state == AWAITING_CONNECT_ESTABLISHED && isWaitingConnectionEstablishedExpired;
    }

    void enqueueOutgoingMessage(OutgoingMessage msg, DeliveryMethod deliveryMethod, int channelId) {
        SenderChannel channel = getOrCreateSenderChannel(deliveryMethod, channelId);
        channel.enqueue(msg);
    }

    void enqueueIncomingMessage(IncomingMessage msg, DeliveryMethod deliveryMethod, int channelId) {
        ReceiverChannel channel = getOrCreateReceiverChannel(deliveryMethod, channelId);
        channel.enqueue(msg);
    }

    private SenderChannel getOrCreateSenderChannel(DeliveryMethod deliveryMethod, int channelId) {
        byte channelIndex = (byte)(deliveryMethod.getId() + channelId);
        senderChannels.computeIfAbsent(channelIndex, c -> createSenderChannel(deliveryMethod, channelId));
        return senderChannels.get(channelIndex);
    }

    private SenderChannel createSenderChannel(DeliveryMethod deliveryMethod, int channelId) {
        return SenderChannel.create(deliveryMethod, channelId, address, sender, cachedMemory, heartbeat::getResendDelay, mtu);
    }

    private ReceiverChannel getOrCreateReceiverChannel(DeliveryMethod deliveryMethod, int channelId) {
        byte channelIndex = (byte)(deliveryMethod.getId() + channelId);
        receiverChannels.computeIfAbsent(channelIndex, c -> createReceiverChannel(deliveryMethod, channelId));
        return receiverChannels.get(channelIndex);
    }

    private ReceiverChannel createReceiverChannel(DeliveryMethod deliveryMethod, int channelId) {
        return ReceiverChannel.create(deliveryMethod, channelId, address, sender, incomingMessages, cachedMemory);
    }

    void pingReceived(short seqNumber) {
        heartbeat.pingReceived(seqNumber);
    }

    void pongReceived(short seqNumber) {
        heartbeat.pongReceived(seqNumber);
    }

    void ackReceived(IncomingMessage ack, MessageType type) {
        SenderChannel channel = getOrCreateSenderChannel(DeliveryMethod.from(type.getBaseId()), type.getOffset());
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
            case CONNECTION_APPROVED:
                setAwaitingConnectEstablished();
                sender.sendProtocol(getAddress(), MessageType.CONNECTION_RESPONSE, (short)0);
                break;
            case CONNECTION_DENIED:
                setState(DISCONNECTED);
                OutgoingMessage msg = new OutgoingMessage(cachedMemory, 1);
                msg.writeByte(disconnectReason.getId());
                sender.encodeSendProtocol(getAddress(), MessageType.CONNECTION_DENIED, (short)0, msg.getData(), msg.getDataWritten());
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

        if (hailMessage != null) {
            sender.encodeSendProtocol(address, MessageType.CONNECTION_REQUEST, (short)0, hailMessage.getData(), hailMessage.getDataWritten());
        }
        else {
            sender.sendProtocol(address, MessageType.CONNECTION_REQUEST, (short)0);
        }

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
