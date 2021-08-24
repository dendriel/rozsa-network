package com.rozsa.network.message;

import com.rozsa.network.Connection;
import com.rozsa.network.MessageType;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class IncomingUserDataMessage extends IncomingMessage {
    private int dataIdx;

    public IncomingUserDataMessage(Connection connection, short seqNumber, byte[] data, int length, MessageType type, boolean isFrag) {
        super(IncomingMessageType.USER_DATA, connection, seqNumber, data, length, type, isFrag);
    }

    public int getDataRead() {
        return dataIdx;
    }

    public BitSet readBitSet() {
        int length = (data[dataIdx++] & 0xFF) | (data[dataIdx++] << 8 & 0xFF);
        byte[] bytes = new byte[length];

        System.arraycopy(data, dataIdx, bytes, 0, length);
        dataIdx += length;

        BitSet value = BitSet.valueOf(bytes);
        return value;
    }

    public boolean readBoolean() {
        boolean value = data[dataIdx++] > 0;
        return value;
    }

    public float readFloat() {
        byte[] bits = new byte[] {
                (byte)(0xFF & data[dataIdx++]),
                (byte)(0xFF & data[dataIdx++]),
                (byte)(0xFF & data[dataIdx++]),
                (byte)(0xFF & data[dataIdx++]),
        };

        float value = ByteBuffer.wrap(bits).getFloat();
        return value;
    }

    public long readLong() {
        final int totalLength = 8;
        long value = 0;
        for (int i = 0; i < totalLength; i++) {
            value <<= Long.BYTES;
            value |= (data[dataIdx+i] & 0xFF);
        }
        dataIdx += totalLength;
        return value;
    }

    public short readShort() {
        short value = (short)((0xFF & data[dataIdx++]) << 8   |
                        (0xFF & data[dataIdx++]));
        return value;
    }

    public int readInt() {
        int value = (0xFF & data[dataIdx++]) << 24  |
                    (0xFF & data[dataIdx++]) << 16  |
                    (0xFF & data[dataIdx++]) << 8   |
                    (0xFF & data[dataIdx++]);
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
