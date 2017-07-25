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
import java.util.function.Consumer;

public abstract class PieceSelectionStrategy {

    protected final Map<Integer, DataPiece> downloadingPieces = new HashMap<>();
    protected final int[] pieceAvailabilities;
    protected BitSet receivedPieces;

    protected PieceSelectionStrategy(final int pieceCount, final BitSet receivedPieces) {
        pieceAvailabilities = new int[pieceCount];
        this.receivedPieces = receivedPieces;
    }

    public abstract Optional<Integer> selectNext(BitSet peerCondition);
    public abstract void occurrenceIncreased(int pieceIndex);
    public abstract void occurrenceDecreased(int pieceIndex);

    public DataPiece getRequestedPiece(final int pieceIndex) {
        return downloadingPieces.get(pieceIndex);
    }

    public boolean anyRequested(final BitSet peerPieces) {
        return downloadingPieces.keySet().stream().filter(
                pieceIndex -> peerPieces.get(pieceIndex)).findAny().isPresent();
    }

    public boolean pieceRequested(final int pieceIndex, final DataPiece piece) {
        return downloadingPieces.put(pieceIndex, piece) == null;
    }

    public boolean pieceFailure(final int pieceIndex) {
        receivedPieces.clear(pieceIndex);
        return downloadingPieces.remove(pieceIndex) != null;
    }

    public void pieceObtained(final int pieceIndex) {
        receivedPieces.set(pieceIndex);
        downloadingPieces.remove(pieceIndex);
    }

    public void peerLost(final BitSet peerPieces) {
        peerStateChanged(peerPieces, pieceIndex -> occurrenceDecreased(pieceIndex));
    }

    public void peerGained(final BitSet peerPieces) {
        peerStateChanged(peerPieces, pieceIndex -> occurrenceIncreased(pieceIndex));
    }

    private void peerStateChanged(final BitSet peerPieces, final Consumer<Integer> pieceAvailabilityChangeHandler) {
        for(int i = peerPieces.nextSetBit(0); i >= 0; i = peerPieces.nextSetBit(i + 1)) {
            pieceAvailabilityChangeHandler.accept(i);
        }
    }
}