package com.rozsa.network;

public final class OutgoingMessage {
    private final CachedMemory cachedMemory;
    private byte[] data;
    private int dataLength;
    private int dataIdx;

    OutgoingMessage(CachedMemory cachedMemory, int dataLength) {
        this.cachedMemory = cachedMemory;
        this.data = cachedMemory.allocBuffer(dataLength);
        this.dataLength = dataLength;
        dataIdx = 0;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataWritten() {
        return dataIdx;
    }

    private void reallocateData(int extraLength) {
        int newLength = dataLength + extraLength;
        byte[] newBuf = cachedMemory.allocBuffer(newLength);

        System.arraycopy(data, 0, newBuf, 0, dataLength);
        cachedMemory.freeBuffer(data);
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
