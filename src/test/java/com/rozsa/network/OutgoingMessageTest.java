package com.rozsa.network;

import com.rozsa.network.message.IncomingUserDataMessage;
import org.junit.jupiter.api.Test;

import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OutgoingMessageTest {
    @Test
    public void serializationTest() throws SocketException {
        NetworkPeer peer = new NetworkPeer(new PeerConfig());

        OutgoingMessage outgoingMsg = peer.createOutgoingMessage(10);
        outgoingMsg.writeByte(Byte.MIN_VALUE);
        outgoingMsg.writeInt(Integer.MIN_VALUE);
        outgoingMsg.writeInt(Integer.MAX_VALUE);
        outgoingMsg.writeByte(Byte.MAX_VALUE);

        peer.sendInternal(outgoingMsg);

        IncomingUserDataMessage incomingMsg = peer.read();
        assertNotNull(incomingMsg);
        assertEquals(Byte.MIN_VALUE, incomingMsg.readByte());
        assertEquals(Integer.MIN_VALUE, incomingMsg.readInt());
        assertEquals(Integer.MAX_VALUE, incomingMsg.readInt());
        assertEquals(Byte.MAX_VALUE, incomingMsg.readByte());
    }
}
