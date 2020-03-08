package com.rozsa.network;

import com.rozsa.network.message.OutgoingMessage;

class StoredMessage {
    private boolean isValid;
    private OutgoingMessage message;
    private long sentTime;
    private short seqNumber;

    void reset() {
        message = null;
        sentTime = 0;
        seqNumber = -1;
        isValid = false;
    }

    void set(OutgoingMessage message, short seqNumber) {
        this.message = message;
        this.seqNumber = seqNumber;
        this.sentTime = Clock.getCurrentTimeInNanos();
        isValid = true;
    }

    void resetSentTime() {
        this.sentTime = Clock.getCurrentTimeInNanos();
    }

    boolean isTimeout(long timeoutTime) {
//        Logger.debug("SEQ %d isTimeout? %d > %d - %s", seqNumber, Clock.getTimePassedSinceInNanos(sentTime), timeoutTime, Clock.getTimePassedSinceInNanos(sentTime) > timeoutTime);
        return isValid && Clock.getTimePassedSinceInNanos(sentTime) > timeoutTime;
    }

    OutgoingMessage getMessage() {
        return message;
    }

    public short getSeqNumber() {
        return seqNumber;
    }
}
