/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.utils.UnitConverter;

import java.util.Arrays;
import java.util.BitSet;

public final class PwpMessageFactoryTest {

    private final int pieceCount = 821;
    private final BitSet pieces = new BitSet(pieceCount);

    @Before
    public void setup() {
        pieces.clear();
    }

    @Test
    public void testCreateAndParseBitfieldMessage() {
        for(int i = 0; i < pieceCount; i += 2) {
            pieces.set(i);
        }
        final PwpMessage builtBitfieldMessage = PwpMessageFactory.buildBitfieldMessage(pieces, pieceCount);

        Assert.assertEquals(PwpMessage.MessageType.BITFIELD, builtBitfieldMessage.getMessageType());

        final byte[] messagePayload = UnitConverter.reverseBits(builtBitfieldMessage.getPayload());
        final byte[] builtBitfieldBytes = Arrays.copyOfRange(messagePayload, 5, messagePayload.length);

        final BitSet builtPiecesBitSet = BitSet.valueOf(builtBitfieldBytes);
        Assert.assertEquals(pieces, builtPiecesBitSet);
    }
}