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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.io.DataPiece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

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
    public void testPieceObtainedAndWasAmongRarestPiecesAndLimitNotReached() {
        final int targetPieceIndex = 4;

        unitUnderTest.downloadingPieces.put(4, EasyMock.createMock(DataPiece.class));
        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[4] = 2;

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(2, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.pieceObtained(targetPieceIndex);

        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(2));
        Assert.assertTrue(unitUnderTest.downloadingPieces.isEmpty());
        Assert.assertTrue(unitUnderTest.receivedPieces.get(targetPieceIndex));
    }

    @Test
    public void testPieceObtainedAndWasNotAmongRarestPieces() {
        final int targetPieceIndex = 5;

        unitUnderTest.downloadingPieces.put(targetPieceIndex, EasyMock.createMock(DataPiece.class));
        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.pieceObtained(targetPieceIndex);

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());
        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertTrue(unitUnderTest.downloadingPieces.isEmpty());
        Assert.assertTrue(unitUnderTest.receivedPieces.get(targetPieceIndex));
    }

    @Test
    public void testPieceRequestedAndWasAmongstRarestPieces() {
        final int targetPieceIndex = 0;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final boolean wasRequested = unitUnderTest.pieceRequested(
                targetPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertTrue(wasRequested);
        Assert.assertTrue(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(1));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(6).size());

        Assert.assertFalse(unitUnderTest.receivedPieces.get(targetPieceIndex));
    }

    @Test
    public void testPieceRequestedAndWasNotAmongstRarestPieces() {
        final int targetPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final boolean wasRequested = unitUnderTest.pieceRequested(
                targetPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertTrue(wasRequested);
        Assert.assertTrue(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());

        Assert.assertFalse(unitUnderTest.receivedPieces.get(targetPieceIndex));
    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesAndNoOtherMatchingPieces() {
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());

        final Optional<Integer> result = unitUnderTest.selectNext(new BitSet());

        Assert.assertFalse(result.isPresent());
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());
        Assert.assertTrue(unitUnderTest.downloadingPieces.isEmpty());
    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesThatMatchAndNoOtherPieces() {
        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;

        Assert.assertEquals(3, unitUnderTest.getRarestPiecesCount());

        final Optional<Integer> result = unitUnderTest.selectNext(new BitSet());

        Assert.assertFalse(result.isPresent());
        Assert.assertTrue(unitUnderTest.downloadingPieces.isEmpty());

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(3, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesThatMatchOneOtherMatching() {
        final int targetPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final BitSet peerPieces = new BitSet();
        peerPieces.set(targetPieceIndex);

        final Optional<Integer> result = unitUnderTest.selectNext(peerPieces);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(targetPieceIndex, result.get().intValue());
        Assert.assertFalse(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());

        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testSelectNextPieceAndThereAreRarestPiecesThatMatchOneOtherMatching() {
        final int targetPieceIndex = 4;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        Assert.assertEquals(5, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final BitSet peerPieces = new BitSet();
        peerPieces.set(targetPieceIndex);
        peerPieces.set(5);

        final Optional<Integer> result = unitUnderTest.selectNext(peerPieces);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(targetPieceIndex, result.get().intValue());
        Assert.assertFalse(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());

        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testSelectNextPieceAndThereAreNoRarestPiecesThatMatchOneOtherMatchingAndWeHaveThatPiece() {
        final int targetPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        unitUnderTest.receivedPieces.set(targetPieceIndex);

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final BitSet peerPieces = new BitSet();
        peerPieces.set(targetPieceIndex);

        final Optional<Integer> result = unitUnderTest.selectNext(peerPieces);

        Assert.assertFalse(result.isPresent());
        Assert.assertFalse(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());

        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testSelectNextPieceAndThePieceIsAlreadyDownloading() {
        final int targetPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(2)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(4)));

        unitUnderTest.pieceAvailabilities[0] = 1;
        unitUnderTest.pieceAvailabilities[1] = 2;
        unitUnderTest.pieceAvailabilities[2] = 3;
        unitUnderTest.pieceAvailabilities[3] = 4;
        unitUnderTest.pieceAvailabilities[4] = 5;
        unitUnderTest.pieceAvailabilities[5] = 6;

        unitUnderTest.downloadingPieces.put(targetPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[targetPieceIndex]);
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        final BitSet peerPieces = new BitSet();
        peerPieces.set(targetPieceIndex);

        final Optional<Integer> result = unitUnderTest.selectNext(peerPieces);

        Assert.assertFalse(result.isPresent());
        Assert.assertTrue(unitUnderTest.downloadingPieces.containsKey(targetPieceIndex));

        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(1).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(2).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(3).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(4).size());
        Assert.assertEquals(1, unitUnderTest.rarestPieces.get(5).size());

        Assert.assertFalse(unitUnderTest.rarestPieces.containsKey(6));
        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testPeerLost() {
        final int downloadingOnlyPieceIndex = 5;
        final int downloadingMoreAvailablePieceIndex = 6;
        final int rarestOnlyPieceIndex = 7;

        unitUnderTest.downloadingPieces.put(downloadingOnlyPieceIndex, EasyMock.createMock(DataPiece.class));
        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(rarestOnlyPieceIndex)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(downloadingMoreAvailablePieceIndex)));

        unitUnderTest.pieceAvailabilities[downloadingOnlyPieceIndex] = 1;
        unitUnderTest.pieceAvailabilities[downloadingMoreAvailablePieceIndex] = 2;
        unitUnderTest.pieceAvailabilities[rarestOnlyPieceIndex] = 1;

        final BitSet peerPieces = new BitSet();
        peerPieces.set(downloadingOnlyPieceIndex);
        peerPieces.set(downloadingMoreAvailablePieceIndex);
        peerPieces.set(rarestOnlyPieceIndex);

        Assert.assertEquals(2, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.pieceFailure(downloadingOnlyPieceIndex);
        unitUnderTest.peerLost(peerPieces);

        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());
        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(1));
        Assert.assertTrue(unitUnderTest.rarestPieces.get(1).contains(downloadingMoreAvailablePieceIndex));

        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[downloadingOnlyPieceIndex]);
        Assert.assertEquals(0, unitUnderTest.pieceAvailabilities[rarestOnlyPieceIndex]);
        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[downloadingMoreAvailablePieceIndex]);

        Assert.assertFalse(unitUnderTest.downloadingPieces.containsKey(downloadingOnlyPieceIndex));
    }

    @Test
    public void testPeerGained() {
        final int obtainedPieceIndex = 5;

        unitUnderTest.rarestPieces.put(1, new ArrayList<>(Arrays.asList(3)));
        unitUnderTest.rarestPieces.put(2, new ArrayList<>(Arrays.asList(4)));
        unitUnderTest.rarestPieces.put(3, new ArrayList<>(Arrays.asList(0)));
        unitUnderTest.rarestPieces.put(4, new ArrayList<>(Arrays.asList(1)));
        unitUnderTest.rarestPieces.put(5, new ArrayList<>(Arrays.asList(2)));

        unitUnderTest.pieceAvailabilities[obtainedPieceIndex] = 0;
        unitUnderTest.pieceAvailabilities[4] = 2;
        unitUnderTest.pieceAvailabilities[3] = 1;
        unitUnderTest.pieceAvailabilities[0] = 3;
        unitUnderTest.pieceAvailabilities[1] = 4;
        unitUnderTest.pieceAvailabilities[2] = 5;

        final BitSet peerPieces = new BitSet();
        peerPieces.set(obtainedPieceIndex);
        peerPieces.set(0);
        peerPieces.set(1);
        peerPieces.set(2);
        peerPieces.set(3);
        peerPieces.set(4);
        peerPieces.set(6);
        peerPieces.set(7);

        //Set some pieces as already have
        unitUnderTest.receivedPieces.set(6);
        unitUnderTest.receivedPieces.set(7);

        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        unitUnderTest.peerGained(peerPieces);

        Assert.assertEquals(5, unitUnderTest.getRarestPiecesCount());

        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(1));
        final List<Integer> oneOccurrencePieces = unitUnderTest.rarestPieces.get(1);
        Assert.assertEquals(1, oneOccurrencePieces.size());
        Assert.assertTrue(oneOccurrencePieces.contains(obtainedPieceIndex));

        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(2));
        final List<Integer> twoOccurrencesPieces = unitUnderTest.rarestPieces.get(2);
        Assert.assertEquals(1, twoOccurrencesPieces.size());
        Assert.assertTrue(twoOccurrencesPieces.contains(3));

        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(3));
        final List<Integer> threeOccurrencesPieces = unitUnderTest.rarestPieces.get(3);
        Assert.assertEquals(1, threeOccurrencesPieces.size());
        Assert.assertTrue(threeOccurrencesPieces.contains(4));

        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(4));
        final List<Integer> fourOccurrencesPieces = unitUnderTest.rarestPieces.get(4);
        Assert.assertEquals(1, fourOccurrencesPieces.size());
        Assert.assertTrue(fourOccurrencesPieces.contains(0));

        Assert.assertTrue(unitUnderTest.rarestPieces.containsKey(5));
        final List<Integer> fiveOccurrencesPieces = unitUnderTest.rarestPieces.get(5);
        Assert.assertEquals(1, fiveOccurrencesPieces.size());
        Assert.assertTrue(fiveOccurrencesPieces.contains(1));

        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[obtainedPieceIndex]);
        Assert.assertEquals(3, unitUnderTest.pieceAvailabilities[4]);
        Assert.assertEquals(2, unitUnderTest.pieceAvailabilities[3]);
        Assert.assertEquals(4, unitUnderTest.pieceAvailabilities[0]);
        Assert.assertEquals(5, unitUnderTest.pieceAvailabilities[1]);
        Assert.assertEquals(6, unitUnderTest.pieceAvailabilities[2]);
        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[6]);
        Assert.assertEquals(1, unitUnderTest.pieceAvailabilities[7]);

        Assert.assertTrue(unitUnderTest.downloadingPieces.isEmpty());
    }

    @Test
    public void testPieceRequested() {
        final int requestedRarestPieceIndex = 9;
        final int requestedNotRarestPieceIndex = 0;

        unitUnderTest.rarestPieces.put(42, new ArrayList<>(Arrays.asList(requestedRarestPieceIndex)));

        unitUnderTest.pieceAvailabilities[requestedRarestPieceIndex] = 42;
        unitUnderTest.pieceAvailabilities[requestedNotRarestPieceIndex] = 89;

        final boolean notRarestPieceRequested = unitUnderTest.pieceRequested(
                requestedNotRarestPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertTrue(notRarestPieceRequested);
        Assert.assertTrue(unitUnderTest.downloadingPieces.containsKey(requestedNotRarestPieceIndex));
        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());

        final boolean rarestPieceRequested = unitUnderTest.pieceRequested(
                requestedRarestPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertTrue(rarestPieceRequested);
        Assert.assertTrue(unitUnderTest.downloadingPieces.containsKey(requestedRarestPieceIndex));
        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());
    }

    @Test
    public void testPieceFailure() {
        final int failedDownloadPieceIndex = 3;

        unitUnderTest.pieceAvailabilities[failedDownloadPieceIndex] = 5;
        unitUnderTest.downloadingPieces.put(failedDownloadPieceIndex, EasyMock.createMock(DataPiece.class));

        Assert.assertEquals(0, unitUnderTest.getRarestPiecesCount());

        final boolean pieceWasRequestedResult = unitUnderTest.pieceFailure(3);

        Assert.assertTrue(pieceWasRequestedResult);
        Assert.assertFalse(unitUnderTest.downloadingPieces.containsKey(failedDownloadPieceIndex));
        Assert.assertFalse(unitUnderTest.receivedPieces.get(failedDownloadPieceIndex));

        Assert.assertEquals(1, unitUnderTest.getRarestPiecesCount());
    }
}