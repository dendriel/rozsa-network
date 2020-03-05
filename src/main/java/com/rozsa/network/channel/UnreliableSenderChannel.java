package com.rozsa.network.channel;

import com.rozsa.network.Address;
import com.rozsa.network.PacketSender;

class UnreliableSenderChannel extends SenderChannel {
    UnreliableSenderChannel(Address addr, PacketSender sender) {
        super(DeliveryMethod.UNRELIABLE, addr, sender);
    }
}
