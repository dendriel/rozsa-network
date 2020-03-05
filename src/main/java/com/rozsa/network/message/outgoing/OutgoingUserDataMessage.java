package com.rozsa.network.message.outgoing;

import static com.rozsa.network.channel.DeliveryMethod.UNRELIABLE;

public class OutgoingUserDataMessage extends OutgoingMessage {
    public OutgoingUserDataMessage(int size) {
        super(MessageType.USER_DATA, UNRELIABLE, size, (short)0);
    }

    public void writeString(String value) {
        byte[] valueAsBytes = value.getBytes();
        if (dataIdx + valueAsBytes.length > data.length) {
            throw new ArrayIndexOutOfBoundsException(String.format("There is no enough buffer remaining to write %s", data));
        }

        System.arraycopy(valueAsBytes, 0, data, dataIdx, valueAsBytes.length);
        dataIdx += valueAsBytes.length;
    }
}
