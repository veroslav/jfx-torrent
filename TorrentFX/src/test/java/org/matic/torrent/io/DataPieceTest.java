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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

public final class DataPieceTest {

    private final byte[] expectedDigestedBytes = new byte[]{
            73, 65, 121, 113, 74, 108, -42, 39, 35, -99, -2, -34, -33, 45, -23, -17, -103, 76, -81, 3};

    private final byte[] block1Bytes = new byte[]{0, 1, 2};
    private final byte[] block2Bytes = new byte[]{3, 4, 5};
    private final byte[] block3Bytes = new byte[]{6, 7, 8};
    private final byte[] lastBlockBytes = new byte[]{9};

    private final int pieceLength = 10;
    private final int pieceIndex = 42;

    private final DataBlock block1 = new DataBlock(block1Bytes, pieceIndex, 0);
    private final DataBlock block2 = new DataBlock(block2Bytes, pieceIndex, 3);
    private final DataBlock block3 = new DataBlock(block3Bytes, pieceIndex, 6);
    private final DataBlock block4 = new DataBlock(lastBlockBytes, pieceIndex, 9);

    private DataPiece unitUnderTest;

    @Before
    public void setup() throws Exception {
        unitUnderTest = new DataPiece(pieceLength, pieceIndex);
    }

    @Test
    public void testAddBlocksUntilPieceIsCompleted() {
        final boolean block1Added = unitUnderTest.addBlock(block1);

        Assert.assertTrue(block1Added);
        Assert.assertTrue(unitUnderTest.queuedBlocks.isEmpty());
        Assert.assertEquals(3, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block1Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 0, 3));

        final boolean block2Added = unitUnderTest.addBlock(block2);

        Assert.assertTrue(block2Added);
        Assert.assertTrue(unitUnderTest.queuedBlocks.isEmpty());
        Assert.assertEquals(6, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block2Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 3, 6));

        final boolean block3Added = unitUnderTest.addBlock(block3);

        Assert.assertTrue(block3Added);
        Assert.assertTrue(unitUnderTest.queuedBlocks.isEmpty());
        Assert.assertEquals(9, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block3Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 6, 9));

        final boolean block4Added = unitUnderTest.addBlock(block4);

        Assert.assertTrue(block4Added);
        Assert.assertTrue(unitUnderTest.queuedBlocks.isEmpty());
        Assert.assertEquals(10, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(lastBlockBytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 9, 10));

        Assert.assertTrue(unitUnderTest.hasCompleted());
        Assert.assertTrue(unitUnderTest.validate(expectedDigestedBytes));
    }

    @Test
    public void testAddBlocksOutOfOrder() {
        //Blocks will be added in this order: 4, 1, 3, 2
        final boolean block4Added = unitUnderTest.addBlock(block4);

        Assert.assertTrue(block4Added);
        Assert.assertEquals(1, unitUnderTest.queuedBlocks.size());
        Assert.assertTrue(unitUnderTest.queuedBlocks.containsKey(9));
        Assert.assertEquals(block4, unitUnderTest.queuedBlocks.get(9));
        Assert.assertEquals(0, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(lastBlockBytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 9, 10));

        final boolean block1Added = unitUnderTest.addBlock(block1);

        Assert.assertTrue(block1Added);
        Assert.assertEquals(1, unitUnderTest.queuedBlocks.size());
        Assert.assertTrue(unitUnderTest.queuedBlocks.containsKey(9));
        Assert.assertFalse(unitUnderTest.queuedBlocks.containsKey(3));
        Assert.assertEquals(3, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block1Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 0, 3));

        final boolean block3Added = unitUnderTest.addBlock(block3);

        Assert.assertTrue(block3Added);
        Assert.assertEquals(2, unitUnderTest.queuedBlocks.size());
        Assert.assertTrue(unitUnderTest.queuedBlocks.containsKey(6));
        Assert.assertEquals(block3, unitUnderTest.queuedBlocks.get(6));
        Assert.assertEquals(3, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block3Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 6, 9));

        final boolean block2Added = unitUnderTest.addBlock(block2);

        Assert.assertTrue(block2Added);
        Assert.assertEquals(0, unitUnderTest.queuedBlocks.size());
        Assert.assertEquals(10, unitUnderTest.digestedBlocksPointer);
        Assert.assertArrayEquals(block2Bytes, Arrays.copyOfRange(unitUnderTest.getPieceBytes(), 3, 6));

        Assert.assertTrue(unitUnderTest.hasCompleted());
        Assert.assertTrue(unitUnderTest.validate(expectedDigestedBytes));
    }

    @Test
    public void testAddInvalidBlock() {
        final boolean tooLongBlockAdded = unitUnderTest.addBlock(new DataBlock(block1Bytes, pieceIndex, 10));
        Assert.assertFalse(tooLongBlockAdded);

        final boolean blockForWrongPieceAdded = unitUnderTest.addBlock(new DataBlock(block1Bytes, 2, 0));
        Assert.assertFalse(blockForWrongPieceAdded);
    }

    @Test
    public void testGetBlock() {
        final boolean block1Added = unitUnderTest.addBlock(block1);
        Assert.assertTrue(block1Added);

        final boolean block2Added = unitUnderTest.addBlock(block2);
        Assert.assertTrue(block2Added);

        final boolean block3Added = unitUnderTest.addBlock(block3);
        Assert.assertTrue(block3Added);

        final boolean block4Added = unitUnderTest.addBlock(block4);
        Assert.assertTrue(block4Added);

        final Optional<DataBlock> longerBlock = unitUnderTest.getBlock(0, 5);
        Assert.assertTrue(longerBlock.isPresent());
        Assert.assertEquals(pieceIndex, longerBlock.get().getPieceIndex());
        Assert.assertEquals(0, longerBlock.get().getPieceOffset());
        Assert.assertArrayEquals(new byte[]{0, 1, 2, 3, 4}, longerBlock.get().getBlockData());
    }

    @Test
    public void testGetInvalidBlock() {
        final boolean block1Added = unitUnderTest.addBlock(block1);
        Assert.assertTrue(block1Added);

        final boolean block2Added = unitUnderTest.addBlock(block2);
        Assert.assertTrue(block2Added);

        final boolean block3Added = unitUnderTest.addBlock(block3);
        Assert.assertTrue(block3Added);

        final boolean block4Added = unitUnderTest.addBlock(block4);
        Assert.assertTrue(block4Added);

        Assert.assertFalse(unitUnderTest.getBlock(-1, 3).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(8, 3).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(11, 3).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(4, 7).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(4, 0).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(4, -1).isPresent());
        Assert.assertFalse(unitUnderTest.getBlock(0, 11).isPresent());
    }
}