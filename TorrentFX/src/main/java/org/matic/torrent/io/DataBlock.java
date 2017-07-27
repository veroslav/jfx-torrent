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
package org.matic.torrent.io;

import java.util.Objects;

/**
 * A block of data that is part of a larger data piece. It is used both
 * when sending data to and receiving data from remote peers.
 *
 * @author Vedran Matic
 */
public final class DataBlock {

    private final int pieceIndex;
    private final int pieceOffset;
    private final byte[] blockData;

    public DataBlock(final byte[] blockData, final int pieceIndex, final int pieceOffset) {
        this.blockData = blockData;
        this.pieceIndex = pieceIndex;
        this.pieceOffset = pieceOffset;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public int getPieceOffset() {
        return pieceOffset;
    }

    public byte[] getBlockData() {
        return blockData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataBlock dataBlock = (DataBlock) o;
        return pieceIndex == dataBlock.pieceIndex &&
                pieceOffset == dataBlock.pieceOffset &&
                blockData.length == dataBlock.blockData.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceIndex, pieceOffset, blockData.length);
    }

    @Override
    public String toString() {
        return "DataBlock{" +
                "pieceIndex=" + pieceIndex +
                ", pieceOffset=" + pieceOffset +
                ", length=" + blockData.length +
                '}';
    }
}