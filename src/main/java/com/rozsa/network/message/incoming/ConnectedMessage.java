package com.rozsa.network.message.incoming;

import com.rozsa.network.Address;
import com.rozsa.network.IncomingMessage;

public class ConnectedMessage extends IncomingMessage {
    public ConnectedMessage(Address address, byte[] data, int dataLen) {
        super(IncomingMessageType.CONNECTED, address, data, dataLen);
    }
}
