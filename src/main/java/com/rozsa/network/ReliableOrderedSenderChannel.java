package com.rozsa.network;

import java.util.function.LongSupplier;

class ReliableOrderedSenderChannel extends ReliableSenderChannel {
    ReliableOrderedSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider,
            int channelId
    ) {
        super(address, sender, cachedMemory, windowSize, maxSeqNumbers, resendDelayProvider, DeliveryMethod.RELIABLE_ORDERED, channelId);
    }
}
