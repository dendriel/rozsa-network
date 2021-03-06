package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

/**
 * Implements the 'receiver' part from a delivery method.
 */
public interface ReceiverChannel {
    /**
     * Channel heartbeat to dispatch incoming messages and send outgoing acks (when applicable).
     */
    void update();

    /**
     * Enqueue an incoming message to be dispatched by the channel.
     * @param message
     */
    void enqueue(IncomingMessage message);

    static ReceiverChannel create(
            DeliveryMethod deliveryMethod,
            int channelId,
            Address address,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory
    ) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableReceiverChannel(incomingMessagesQueue, cachedMemory);
            case UNRELIABLE_SEQUENCED:
                return new UnreliableSequencedReceiverChannel(incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers);
            case RELIABLE:
                return new ReliableReceiverChannel(address, sender, incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers, NetConstants.ReliableWindowSize);
            case RELIABLE_SEQUENCED:
                return new ReliableSequencedReceiverChannel(address, sender, incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers, NetConstants.ReliableWindowSize, channelId);
            case RELIABLE_ORDERED:
                return new ReliableOrderedReceiverChannel(address, sender, incomingMessagesQueue, cachedMemory, NetConstants.MaxSeqNumbers, NetConstants.ReliableWindowSize, channelId);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableReceiverChannel(incomingMessagesQueue, cachedMemory);
        }
    }
}
