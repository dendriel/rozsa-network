package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.concurrent.ConcurrentLinkedQueue;

abstract class ReceiverChannel {
    protected final DeliveryMethod type;
    protected final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    protected final IncomingMessagesQueue incomingMessagesQueue;
    protected final CachedMemory cachedMemory;

    ReceiverChannel(DeliveryMethod type, IncomingMessagesQueue incomingMessagesQueue, CachedMemory cachedMemory) {
        this.type = type;
        this.incomingMessagesQueue = incomingMessagesQueue;
        this.cachedMemory = cachedMemory;

        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public DeliveryMethod getType() {
        return type;
    }

    public void enqueue(IncomingMessage msg) {
        incomingMessages.add(msg);
    }

    public void update() {
        while (!incomingMessages.isEmpty()) {
            handleIncomingMessage(incomingMessages.poll());
        }
    }

    private void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            cachedMemory.freeBuffer(message.getData());
            Logger.error("Unhandled channel message received: %s", message.getType());
            return;
        }

        incomingMessagesQueue.enqueue(message);
    }

    static ReceiverChannel create(
            DeliveryMethod deliveryMethod,
            Address address,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory
    ) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableReceiverChannel(incomingMessagesQueue, cachedMemory);
            case RELIABLE:
                return new ReliableReceiverChannel(address, sender, incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableReceiverChannel(incomingMessagesQueue, cachedMemory);
        }
    }
}
