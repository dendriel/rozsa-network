package com.rozsa.network;

class UnreliableSequencedSenderChannel extends UnreliableSenderChannel {
    UnreliableSequencedSenderChannel(
            Address addr,
            PacketSender sender,
            CachedMemory cachedMemory,
            short maxSeqNumbers
    ) {
        super(addr, sender, cachedMemory, DeliveryMethod.UNRELIABLE_SEQUENCED, maxSeqNumbers);
    }
}
