package com.rozsa.network;

public final class OutgoingMessage {
    private final CachedMemory cachedMemory;
    private byte[] data;
    private int dataLength;
    private int dataIdx;

    private boolean isFrag;
    private int fragGroup;
    private int fragOffset;
    private int fragGroupLength;

    OutgoingMessage(CachedMemory cachedMemory, int dataLength) {
        this(cachedMemory, dataLength, 0, 0, 0);
    }

    OutgoingMessage(CachedMemory cachedMemory, int dataLength, int fragGroup, int fragOffset, int fragGroupLength) {
        this.cachedMemory = cachedMemory;
        this.data = cachedMemory.allocBuffer(dataLength);
        this.dataLength = dataLength;
        dataIdx = 0;

        isFrag = fragGroupLength > 0;
        this.fragGroup = fragGroup;
        this.fragOffset = fragOffset;
        this.fragGroupLength = fragGroupLength;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }

    public int getDataWritten() {
        return dataIdx;
    }

    int getFragGroup() {
        return fragGroup;
    }

    int getFragOffset() {
        return fragOffset;
    }

    int getFragGroupLength() {
        return fragGroupLength;
    }

    boolean isFrag() {
        return isFrag;
    }

    private void reallocateData(int extraLength) {
        int newLength = dataLength + extraLength;
        byte[] newBuf = cachedMemory.allocBuffer(newLength);

        System.arraycopy(data, 0, newBuf, 0, dataLength);
        cachedMemory.freeBuffer(data);
        data = newBuf;
        dataLength = newLength;
    }

    public void writeInt(int value) {
        int totalLength = 4;
        reallocateDataIfOverflow(totalLength);

        data[dataIdx++] = (byte)((value >> 24) & 0xFF);
        data[dataIdx++] = (byte)((value >> 16) & 0xFF);
        data[dataIdx++] = (byte)((value >> 8) & 0xFF);
        data[dataIdx++] = (byte)(value & 0xFF);
    }

    public void writeString(String value) {
        byte[] valueAsBytes = value.getBytes();
        writeBytes(valueAsBytes, valueAsBytes.length);
    }

    public void writeByte(byte value) {
        reallocateDataIfOverflow(1);
        data[dataIdx++] = value;
    }

    public void writeBytes(byte[] value) {
        writeBytes(value, value.length);
    }

    public void writeBytes(byte[] value, int length) {
        int totalLength = length + 2;
        reallocateDataIfOverflow(totalLength);

        data[dataIdx++] = (byte)(length & 0xFF);
        data[dataIdx++] = (byte)((length >> 8) & 0xFF);

        System.arraycopy(value, 0, data, dataIdx, length);
        dataIdx += length;
    }

    public void incrementDataIdx(int value) {
        dataIdx += value;
    }

    private void reallocateDataIfOverflow(int length) {
        int overflowLength = dataIdx + length - data.length;
        if (overflowLength > 0) {
            reallocateData(overflowLength);
            Logger.debug("Reallocated buffer in %d bytes. Total buffer size: %d", overflowLength, data.length);
        }
    }
}
