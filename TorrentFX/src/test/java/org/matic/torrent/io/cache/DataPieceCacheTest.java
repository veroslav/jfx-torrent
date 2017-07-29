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
import org.junit.Test;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataPiece;
import org.matic.torrent.net.pwp.PeerSession;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The unit test class for {@link DataPieceCache}.
 *
 * @author Vedran Matic
 */
public final class DataPieceCacheTest {

    private final long cacheSize = 10;
    private DataPieceCache unitUnderTest;

    private final InfoHash infoHash = new InfoHash("1".getBytes(StandardCharsets.UTF_8));

    private final DataPiece dataPieceMock = EasyMock.createMock(DataPiece.class);
    private final PeerSession peerMock = EasyMock.createMock(PeerSession.class);

    @Before
    public void setup() {
        unitUnderTest = new DataPieceCache(cacheSize);
        EasyMock.reset(dataPieceMock, peerMock);
    }

    @Test
    public void testSimpleInsertionOfOnePiece() {
        final int pieceLength = 7;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength);

        EasyMock.replay(dataPieceMock);

        final CachedDataPieceIdentifier identifier = new CachedDataPieceIdentifier(0, infoHash);
        unitUnderTest.put(identifier, dataPieceMock);

        EasyMock.verify(dataPieceMock);

        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(pieceLength, unitUnderTest.getSize());
    }

    @Test
    public void testTwoPieceInsertionsWithinCacheLimits() {
        final DataPiece secondDataPieceMock = EasyMock.createMock(DataPiece.class);
        final PeerSession secondPeerMock = EasyMock.createMock(PeerSession.class);

        final CachedDataPieceIdentifier firstIdentifier = new CachedDataPieceIdentifier(0, infoHash);
        final CachedDataPieceIdentifier secondIdentifier = new CachedDataPieceIdentifier(1, infoHash);

        final int firstPieceLength = 4;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(firstPieceLength);
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        final int secondPieceLength = 6;
        EasyMock.expect(secondDataPieceMock.getLength()).andReturn(secondPieceLength);
        EasyMock.expect(secondDataPieceMock.getIndex()).andReturn(1).anyTimes();

        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();
        EasyMock.expect(secondPeerMock.getInfoHash()).andReturn(infoHash).anyTimes();

        EasyMock.replay(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        unitUnderTest.put(firstIdentifier, dataPieceMock);
        unitUnderTest.put(secondIdentifier, secondDataPieceMock);

        EasyMock.verify(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        Assert.assertEquals(10, unitUnderTest.getSize());
        Assert.assertEquals(2, unitUnderTest.getItemCount());
    }

    @Test
    public void testTwoPieceInsertionsOutsideOfCacheLimits() {
        final DataPiece secondDataPieceMock = EasyMock.createMock(DataPiece.class);
        final PeerSession secondPeerMock = EasyMock.createMock(PeerSession.class);

        final InfoHash firstInfoHash = new InfoHash("1".getBytes(StandardCharsets.UTF_8));
        final InfoHash secondInfoHash = new InfoHash("2".getBytes(StandardCharsets.UTF_8));

        final CachedDataPieceIdentifier firstIdentifier = new CachedDataPieceIdentifier(0, firstInfoHash);
        final CachedDataPieceIdentifier secondIdentifier = new CachedDataPieceIdentifier(0, secondInfoHash);

        final int firstPieceLength = 8;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(firstPieceLength).anyTimes();

        final int secondPieceLength = 3;
        EasyMock.expect(secondDataPieceMock.getLength()).andReturn(secondPieceLength).anyTimes();

        EasyMock.expect(peerMock.getInfoHash()).andReturn(firstInfoHash).anyTimes();
        EasyMock.expect(secondPeerMock.getInfoHash()).andReturn(secondInfoHash).anyTimes();

        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();
        EasyMock.expect(secondDataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        unitUnderTest.put(firstIdentifier, dataPieceMock);
        unitUnderTest.put(secondIdentifier, secondDataPieceMock);

        EasyMock.verify(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        Assert.assertEquals(3, unitUnderTest.getSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
    }

    @Test
    public void testPieceTooBigForCache() {
        final int pieceLength = 14;

        final CachedDataPieceIdentifier identifier = new CachedDataPieceIdentifier(0, infoHash);

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, peerMock);

        unitUnderTest.put(identifier, dataPieceMock);

        EasyMock.verify(dataPieceMock, peerMock);

        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());
    }

    @Test
    public void testInvalidCacheSize() {
        unitUnderTest = new DataPieceCache(-1);

        Assert.assertEquals(DataPieceCache.DEFAULT_MAX_SIZE, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());
    }

    @Test
    public void testDecreaseCacheSizeNoPiecesCached() {
        unitUnderTest = new DataPieceCache(1024);

        Assert.assertEquals(1024, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());

        unitUnderTest.setMaxSize(42);

        Assert.assertEquals(42, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());

        unitUnderTest.setMaxSize(0);

        Assert.assertEquals(DataPieceCache.DEFAULT_MAX_SIZE, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());
    }

    @Test
    public void testDecreaseCacheSizeStillBigEnough() {
        final int pieceLength = 6;

        final CachedDataPieceIdentifier identifier = new CachedDataPieceIdentifier(0, infoHash);

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, peerMock);

        unitUnderTest.put(identifier, dataPieceMock);

        EasyMock.verify(dataPieceMock, peerMock);

        Assert.assertEquals(cacheSize, unitUnderTest.getMaxSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(6, unitUnderTest.getSize());

        unitUnderTest.setMaxSize(6);

        Assert.assertEquals(6, unitUnderTest.getMaxSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(6, unitUnderTest.getSize());
    }

    @Test
    public void testDecreaseCachePieceMustBeRemoved() {
        final int pieceLength = 6;

        final CachedDataPieceIdentifier identifier = new CachedDataPieceIdentifier(0, infoHash);

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, peerMock);

        unitUnderTest.put(identifier, dataPieceMock);

        EasyMock.verify(dataPieceMock, peerMock);

        Assert.assertEquals(cacheSize, unitUnderTest.getMaxSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(6, unitUnderTest.getSize());

        unitUnderTest.setMaxSize(5);

        Assert.assertEquals(5, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());
    }

    @Test
    public void testIncreaseCacheSize() {
        final int pieceLength = 12;

        final CachedDataPieceIdentifier identifier = new CachedDataPieceIdentifier(0, infoHash);

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, peerMock);

        unitUnderTest.put(identifier, dataPieceMock);

        EasyMock.verify(dataPieceMock, peerMock);

        Assert.assertEquals(cacheSize, unitUnderTest.getMaxSize());
        Assert.assertEquals(0, unitUnderTest.getItemCount());
        Assert.assertEquals(0, unitUnderTest.getSize());

        unitUnderTest.setMaxSize(12);
        unitUnderTest.put(identifier, dataPieceMock);

        Assert.assertEquals(12, unitUnderTest.getMaxSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());
        Assert.assertEquals(12, unitUnderTest.getSize());
    }

    @Test
    public void testCacheHit() {
        final DataPiece secondDataPieceMock = EasyMock.createMock(DataPiece.class);
        final PeerSession secondPeerMock = EasyMock.createMock(PeerSession.class);

        final InfoHash firstInfoHash = new InfoHash("1".getBytes(StandardCharsets.UTF_8));
        final InfoHash secondInfoHash = new InfoHash("2".getBytes(StandardCharsets.UTF_8));

        final CachedDataPieceIdentifier firstIdentifier = new CachedDataPieceIdentifier(0, firstInfoHash);
        final CachedDataPieceIdentifier secondIdentifier = new CachedDataPieceIdentifier(0, secondInfoHash);

        final int firstPieceLength = 8;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(firstPieceLength).anyTimes();

        final int secondPieceLength = 3;
        EasyMock.expect(secondDataPieceMock.getLength()).andReturn(secondPieceLength).anyTimes();

        EasyMock.expect(peerMock.getInfoHash()).andReturn(firstInfoHash).anyTimes();
        EasyMock.expect(secondPeerMock.getInfoHash()).andReturn(secondInfoHash).anyTimes();

        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();
        EasyMock.expect(secondDataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.replay(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);

        unitUnderTest = new DataPieceCache(15);
        unitUnderTest.put(firstIdentifier, dataPieceMock);
        unitUnderTest.put(secondIdentifier, secondDataPieceMock);

        EasyMock.verify(dataPieceMock, secondDataPieceMock, peerMock, secondPeerMock);
        Assert.assertEquals(15, unitUnderTest.getMaxSize());
        Assert.assertEquals(11, unitUnderTest.getSize());
        Assert.assertEquals(2, unitUnderTest.getItemCount());

        final Optional<DataPiece> firstDataPiece = unitUnderTest.get(firstIdentifier);
        Assert.assertTrue(firstDataPiece.isPresent());
        Assert.assertEquals(dataPieceMock, firstDataPiece.get());

        final Optional<DataPiece> secondDataPiece = unitUnderTest.get(secondIdentifier);
        Assert.assertTrue(secondDataPiece.isPresent());
        Assert.assertEquals(secondDataPieceMock, secondDataPiece.get());
    }

    @Test
    public void testCacheMiss() {
        //Test searching for a piece with same piece index but differing info hash and vice versa
        final DataPiece searchedDataPieceMock = EasyMock.createMock(DataPiece.class);
        final CachedDataPieceIdentifier firstIdentifier = new CachedDataPieceIdentifier(0, infoHash);

        final int pieceLength = 3;
        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(searchedDataPieceMock.getLength()).andReturn(pieceLength).anyTimes();

        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();
        EasyMock.expect(searchedDataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();

        EasyMock.replay(dataPieceMock, searchedDataPieceMock, peerMock);

        unitUnderTest.put(firstIdentifier, dataPieceMock);

        EasyMock.verify(dataPieceMock, searchedDataPieceMock, peerMock);
        Assert.assertEquals(cacheSize, unitUnderTest.getMaxSize());
        Assert.assertEquals(3, unitUnderTest.getSize());
        Assert.assertEquals(1, unitUnderTest.getItemCount());

        EasyMock.reset(dataPieceMock, searchedDataPieceMock, peerMock);

        //Test a cache miss when piece index differs for a same info hash
        CachedDataPieceIdentifier searchedForIdentifier = new CachedDataPieceIdentifier(1, infoHash);

        EasyMock.expect(searchedDataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(searchedDataPieceMock.getIndex()).andReturn(1).anyTimes();

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(0).anyTimes();

        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();

        EasyMock.replay(searchedDataPieceMock, dataPieceMock, peerMock);

        Optional<DataPiece> searchMiss = unitUnderTest.get(searchedForIdentifier);

        EasyMock.verify(searchedDataPieceMock, dataPieceMock, peerMock);
        Assert.assertFalse(searchMiss.isPresent());

        EasyMock.reset(searchedDataPieceMock, dataPieceMock, peerMock);

        //Test a cache miss when info hashes differ but the piece indexes are equal
        final InfoHash searchedInfoHash = new InfoHash("2".getBytes(StandardCharsets.UTF_8));

        final PeerSession searchedPeerMock = EasyMock.createMock(PeerSession.class);
        searchedForIdentifier = new CachedDataPieceIdentifier(1, searchedInfoHash);

        EasyMock.expect(searchedDataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(searchedDataPieceMock.getIndex()).andReturn(1).anyTimes();
        EasyMock.expect(searchedPeerMock.getInfoHash()).andReturn(searchedInfoHash).anyTimes();

        EasyMock.expect(dataPieceMock.getLength()).andReturn(pieceLength).anyTimes();
        EasyMock.expect(dataPieceMock.getIndex()).andReturn(1).anyTimes();
        EasyMock.expect(peerMock.getInfoHash()).andReturn(infoHash).anyTimes();

        EasyMock.replay(searchedDataPieceMock, dataPieceMock, peerMock, searchedPeerMock);

        searchMiss = unitUnderTest.get(searchedForIdentifier);

        EasyMock.verify(searchedDataPieceMock, dataPieceMock, peerMock, searchedPeerMock);
        Assert.assertFalse(searchMiss.isPresent());
    }
}