package com.rozsa.network.message;

import com.rozsa.network.Logger;


public final class OutgoingMessage {
    private byte[] data;
    private int dataIdx = 0;

    public OutgoingMessage(int size) {
        data = new byte[size];
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return dataIdx;
    }

    // watch out for GC.
    private void reallocateData(int extraLength) {
        int newSize = data.length + extraLength;
        byte[] newBuf = new byte[newSize];

        System.arraycopy(data, 0, newBuf, 0, data.length);
        data = newBuf;
    }

    // TODO: add serialization methods. writeInt; writeString etc

    public void writeString(String value) {
        byte[] valueAsBytes = value.getBytes();

        int overflowLength = dataIdx + valueAsBytes.length - data.length;
        if (overflowLength > 0) {
            reallocateData(overflowLength);
            Logger.debug("Reallocated buffer in %d bytes. Total buffer size: %d", overflowLength, data.length);
        }

        System.arraycopy(valueAsBytes, 0, data, dataIdx, valueAsBytes.length);
        dataIdx += valueAsBytes.length;
    }

}
