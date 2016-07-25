/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*
*/
package org.matic.torrent.net.pwp;

import org.matic.torrent.hash.InfoHash;

/**
 * An initial (handshake) message sent between two peers
 *
 * @author Vedran Matic
 *
 */
public final class PwpHandshakeMessage extends PwpMessage {
    /*
        Reserved bytes extensions:
            Fast Peer Extension (The third least significant bit in the 8th reserved byte i.e. reserved[7] |= 0x04)
            Distributed Hash Table (Reserved Bit: The last bit in the 8th reserved byte i.e. reserved[7] |= 0x01)
            Connection Obfuscation
            Azureus Messaging Protocol (Reserved Bit: 1)
            WebSeeding
            Extension protocol (Reserved Bit: 44, the fourth most significant bit in the 6th reserved byte i.e. reserved[5] |= 0x10)
            Extension Negotiation Protocol (Reserved bits: 47 and 48)
            BitTorrent Location-aware Protocol 1.0 (Reserved Bit: 21)
            SimpleBT Extension Protocol (Reserved Bits: fist reserved byte = 0x01, following bytes may need to be set to zero)
            BitComet Extension Protocol (Reserved Bits: first two reserved bytes = "ex")

        The reserved bits are numbered 1-64 in the following table for ease of identification. Bit 1 corresponds to the most
        significant bit of the first reserved byte. Bit 8 corresponds to the least significant bit of the first reserved byte
        (i.e. byte[0] |= 0x01). Bit 64 is the least significant bit of the last reserved byte i.e. byte[7] |= 0x01
        An orange bit is a known unofficial extension, a red bit is an unknown unofficial extension.

         Bit 	Use
         1      Azureus Extended Messaging
         1-16   BitComet Extension protocol
         21     BitTorrent Location-aware Protocol 1.0
         44     Extension protocol
         47-48  Extension Negotiation Protocol
         61     NAT Traversal
         62     Fast Peers
         63     XBT Peer Exchange
         64     DHT
         64     XBT Metadata Exchange
     */

    private final byte[] reservedBytes;
    private final String peerId;

    private final InfoHash infoHash;

    public PwpHandshakeMessage(final byte[] reservedBytes, final byte[] infoHashBytes, final byte[] peerIdBytes) {
        super(MessageType.HANDSHAKE);

        this.reservedBytes = reservedBytes;
        this.peerId = PeerIdMapper.mapId(peerIdBytes);
        this.infoHash = new InfoHash(infoHashBytes);
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public String getPeerId() {
        return peerId;
    }
}