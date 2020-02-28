package com.rozsa.network.channel;

import com.rozsa.network.Address;
import com.rozsa.network.PacketSender;

public class UnreliableSenderChannel extends SenderChannel {
    public UnreliableSenderChannel(Address addr, PacketSender sender) {
        super(DeliveryMethod.UNRELIABLE, addr, sender);
    }
}
