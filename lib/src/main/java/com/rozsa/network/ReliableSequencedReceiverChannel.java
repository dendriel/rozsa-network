package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

class ReliableSequencedReceiverChannel extends ReliableReceiverChannel {
    ReliableSequencedReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize,
            int channelId
    ) {
        super(addr, sender, incomingMessagesQueue, cachedMemory, maxSeqNumber, windowSize, DeliveryMethod.RELIABLE_SEQUENCED, channelId);
    }

    @Override
    protected void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            cachedMemory.freeBuffer(message.getData());
            return;
        }

        short seqNumber = message.getSeqNumber();
        int relativeAckNumber = expectedSeqNumber - ((seqNumber < expectedSeqNumber) ? seqNumber + maxSeqNumber : seqNumber);

        // seqNumber is equals or higher than expected.
        if (Math.abs(relativeAckNumber) < windowSize) {
            acksToSend.add(seqNumber);
//            Logger.debug("Dispatch message seq number: %d", seqNumber);
            dispatch(message);
            expectedSeqNumber = (short)((seqNumber + 1) % maxSeqNumber);
        }
        else {
//            Logger.debug("Unexpected seq number: %d - exp: %d - rel: %d", seqNumber, expectedSeqNumber, relativeAckNumber);
            // We don't expect this message sequence number.. resend its ack
            acksToSend.add(seqNumber);
            cachedMemory.freeBuffer(message.getData());
        }
    }

    @Override
    protected void dispatchFragment(IncomingMessage message) {
        super.dispatchFragment(message);
        // Discard any previous withhold messages because they are outdated and will never be completed.
//        Logger.debug("Discarding %d withhold fragments.", withholdFragments.size());
        withholdFragments.values().forEach(m -> cachedMemory.freeBuffer(m.getData()));
        withholdFragments.clear();
    }
}
