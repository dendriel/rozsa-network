package com.rozsa.network;

public class NetConstants {
    /**
     * 1 byte - message type
     * 1 byte - delivery method
     * 2 bytes - sequence number
     */
    public static final int MsgHeaderSize = 4;

    static final int DefaultMaximumHandshakeAttempts = 5;
    static final long DefaultIntervalBetweenHandshakesInMillis = 1000;
    static final int DefaultReceiveBufferSize = 131071; // udp buffer size.
    static final float DefaultPingInterval = 1;
    static final float DefaultConnectionTimeout = 5;
    static final boolean DefaultIsPingUpdatedEventEnabled = false;
}
