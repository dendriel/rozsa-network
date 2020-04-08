package com.rozsa.network.message;

import com.rozsa.network.Connection;
import com.rozsa.network.MessageType;

public class IncomingUserDataMessage extends IncomingMessage {
    private int dataIdx;

    public IncomingUserDataMessage(Connection connection, short seqNumber, byte[] data, int length, MessageType type, boolean isFrag) {
        super(IncomingMessageType.USER_DATA, connection, seqNumber, data, length, type, isFrag);
    }

    public int readInt() {
        int value = (0xff & data[dataIdx++]) << 24  |
                    (0xff & data[dataIdx++]) << 16  |
                    (0xff & data[dataIdx++]) << 8   |
                    (0xff & data[dataIdx++]);
        return value;
    }

    public byte readByte() {
        return data[dataIdx++];
    }

    public String readString() {
        return new String(readBytes());
    }

    public byte[] readBytes() {
        int length = (data[dataIdx++] & 0xFF) | (data[dataIdx++] << 8 & 0xFF);

        byte[] value = new byte[length];
        System.arraycopy(data, dataIdx, value, 0, length);
        dataIdx += length;

        return value;
    }
}
