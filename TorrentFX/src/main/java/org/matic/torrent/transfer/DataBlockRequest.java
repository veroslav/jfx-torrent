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
package org.matic.torrent.transfer;

public final class DataBlockRequest {

    private final int pieceIndex;
    private final int pieceOffset;
    private final int blockLength;

    private final long creationTime;

    public DataBlockRequest(final int pieceIndex, final int pieceOffset, final int blockLength) {
        this.pieceIndex = pieceIndex;
        this.pieceOffset = pieceOffset;
        this.blockLength = blockLength;

        this.creationTime = System.currentTimeMillis();
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public int getPieceOffset() {
        return pieceOffset;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public long getCreationTime() {
        return creationTime;
    }
}