package com.rozsa.network;

class MessageSerializer {
    static byte[] serialize(MessageType type) {
        return serialize(type, DeliveryMethod.UNRELIABLE, (short)0, new byte[0], 0);
    }

    static byte[] serialize(MessageType type, DeliveryMethod method, short seqNumber) {
        return serialize(type, method, seqNumber, new byte[0], 0);
    }

    static byte[] serialize(MessageType type, DeliveryMethod method, short seqNumber, byte[] data, int dataLen) {
        int dataSize = dataLen + NetConstants.MsgHeaderSize;
        byte[] buf = new byte[dataSize];

        int bufIdx = 0;
        buf[bufIdx++] = type.getId();
        buf[bufIdx++] = method.getId();
        buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
        buf[bufIdx++] = (byte)(seqNumber & 0xFF);

        System.arraycopy(data, 0, buf, bufIdx, dataLen);

        return buf;
    }
}
