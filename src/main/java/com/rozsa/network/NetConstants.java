package com.rozsa.network;

public class NetConstants {
    /**
     * 1 byte - message type
     * 2 bytes - sequence number
     */
    static final int MsgHeaderSize = 3;
    /**
     * 2 bytes - fragment group;
     * 2 bytes - total unfragmented message length;
     * 2 bytes - fragment chunk offset.
     */
    public static final int MsgFragHeaderSize = 6;
    static final int MsgTotalFragHeaderSize = MsgHeaderSize + MsgFragHeaderSize;

    static final int MessageTypeHeaderIdx = 0;
    static final int DeliveryMethodHeaderIdx = 1;

    static final short MaxSeqNumbers = 1024;
    static final short ReliableWindowSize = 64;

    static final int MaxFragGroups = Short.MAX_VALUE * 2;
    static final int MaxFragGroupLength = Short.MAX_VALUE * 2;
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
    static final int DefaultMTU = 508;
}
