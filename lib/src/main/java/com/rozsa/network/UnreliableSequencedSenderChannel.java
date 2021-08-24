package com.rozsa.network;

class UnreliableSequencedSenderChannel extends UnreliableSenderChannel {
    UnreliableSequencedSenderChannel(
            Address addr,
            PacketSender sender,
            CachedMemory cachedMemory,
            short maxSeqNumbers,
            int channelId
    ) {
        super(addr, sender, cachedMemory, DeliveryMethod.UNRELIABLE_SEQUENCED, maxSeqNumbers, channelId);
    }
}
