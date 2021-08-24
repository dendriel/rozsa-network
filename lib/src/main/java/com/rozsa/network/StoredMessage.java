package com.rozsa.network;

class StoredMessage {
    private boolean isValid;
    private long sentTime;
    private byte[] encodedMsg;
    private int encodedMsgLength;

    void reset() {
        encodedMsg = null;
        sentTime = 0;
        isValid = false;
    }

    void set(byte[] encodedMsg, int encodedMsgLength) {
        this.encodedMsg = encodedMsg;
        this.encodedMsgLength = encodedMsgLength;
        this.sentTime = Clock.getCurrentTimeInNanos();
        isValid = true;
    }

    void resetSentTime() {
        this.sentTime = Clock.getCurrentTimeInNanos();
    }

    boolean isTimeout(long timeoutTime) {
        return isValid && Clock.getTimePassedSinceInNanos(sentTime) > timeoutTime;
    }

    byte[] getEncodedMsg() {
        return encodedMsg;
    }

    int getEncodedMsgLength() {
        return encodedMsgLength;
    }
}
