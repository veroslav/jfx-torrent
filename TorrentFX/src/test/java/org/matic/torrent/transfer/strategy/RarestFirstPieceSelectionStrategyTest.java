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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public final class RarestFirstPieceSelectionStrategyTest {

    private final int maxRarestPieces = 5;
    private final int pieceCount = 10;

    private RarestFirstPieceSelectionStrategy unitUnderTest;
    private BitSet receivedPieces;

    @Before
    public void setup() {
        receivedPieces = new BitSet(pieceCount);
        unitUnderTest = new RarestFirstPieceSelectionStrategy(maxRarestPieces, pieceCount, receivedPieces);
    }

    @Test
    public void testOccurrenceIncreasedPieceNotDownloadedAndWasUnavailableAndRarestPiecesLimitNotReached() {
        final int targetPieceIndex = 2;

        final List<Integer> oneOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(0, 1));
        final List<Integer> twoOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(3, 4));

        unitUnderTest.rarestPieces.put(1, oneOccurrencePieceIndexes);
        unitUnderTest.rarestPieces.put(2, twoOccurrencePieceIndexes);

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[3] = 2;
        unitUnderTest.pieceAvailabilities[4] = 2;

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(4, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        Assert.assertEquals(3, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(2, twoOccurrencePieceIndexes.size());
    }

    @Test
    public void testOccurrenceIncreasedPieceNotDownloadedAndWasUnavailableAndRarestPiecesIsEmpty() {
        final int targetPieceIndex = 2;

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());

        final List<Integer> oneOccurrencePieceIndexes = unitUnderTest.rarestPieces.get(1);
        Assert.assertEquals(1, oneOccurrencePieceIndexes.size());
        Assert.assertTrue(oneOccurrencePieceIndexes.contains(targetPieceIndex));
    }

    @Test
    public void testOccurrenceIncreasedPieceNotDownloadedAndWasAvailableAndRarestPiecesLimitNotReached() {
        final int targetPieceIndex = 1;

        final List<Integer> oneOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(0, 1));
        final List<Integer> twoOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(2, 3));

        unitUnderTest.rarestPieces.put(1, oneOccurrencePieceIndexes);
        unitUnderTest.rarestPieces.put(2, twoOccurrencePieceIndexes);

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[2] = 2;
        unitUnderTest.pieceAvailabilities[3] = 2;

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(4, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(4, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(1, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(3, twoOccurrencePieceIndexes.size());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(3, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(4, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(1, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(2, twoOccurrencePieceIndexes.size());

        final List<Integer> threeOccurrencePieceIndexes = unitUnderTest.rarestPieces.get(3);
        Assert.assertEquals(1, threeOccurrencePieceIndexes.size());
        Assert.assertTrue(threeOccurrencePieceIndexes.contains(targetPieceIndex));
    }

    @Test
    public void testOccurrenceIncreasedPieceNotDownloadedAndWasAvailableAndRarestPiecesIsEmpty() {

    }

    @Test
    public void testOccurrenceIncreasedPieceNotDownloadedAndWasUnavailableAndRarestPiecesIsFull() {
        final int targetPieceIndex = 9;

        final List<Integer> oneOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(0, 1, 2));
        final List<Integer> twoOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(3, 4));

        unitUnderTest.rarestPieces.put(1, oneOccurrencePieceIndexes);
        unitUnderTest.rarestPieces.put(2, twoOccurrencePieceIndexes);

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[2] = 1;
        unitUnderTest.pieceAvailabilities[3] = 2;
        unitUnderTest.pieceAvailabilities[4] = 2;

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(4, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(1, twoOccurrencePieceIndexes.size());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(3, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(2, twoOccurrencePieceIndexes.size());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(3, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(3, oneOccurrencePieceIndexes.size());
        Assert.assertEquals(2, twoOccurrencePieceIndexes.size());
    }

    @Test
    public void testOccurrenceIncreasedPieceDownloadedAndWasUnavailable() {
        final int targetPieceIndex = 8;
        receivedPieces.set(8);

        unitUnderTest = new RarestFirstPieceSelectionStrategy(maxRarestPieces, pieceCount, receivedPieces);

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceIncreased(targetPieceIndex);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testOccurrenceDecreasedPieceNotDownloadedAndIsUnavailableAndRarestPiecesLimitNotReached() {
        final int targetPieceIndex = 6;

        final List<Integer> oneOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(0, 6));

        unitUnderTest.rarestPieces.put(1, oneOccurrencePieceIndexes);

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[6] = 1;

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(2, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(1, oneOccurrencePieceIndexes.size());
    }

    @Test
    public void testOccurrenceDecreasedPieceNotDownloadedAndIsUnavailableAndRarestPiecesIsEmpty() {
        final int targetPieceIndex = 6;

        final List<Integer> oneOccurrencePieceIndexes = new ArrayList<>(Arrays.asList(6));

        unitUnderTest.rarestPieces.put(1, oneOccurrencePieceIndexes);
        unitUnderTest.pieceAvailabilities[6] = 1;

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(0, oneOccurrencePieceIndexes.size());
    }

    @Test
    public void testOccurrenceDecreasedPieceNotDownloadedAndIsUnavailableAndRarestPiecesIsFull() {
        final int targetPieceIndex = 3;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0, 1, 2)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(3, 4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[2] = 1;
        unitUnderTest.pieceAvailabilities[3] = 2;
        unitUnderTest.pieceAvailabilities[4] = 2;
        unitUnderTest.pieceAvailabilities[5] = 3;

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(4, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(3, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());

        final List<Integer> threeOccurrencePieces = unitUnderTest.rarestPieces.get(3);
        Assert.assertEquals(1, threeOccurrencePieces.size());
        Assert.assertTrue(threeOccurrencePieces.contains(5));
    }

    @Test
    public void testOccurrenceDecreasedPieceDownloadedAndIsUnavailableAndRarestPiecesIsFull() {
        final int targetPieceIndex = 5;
        receivedPieces.set(targetPieceIndex);

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0, 1, 2)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(3, 4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[2] = 1;
        unitUnderTest.pieceAvailabilities[3] = 2;
        unitUnderTest.pieceAvailabilities[4] = 2;
        unitUnderTest.pieceAvailabilities[5] = 1;

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(3, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(2, unitUnderTest.rarestPieces.get(2).size());
    }

    @Test
    public void testOccurrenceDecreasedPieceNotDownloadedAndIsAvailableAndRarestPiecesIsFull() {
       final int targetPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0, 1, 2)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(3, 4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 1;
        unitUnderTest.pieceAvailabilities[2] = 1;
        unitUnderTest.pieceAvailabilities[3] = 2;
        unitUnderTest.pieceAvailabilities[4] = 2;
        unitUnderTest.pieceAvailabilities[5] = 3;

        Assert.assertEquals(3, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(3, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(2, unitUnderTest.rarestPieces.get(2).size());

        unitUnderTest.occurrenceDecreased(targetPieceIndex);

        final List<Integer> oneOccurrencePieces = unitUnderTest.rarestPieces.get(1);
        final List<Integer> twoOccurrencePieces = unitUnderTest.rarestPieces.get(2);

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        Assert.assertEquals(4, oneOccurrencePieces.size());
        Assert.assertEquals(1, twoOccurrencePieces.size());

        Assert.assertTrue(oneOccurrencePieces.contains(5));
    }

    @Test
    public void testPieceObtainedAndWasAmongRarestPieces() {

    }

    @Test
    public void testPieceObtainedAndWasNotAmongRarestPieces() {

    }

    @Test
    public void testPieceRequestedAndWasAmongRarestPieces() {

    }

    @Test
    public void testPieceRequestedAndWasNotAmongRarestPieces() {

    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPieces() {

    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesThatMatch() {

    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesThatMatchOneOtherMatching() {

    }

    @Test
    public void testThereAreRequestedPiecesThatThePeerHas() {

    }

    @Test
    public void testThereAreNoRequestedPiecesThatThePeerHas() {

    }

    @Test
    public void testPeerLost() {

    }

    @Test
    public void testPeerGained() {

    }

    @Test
    public void testPieceRequested() {

    }

    @Test
    public void testPieceFailureAndWasRequested() {

    }

    @Test
    public void testPieceFailureAndWasNotRequested() {

    }
}