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
package org.matic.torrent.transfer.strategy;

import org.matic.torrent.io.DataPiece;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class PieceSelectionStrategy {

    protected final Map<Integer, DataPiece> downloadingPieces = new HashMap<>();
    protected final int[] pieceAvailabilities;
    protected BitSet receivedPieces;

    protected PieceSelectionStrategy(int pieceCount, final BitSet receivedPieces) {
        pieceAvailabilities = new int[pieceCount];
        this.receivedPieces = receivedPieces;
    }

    public boolean hasCompleted() {
        return receivedPieces.cardinality() == pieceAvailabilities.length;
    }

    protected void pieceRequested(final int pieceIndex, final DataPiece piece) {
        downloadingPieces.put(pieceIndex, piece);
    }

    protected Optional<DataPiece> pieceObtained(final int pieceIndex) {
        receivedPieces.set(pieceIndex);
        return Optional.ofNullable(downloadingPieces.remove(pieceIndex));
    }

    protected abstract Optional<Integer> selectNext(BitSet peerCondition);
    protected abstract void occurrenceIncreased(int pieceIndex);
    protected abstract void occurrenceDecreased(int pieceIndex);

    protected abstract boolean hasNext();
}