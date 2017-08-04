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
import java.util.Set;
import java.util.function.Consumer;

public abstract class PieceSelectionStrategy {

    protected final Map<Integer, DataPiece> downloadingPieces = new HashMap<>();
    protected final Map<Integer, DataPiece> interruptedPieces = new HashMap<>();

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

        //System.out.println("getRequestedPiece(" + pieceIndex + "): " + downloadingPieces.get(pieceIndex));

        return downloadingPieces.get(pieceIndex);
    }

    public DataPiece getInterruptedPiece(final int pieceIndex) {

        //System.out.println("getInterruptedPiece(" + pieceIndex + "): " + interruptedPieces.get(pieceIndex));

        return interruptedPieces.remove(pieceIndex);
    }

    public boolean anyPiecesNotYetRequested(final BitSet peerPieces) {
        for(int i = peerPieces.nextSetBit(0); i >= 0; i = peerPieces.nextSetBit(i + 1)) {
            if(!downloadingPieces.containsKey(i)) {
                return true;
            }
        }

        return false;
    }

    public boolean pieceRequested(final int pieceIndex, final DataPiece piece) {

        //System.out.println("pieceRequested(" + pieceIndex + ", " + piece + ")");

        return downloadingPieces.put(pieceIndex, piece) == null;
    }

    public boolean pieceFailure(final int pieceIndex) {

        //System.out.println("pieceFailure(" + pieceIndex + ")");

        receivedPieces.clear(pieceIndex);
        return downloadingPieces.remove(pieceIndex) != null;
    }

    public boolean pieceInterrupted(final int pieceIndex, final String caller) {

        //System.out.println("pieceInterrupted(" + pieceIndex + ")");

        receivedPieces.clear(pieceIndex);
        final DataPiece interruptedPiece = downloadingPieces.remove(pieceIndex);
        if(interruptedPiece != null) {
            interruptedPieces.put(pieceIndex, interruptedPiece);
            return true;
        }
        return false;
    }

    public void pieceObtained(final int pieceIndex) {

        //System.out.println("pieceObtained(" + pieceIndex + ")");

        receivedPieces.set(pieceIndex);
        downloadingPieces.remove(pieceIndex);
        interruptedPieces.remove(pieceIndex);
    }

    public void peerLost(final BitSet peerPieces, final Set<Integer> inProgressPieces) {

        //System.out.println("peerLost(" + peerPieces + ", " + inProgressPieces + ")");

        peerStateChanged(peerPieces, pieceIndex -> occurrenceDecreased(pieceIndex));
    }

    public void peerGained(final BitSet peerPieces) {

        //System.out.println("peerGained(" + peerPieces + ")");

        peerStateChanged(peerPieces, pieceIndex -> occurrenceIncreased(pieceIndex));
    }

    private void peerStateChanged(final BitSet peerPieces, final Consumer<Integer> pieceAvailabilityChangeHandler) {
        for(int i = peerPieces.nextSetBit(0); i >= 0; i = peerPieces.nextSetBit(i + 1)) {
            pieceAvailabilityChangeHandler.accept(i);
        }
    }
}