package com.rozsa.network;

class NetConstants {
    /**
     * 1 byte - message type
     * 2 bytes - sequence number
     */
    static final int MsgHeaderSize = 3;
    static final int MessageTypeHeaderIdx = 0;
    static final int DeliveryMethodHeaderIdx = 1;

    static final short MaxSeqNumbers = 1024;
    static final short ReliableWindowSize = 64;
    /**
     * Maximum number of messages peer loop will receive in a single interaction.
     */
    static final short ReceiveMessagesThreshold = 128;

    /**
     * Maximum number of delivery channels (only for sequenced and ordered types).
     */
    static final int MaxChannelsPerDeliveryMethod = 32;

    static final int DefaultMaximumHandshakeAttempts = 5;
    static final long DefaultIntervalBetweenHandshakesInMillis = 1000;
    static final int DefaultReceiveBufferSize = 131071; // udp buffer size.
    static final float DefaultPingInterval = 1;
    static final float DefaultConnectionTimeout = 5;
    static final boolean DefaultIsPingUpdatedEventEnabled = false;

    static final int DefaultMaxCachedBufferCount = 512;
}
