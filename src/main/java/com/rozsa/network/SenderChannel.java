package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.function.LongSupplier;

/**
 * Implements the 'sender' part from a delivery method.
 */
public interface SenderChannel {
    /**
     * Channel heartbeat to process incoming messages and send outgoing messages.
     */
    void update();

    /**
     * Enqueue an outgoing message to be sent by this channel.
     * @param message
     */
    void enqueue(OutgoingMessage message);

    /**
     * Enqueue an incoming ack message to be processed by this channel.
     * @param ack
     */
    void enqueueAck(IncomingMessage ack);

    static SenderChannel create(
            DeliveryMethod deliveryMethod,
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            LongSupplier latencyProvider
    ) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableSenderChannel(address, sender, cachedMemory);
            case UNRELIABLE_SEQUENCED:
                return new UnreliableSequencedSenderChannel(address, sender, cachedMemory, NetConstants.MaxSeqNumbers);
            case RELIABLE:
                return new ReliableSenderChannel(address, sender, cachedMemory, NetConstants.ReliableWindowSize, NetConstants.MaxSeqNumbers, latencyProvider);
            case RELIABLE_SEQUENCED:
                return new ReliableSequencedSenderChannel(address, sender, cachedMemory, NetConstants.ReliableWindowSize, NetConstants.MaxSeqNumbers, latencyProvider);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableSenderChannel(address, sender, cachedMemory);
        }
    }
}
