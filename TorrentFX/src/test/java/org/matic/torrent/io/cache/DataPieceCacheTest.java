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
package org.matic.torrent.io.cache;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.io.DataPiece;
import org.matic.torrent.io.DataPieceIdentifier;

/**
 * @author Vedran Matic
 */
public final class DataPieceCacheTest {

    private final long cacheSize = 10;
    private DataPieceCache unitUnderTest;

    private final DataPieceIdentifier identifierMock = EasyMock.createMock(DataPieceIdentifier.class);
    private final DataPiece dataPieceMock = EasyMock.createMock(DataPiece.class);
    private final PeerView peerMock = EasyMock.createMock(PeerView.class);

    @Before
    public void setup() {
        unitUnderTest = new DataPieceCache(cacheSize);
        EasyMock.reset(identifierMock, dataPieceMock, peerMock);
    }

    @Test
    public void testSimpleInsertionOfOnePiece() {
        final int pieceLength = 7;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength);

        EasyMock.replay(dataPieceMock);

        unitUnderTest.put(identifierMock, dataPieceMock);

        EasyMock.verify(dataPieceMock);

        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(pieceLength, unitUnderTest.getSize());
    }

    @Test
    public void testTwoPieceInsertionsWithinCacheLimits() {
        final DataPiece secondDataPieceMock = EasyMock.createMock(DataPiece.class);
        final PeerView secondPeerMock = EasyMock.createMock(PeerView.class);

        final DataPieceIdentifier firstIdentifier = new DataPieceIdentifier(dataPieceMock, null, peerMock);
        final DataPieceIdentifier secondIdentifier = new DataPieceIdentifier(secondDataPieceMock, null, secondPeerMock);

        final int firstPieceLength = 4;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(firstPieceLength);

        final int secondPieceLength = 6;
        EasyMock.expect(secondDataPieceMock.getLength()).andReturn(secondPieceLength);

        EasyMock.replay(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        unitUnderTest.put(firstIdentifier, dataPieceMock);
        unitUnderTest.put(secondIdentifier, secondDataPieceMock);

        EasyMock.verify(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        Assert.assertEquals(10, unitUnderTest.getSize());
        Assert.assertEquals(2, unitUnderTest.getItemCount());
    }

    //TODO: Fix this failing test by fixing the DataPieceCache implementation
    @Ignore
    @Test
    public void testTwoPieceInsertionsOutsideOfCacheLimits() {
        final DataPiece secondDataPieceMock = EasyMock.createMock(DataPiece.class);
        final PeerView secondPeerMock = EasyMock.createMock(PeerView.class);

        final DataPieceIdentifier firstIdentifier = new DataPieceIdentifier(dataPieceMock, null, peerMock);
        final DataPieceIdentifier secondIdentifier = new DataPieceIdentifier(secondDataPieceMock, null, secondPeerMock);

        final int firstPieceLength = 8;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(firstPieceLength);

        final int secondPieceLength = 3;
        EasyMock.expect(secondDataPieceMock.getLength()).andReturn(secondPieceLength);

        EasyMock.replay(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        unitUnderTest.put(firstIdentifier, dataPieceMock);
        unitUnderTest.put(secondIdentifier, secondDataPieceMock);

        EasyMock.verify(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        Assert.assertEquals(8, unitUnderTest.getSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
    }

    @Test
    public void testPieceToBigForCache() {

    }

    @Test
    public void testNegativeCacheSize() {

    }

    @Test
    public void testDecreaseCacheSizeNoPiecesCached() {

    }

    @Test
    public void testDecreaseCacheSizeStillBigEnough() {

    }

    @Test
    public void testDecreaseCachePieceMustBeRemoved() {

    }

    @Test
    public void testIncreaseCacheSize() {

    }

    @Test
    public void testCacheHit() {

    }

    @Test
    public void testCacheMiss() {

    }
}