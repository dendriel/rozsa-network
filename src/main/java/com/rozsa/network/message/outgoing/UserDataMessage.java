package com.rozsa.network.message.outgoing;

public class UserDataMessage extends OutgoingMessage {
    public UserDataMessage(int size) {
        super(MessageType.USER_DATA, size);
    }

    public void writeString(String value) {
        byte[] valueAsBytes = value.getBytes();
        if (dataIdx + valueAsBytes.length > value.length()) {
            throw new ArrayIndexOutOfBoundsException(String.format("There is no enough buffer remaining to write %s", data));
        }

        System.arraycopy(valueAsBytes, 0, data, dataIdx, valueAsBytes.length);
        dataIdx += valueAsBytes.length;
    }
}
