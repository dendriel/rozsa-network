package com.rozsa.network;

class NetConstants {
    static final int DefaultMaximumHandshakeAttempts = 5;
    static final long DefaultIntervalBetweenHandshakesInMillis = 1000;
    static final int DefaultReceiveBufferSize = 131071; // udp buffer size.
    static final float DefaultPingInterval = 1;
    static final float DefaultConnectionTimeout = 5;
}
