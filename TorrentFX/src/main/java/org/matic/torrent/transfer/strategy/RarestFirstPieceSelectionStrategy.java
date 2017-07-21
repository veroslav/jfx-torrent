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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.Predicate;

public class RarestFirstPieceSelectionStrategy extends PieceSelectionStrategy {

    private final TreeMap<Integer, List<Integer>> rarestPieces = new TreeMap<>();
    private final int maxRarestPieces;

    public RarestFirstPieceSelectionStrategy(final int maxRarestPieces, final int pieceLength,
                                             final BitSet receivedPieces) {
        super(pieceLength, receivedPieces);

        this.maxRarestPieces = maxRarestPieces;
    }

    @Override
    public boolean hasNext() {
        return getRarestPiecesCount() > 0;
    }

    @Override
    public Optional<Integer> selectNext(BitSet peerCondition) {
        if(rarestPieces.isEmpty()) {
            return Optional.empty();
        }
        else {
            //TODO: Implement
            //rarestPieces.keySet().stream()
            return Optional.empty();
        }
    }

    @Override
    public void occurrenceIncreased(final int pieceIndex) {
        //Check if the piece just become available
        final int oldPieceAvailability = pieceAvailabilities[pieceIndex];
        if(oldPieceAvailability + 1 == 1 && !receivedPieces.get(pieceIndex)) {
            //Add it straight to the rarestPieces
            rarestPieces.computeIfAbsent(oldPieceAvailability + 1, value -> new ArrayList()).add(pieceIndex);
            if(getRarestPiecesCount() > maxRarestPieces) {
                rarestPieces.lastEntry().getValue().remove(0);
            }
        } else {
            //Check whether this piece should be removed from rarestPieces, if there
            final List<Integer> matchingPieces = rarestPieces.computeIfAbsent(
                    oldPieceAvailability, value -> new ArrayList());
            if(matchingPieces.contains(pieceIndex)) {
                final int highestRarePiece = rarestPieces.lastKey();
                if(oldPieceAvailability + 1 > highestRarePiece) {

                    final OptionalInt lowerPrioPiece = findAvailabilityPiece(avail -> avail < oldPieceAvailability + 1);

                    if(lowerPrioPiece.isPresent()) {
                        final int lowerPrioPieceIndex = lowerPrioPiece.getAsInt();
                        final int pieceAvail = pieceAvailabilities[lowerPrioPieceIndex];

                        matchingPieces.remove(pieceIndex);
                        rarestPieces.computeIfAbsent(pieceAvail, value -> new ArrayList()).add(lowerPrioPieceIndex);
                    }
                }
            }
        }
        ++pieceAvailabilities[pieceIndex];
    }

    @Override
    public void occurrenceDecreased(final int pieceIndex) {
        final int oldAvailability = super.pieceAvailabilities[pieceIndex];
        if(oldAvailability == 1) {
            rarestPieces.get(oldAvailability).remove(pieceIndex);

            //Add another candidate to rarestPieces
            updateRarestPieces();
        } else {
            //Check whether this piece is a candidate for rarestPieces, if not already

            if(!rarestPieces.containsKey(oldAvailability)) {
                final Map.Entry<Integer, List<Integer>> lastRarestEntry = rarestPieces.lastEntry();
                if(lastRarestEntry.getKey() > oldAvailability - 1) {
                    rarestPieces.get(lastRarestEntry.getKey()).remove(lastRarestEntry.getValue());
                    rarestPieces.computeIfAbsent(oldAvailability - 1, value -> new ArrayList()).add(pieceIndex);
                }
            }
        }
        --super.pieceAvailabilities[pieceIndex];
    }

    @Override
    public void pieceRequested(final int pieceIndex, final DataPiece piece) {
        super.pieceRequested(pieceIndex, piece);

        //TODO: Update rarestPieces
    }

    @Override
    public Optional<DataPiece> pieceObtained(final int pieceIndex) {
        final Optional<DataPiece> dataPiece = super.pieceObtained(pieceIndex);

        if(!dataPiece.isPresent()) {
            return Optional.empty();
        }

        //TODO: Check whether rarestPieces contains the piece before getting it!
        if(rarestPieces.get(pieceAvailabilities[pieceIndex]).remove(dataPiece.get().getIndex()) != null) {
            updateRarestPieces();
        }

        return dataPiece;
    }

    private int getRarestPiecesCount() {
        return (int)rarestPieces.values().stream().flatMap(List::stream).count();
    }

    private OptionalInt findAvailabilityPiece(final Predicate<Integer> condition) {
        return Arrays.stream(pieceAvailabilities).filter(avail -> condition.test(avail)).findFirst();
    }

    private void updateRarestPieces() {
        final int targetPiecesCount = maxRarestPieces - getRarestPiecesCount();
        final TreeMap<Integer, List<Integer>> result = new TreeMap<>();

        for(int i = 0; i < super.pieceAvailabilities.length; ++i) {
            //Skip this piece if we already have it
            if(super.receivedPieces.get(i)) {
                continue;
            }
            if(result.size() < targetPiecesCount) {
                result.computeIfAbsent(super.pieceAvailabilities[i], value -> new ArrayList<>()).add(i);
            }
            else {
                final int highestFoundAvailability = result.lastKey();
                if(super.pieceAvailabilities[i] < highestFoundAvailability) {
                    result.remove(highestFoundAvailability);
                    result.computeIfAbsent(super.pieceAvailabilities[i], value -> new ArrayList<>()).add(i);
                }
            }
        }

        result.entrySet().forEach(entry -> rarestPieces.computeIfAbsent(
                entry.getKey(), value -> new ArrayList()).addAll(entry.getValue()));
    }
}