package com.rozsa.network.channel;

import com.rozsa.network.IncomingMessagesQueue;
import com.rozsa.network.Logger;
import com.rozsa.network.message.IncomingMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiverChannel extends BaseChannel {
    private final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    private final IncomingMessagesQueue incomingMessagesQueue;

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
        switch (message.getType()) {
            case USER_DATA:
                incomingMessagesQueue.enqueue(message);
                break;
            default:
                Logger.error("Unhandled channel message received: %s", message.getType());
                break;
        }
    }


    public static ReceiverChannel create(DeliveryMethod deliveryMethod, IncomingMessagesQueue incomingMessagesQueue) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableReceiverChannel(incomingMessagesQueue);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableReceiverChannel(incomingMessagesQueue);
        }
    }
}
