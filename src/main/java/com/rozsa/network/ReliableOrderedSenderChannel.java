package com.rozsa.network;

import java.util.function.LongSupplier;

class ReliableOrderedSenderChannel extends ReliableSenderChannel {
    ReliableOrderedSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            int mtu,
            LongSupplier resendDelayProvider,
            int channelId
    ) {
        super(address, sender, cachedMemory, windowSize, maxSeqNumbers, mtu, resendDelayProvider, DeliveryMethod.RELIABLE_ORDERED, channelId);
    }
}
