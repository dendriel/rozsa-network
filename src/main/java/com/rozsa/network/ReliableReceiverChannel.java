package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ReliableReceiverChannel extends ReceiverChannel {
    private final Address addr;
    private final PacketSender sender;
    private short expectedSeqNumber;
    private short maxSeqNumber;
    private Map<Short, IncomingMessage> withholdSeqNumbers;
    private Set<Short> acksToSend;

    ReliableReceiverChannel(Address addr, PacketSender sender, IncomingMessagesQueue incomingMessagesQueue, short maxSeqNumber) {
        super(DeliveryMethod.RELIABLE, incomingMessagesQueue);
        this.addr = addr;
        this.sender = sender;
        this.maxSeqNumber = maxSeqNumber;

        expectedSeqNumber = 0;
        acksToSend = new HashSet<>();
        withholdSeqNumbers = new HashMap<>();
    }

    @Override
    public void update() {
        acksToSend.clear();
        while (!incomingMessages.isEmpty()) {
            handleIncomingMessage(incomingMessages.poll());
        }

        sendAcks();
    }

    private void sendAcks() {
        // TODO TODO send all acks in a single message.
        acksToSend.forEach(ack -> {
            byte[] data = MessageSerializer.serialize(MessageType.ACK, DeliveryMethod.RELIABLE, ack);
            sender.send(addr, data, data.length);
//            Logger.debug("Sent ack %d", ack);
        });
    }

    private void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
//            Logger.error("Unhandled channel message received: %s", message.getType());
            return;
        }

        short seqNumber = message.getSeqNumber();
        if (seqNumber == expectedSeqNumber) {
//            Logger.debug("Received expected msg seq number %d", expectedSeqNumber);
            acksToSend.add(expectedSeqNumber);
            incomingMessagesQueue.enqueue(message);
            updateExpectedSeqNumber();
        }
        else if (expectedSeqNumber <= (maxSeqNumber / 2) && seqNumber > (maxSeqNumber / 2)) {
            // if received seq number is not in the same half as the expected seq number, probably
            // received a already acked seq number.
//            Logger.debug("Unexpected message. Different half: %d - exp %d", seqNumber, expectedSeqNumber);
            acksToSend.add(seqNumber);
        }
        // too early message?
        else if (seqNumber > expectedSeqNumber) {
//            Logger.debug("Received to early msg seq number %d expected %d", seqNumber, expectedSeqNumber);
            if (withholdSeqNumbers.containsKey(seqNumber)) {
//                Logger.debug("Already dispatched this message seq number %d", seqNumber);
                return;
            }
            acksToSend.add(seqNumber);
            incomingMessagesQueue.enqueue(message);
            withholdSeqNumbers.put(seqNumber, message);
        }
        else {
            // We don't expect this message sequence number.. resend its ack
//            Logger.debug("Unexpected message: %d - exp %d", seqNumber, expectedSeqNumber);
            acksToSend.add(seqNumber);
        }
    }

    private void updateExpectedSeqNumber() {
        expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
//        Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        while(withholdSeqNumbers.containsKey(expectedSeqNumber)) {
            withholdSeqNumbers.remove(expectedSeqNumber);
            expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
//            Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        }
    }
}
