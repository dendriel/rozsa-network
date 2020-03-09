package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiverChannel extends BaseChannel {
    protected final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    protected final IncomingMessagesQueue incomingMessagesQueue;

    ReceiverChannel(DeliveryMethod type, IncomingMessagesQueue incomingMessagesQueue) {
        super(type);
        this.incomingMessagesQueue = incomingMessagesQueue;

        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(IncomingMessage msg) {
        incomingMessages.add(msg);
    }

    @Override
    public void update() {
        while (!incomingMessages.isEmpty()) {
            handleIncomingMessage(incomingMessages.poll());
        }
    }

    private void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            Logger.error("Unhandled channel message received: %s", message.getType());
            return;
        }

        incomingMessagesQueue.enqueue(message);
    }


    public static ReceiverChannel create(
            DeliveryMethod deliveryMethod,
            Address address,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory
    ) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableReceiverChannel(incomingMessagesQueue);
            case RELIABLE:
                return new ReliableReceiverChannel(address, sender, incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableReceiverChannel(incomingMessagesQueue);
        }
    }
}
