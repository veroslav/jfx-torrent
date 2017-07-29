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
import org.matic.torrent.client.ClientProperties;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class RarestFirstPieceSelectionStrategy extends PieceSelectionStrategy {

    protected final TreeMap<Integer, List<Integer>> rarestPieces = new TreeMap<>();
    private final int maxRarestPieces;

    public RarestFirstPieceSelectionStrategy(final int maxRarestPieces, final int pieceCount,
                                             final BitSet receivedPieces) {
        super(pieceCount, receivedPieces);
        this.maxRarestPieces = maxRarestPieces;
    }

    @Override
    public Optional<Integer> selectNext(final BitSet peerCondition) {
        final List<Integer> pieceCandidates = rarestPieces.entrySet().stream().flatMap(
                entry -> entry.getValue().stream()).filter(pieceIndex ->
                peerCondition.get(pieceIndex) && !receivedPieces.get(pieceIndex) &&
                        !downloadingPieces.containsKey(pieceIndex)).collect(Collectors.toList());

        if(pieceCandidates.isEmpty()) {
            //None of the pieces this peer has are among the rarest, simply request any piece we don't yet have
            final BitSet candidatePieces = BitSet.valueOf(peerCondition.toByteArray());
            candidatePieces.andNot(receivedPieces);

            //Check which of the candidate peers has the lowest availability
            int lowestAvailability = Integer.MAX_VALUE;
            int rarestPieceIndex = -1;

            for(int i = candidatePieces.nextSetBit(0); i != -1; i = candidatePieces.nextSetBit(i + 1)) {
                if(super.pieceAvailabilities[i] < lowestAvailability && !downloadingPieces.containsKey(i)
                        && !receivedPieces.get(i)) {
                    rarestPieceIndex = i;
                    lowestAvailability = super.pieceAvailabilities[i];
                }
            }

            //System.out.println("Rarest pieces: " + rarestPieces + " , piece to request = " + rarestPieceIndex);

            return rarestPieceIndex != -1? Optional.of(rarestPieceIndex) : Optional.empty();
        }
        else {

            //System.out.println("Rarest pieces: " + rarestPieces + " , download candidates =  " + pieceCandidates);

            return Optional.of(pieceCandidates.get(
                    ClientProperties.RANDOM_INSTANCE.nextInt(pieceCandidates.size())));
        }
    }

    @Override
    public void occurrenceIncreased(final int pieceIndex) {
        final int oldPieceAvailability = pieceAvailabilities[pieceIndex]++;

        //If we already have this piece, don't do anything
        if(super.receivedPieces.get(pieceIndex)) {
            return;
        }

        final Map.Entry<Integer, List<Integer>> lastRarestEntry = rarestPieces.lastEntry();
        final List<Integer> oldAvailabilityEntries = rarestPieces.get(oldPieceAvailability);
        final int rarestPiecesCount = getRarestPiecesCount();

        //Check whether the piece was among the rarest pieces
        if((oldAvailabilityEntries != null && oldAvailabilityEntries.contains(pieceIndex))) {
            oldAvailabilityEntries.remove((Integer)pieceIndex);
            if(oldAvailabilityEntries.isEmpty()) {
                rarestPieces.remove(oldPieceAvailability);
            }

            if(pieceAvailabilities[pieceIndex] > lastRarestEntry.getKey() && rarestPiecesCount == maxRarestPieces) {
                //Check whether we can replace the piece with the one of lower availability
                final OptionalInt lowerPrioPiece = findAvailabilityPiece(
                        (index, avail) -> avail < pieceAvailabilities[pieceIndex] && (!rarestPieces.containsKey(avail)
                                || !rarestPieces.get(avail).contains(index)));

                if (lowerPrioPiece.isPresent()) {
                    final int lowerPrioPieceIndex = lowerPrioPiece.getAsInt();
                    final int pieceAvail = pieceAvailabilities[lowerPrioPieceIndex];

                    rarestPieces.computeIfAbsent(pieceAvail, value -> new ArrayList<>()).add(lowerPrioPieceIndex);
                }
            }
            else {
                //The piece is still among the rarest ones, move it to the correct position
                rarestPieces.computeIfAbsent(pieceAvailabilities[pieceIndex], value -> new ArrayList<>()).add(pieceIndex);
            }
        }
        else {
            //This piece just became available, prioritize it amongst the rarest pieces
            if (rarestPiecesCount == maxRarestPieces) {
                final List<Integer> pieceIndexes = lastRarestEntry.getValue();
                pieceIndexes.remove(pieceIndexes.size() - 1);
                if(pieceIndexes.isEmpty()) {
                    rarestPieces.remove(lastRarestEntry.getKey());
                }
            }
            //Add it to the rarestPieces
            rarestPieces.computeIfAbsent(pieceAvailabilities[pieceIndex], value -> new ArrayList<>()).add(pieceIndex);
        }
    }

    @Override
    public void occurrenceDecreased(final int pieceIndex) {
        final int oldAvailability = pieceAvailabilities[pieceIndex]--;

        //If we already have this piece, don't do anything
        if(super.receivedPieces.get(pieceIndex)) {
            return;
        }

        final List<Integer> oldAvailabilityEntries = rarestPieces.get(oldAvailability);

        //Check whether the piece was among the rarest pieces
        if((oldAvailabilityEntries != null && oldAvailabilityEntries.contains(pieceIndex))) {
            oldAvailabilityEntries.remove((Integer)pieceIndex);
            if(oldAvailabilityEntries.isEmpty()) {
                rarestPieces.remove(oldAvailability);
            }

            //If the piece has become unavailable, add another candidate to rarest pieces
            if(oldAvailability == 1) {
                updateRarestPieces((index, avail) -> index == pieceIndex || (rarestPieces.containsKey(avail)
                        && rarestPieces.get(avail).contains(index)));
            }
            //The piece is still among the rarest ones, move it to the correct position
            else {
                rarestPieces.computeIfAbsent(pieceAvailabilities[pieceIndex], value -> new ArrayList<>()).add(pieceIndex);
            }
        }
        //Check whether the piece has become one of the rarest pieces
        else if(pieceAvailabilities[pieceIndex] != 0) {
            final Map.Entry<Integer, List<Integer>> lastRarestEntry = rarestPieces.lastEntry();

            final List<Integer> lastRarestEntryPieceIndexes = lastRarestEntry != null?
                    lastRarestEntry.getValue() : Collections.emptyList();
            final int lastRarestEntryAvailability = lastRarestEntry.getKey();

            if (pieceAvailabilities[pieceIndex] < lastRarestEntryAvailability) {
                //This piece become one of the rarest pieces, replace one of the highest availability pieces there
                lastRarestEntryPieceIndexes.remove(0);

                if (lastRarestEntryPieceIndexes.isEmpty()) {
                    rarestPieces.remove(lastRarestEntry.getKey());
                }

                rarestPieces.computeIfAbsent(pieceAvailabilities[pieceIndex], value -> new ArrayList<>()).add(pieceIndex);
            }
        }
    }

    @Override
    public boolean pieceRequested(final int pieceIndex, final DataPiece piece) {
        final boolean wasRequested = super.pieceRequested(pieceIndex, piece);

        if(wasRequested && removeFromRarestPieces(pieceIndex)) {
            updateRarestPieces((index, avail) -> index == pieceIndex);
        }

        return wasRequested;
    }

    @Override
    public void pieceObtained(final int pieceIndex) {
        super.pieceObtained(pieceIndex);

        if(removeFromRarestPieces(pieceIndex)) {
            updateRarestPieces((index, avail) -> index == pieceIndex);
        }
    }

    @Override
    public boolean pieceFailure(final int pieceIndex) {
        final boolean failureHandled = super.pieceFailure(pieceIndex);

        if(failureHandled) {
            updateRarestPieces((index, avail) -> false);
        }

        return failureHandled;
    }

    @Override
    public void peerLost(final BitSet peerPieces) {
        super.peerLost(peerPieces);
    }

    protected int getRarestPiecesCount() {
        return (int)rarestPieces.values().stream().flatMap(List::stream).count();
    }

    private boolean removeFromRarestPieces(final int pieceIndex) {
        if(!rarestPieces.containsKey(pieceAvailabilities[pieceIndex])) {
            return false;
        }
        final List<Integer> sameAvailabilityPieces = rarestPieces.get(pieceAvailabilities[pieceIndex]);

        final boolean removed = sameAvailabilityPieces.remove((Integer)pieceIndex);
        if(sameAvailabilityPieces.isEmpty()) {
            rarestPieces.remove(pieceAvailabilities[pieceIndex]);
        }
        return removed;
    }

    private OptionalInt findAvailabilityPiece(final BiFunction<Integer, Integer, Boolean> condition) {
        for(int i = 0; i < pieceAvailabilities.length; ++i) {
            if(condition.apply(i, pieceAvailabilities[i])) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private void updateRarestPieces(final BiFunction<Integer, Integer, Boolean> skipPieceCondition) {
        final int rarestPiecesCount = getRarestPiecesCount();
        if(maxRarestPieces == rarestPiecesCount) {
            return;
        }
        final int targetPiecesCount = maxRarestPieces - rarestPiecesCount;
        final TreeMap<Integer, List<Integer>> result = new TreeMap<>();

        for(int i = 0; i < super.pieceAvailabilities.length; ++i) {
            //Skip this piece if we have it or if it is among the rarest pieces already
            final int availability = pieceAvailabilities[i];
            final boolean isAmongRarestPieces = rarestPieces.containsKey(availability)
                    && rarestPieces.get(availability).contains(i);
            if(availability == 0 || super.receivedPieces.get(i) || isAmongRarestPieces
                    || super.downloadingPieces.containsKey(i) || skipPieceCondition.apply(i, availability)) {
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
                entry.getKey(), value -> new ArrayList<>()).addAll(entry.getValue()));
    }
}