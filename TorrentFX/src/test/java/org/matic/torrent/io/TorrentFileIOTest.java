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

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.queue.QueuedFileMetaData;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public final class TorrentFileIOTest {

    private final RandomAccessFile fileAccessorMock = EasyMock.createMock(RandomAccessFile.class);
    private final Path pathMock = EasyMock.createMock(Path.class);

    private TorrentFileIO unitUnderTest;

    @Before
    public void setup() {
        EasyMock.reset(fileAccessorMock, pathMock);
    }

    @Test
    public void testWritePieceCompletelyInsideFile() throws IOException {
        final long fileLength = 10;
        final long fileOffset = 0;
        final int pieceIndex = 1;

        final byte[] blocks = new byte[] {0, 1, 2};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(3);
        EasyMock.expectLastCall();

        fileAccessorMock.write(blocks, 0, 3);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(3, bytesWritten);
    }

    @Test
    public void testWritePieceBeginsWhereFileBeginsAndEndsInsideFile() throws IOException {
        final long fileLength = 10;
        final long fileOffset = 0;
        final int pieceIndex = 0;

        final byte[] blocks = new byte[] {0, 1, 2};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        fileAccessorMock.write(blocks, 0, 3);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(3, bytesWritten);
    }

    @Test
    public void testWritePieceEndsWhereFileEndsAndBeginsInsideFile() throws IOException {
        final long fileLength = 10;
        final long fileOffset = 0;
        final int pieceIndex = 1;

        final byte[] blocks = new byte[] {0, 1, 2, 3, 4};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(5);
        EasyMock.expectLastCall();

        fileAccessorMock.write(blocks, 0, 5);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(5, bytesWritten);
    }

    @Test
    public void testWritePieceBeginsBeforeFileBeginsAndEndsInsideFile() throws IOException {
        final long fileLength = 10;
        final long fileOffset = 6;
        final int pieceIndex = 1;

        final byte[] blocks = new byte[] {0, 1, 2, 3, 4};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        fileAccessorMock.write(blocks, 1, 4);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(4, bytesWritten);
    }

    @Test
    public void testWritePieceEndsAfterFileEndsAndBeginsInsideFile() throws IOException {
        final long fileLength = 6;
        final long fileOffset = 0;
        final int pieceIndex = 1;

        final byte[] blocks = new byte[] {0, 1, 2, 3, 4};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(5);
        EasyMock.expectLastCall();

        fileAccessorMock.write(blocks, 0, 1);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(1, bytesWritten);
    }

    @Test
    public void testWritePieceBeginsBeforeFileBeginsAndEndsAfterFileEnds() throws IOException {
        final long fileLength = 3;
        final long fileOffset = 4;
        final int pieceIndex = 0;

        final byte[] blocks = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(pathMock, fileLength, fileOffset);
        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        fileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        EasyMock.expect(fileAccessorMock.length()).andReturn(42L);

        fileAccessorMock.write(blocks, 4, 3);
        EasyMock.expectLastCall();

        EasyMock.replay(fileAccessorMock, pathMock);

        unitUnderTest = new TorrentFileIO(fileMetaData, fileAccessorMock);
        final int bytesWritten = unitUnderTest.writePieceToDisk(pieceToWrite);

        EasyMock.verify(fileAccessorMock, pathMock);

        Assert.assertEquals(3, bytesWritten);
    }

    @Test
    public void testWritePieceAcrossThreeFiles() throws IOException {
        final long firstFileLength = 3;
        final long firstFileOffset = 0;
        final QueuedFileMetaData firstFileMetaData = new QueuedFileMetaData(pathMock, firstFileLength, firstFileOffset);
        final RandomAccessFile firstFileAccessorMock = EasyMock.createMock(RandomAccessFile.class);

        final long secondFileLength = 3;
        final long secondFileOffset = 3;
        final QueuedFileMetaData secondFileMetaData = new QueuedFileMetaData(pathMock, secondFileLength, secondFileOffset);
        final RandomAccessFile secondFileAccessorMock = EasyMock.createMock(RandomAccessFile.class);

        final long thirdFileLength = 4;
        final long thirdFileOffset = 6;
        final QueuedFileMetaData thirdFileMetaData = new QueuedFileMetaData(pathMock, thirdFileLength, thirdFileOffset);
        final RandomAccessFile thirdFileAccessorMock = EasyMock.createMock(RandomAccessFile.class);

        final int pieceIndex = 0;
        final byte[] blocks = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        final DataPiece pieceToWrite = new DataPiece(blocks, pieceIndex);

        firstFileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        firstFileAccessorMock.write(blocks, 0, 3);
        EasyMock.expectLastCall();

        secondFileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        secondFileAccessorMock.write(blocks, 3, 3);
        EasyMock.expectLastCall();

        thirdFileAccessorMock.seek(0);
        EasyMock.expectLastCall();

        thirdFileAccessorMock.write(blocks, 6, 4);
        EasyMock.expectLastCall();

        EasyMock.expect(firstFileAccessorMock.length()).andReturn(42L);
        EasyMock.expect(secondFileAccessorMock.length()).andReturn(42L);
        EasyMock.expect(thirdFileAccessorMock.length()).andReturn(42L);

        EasyMock.replay(firstFileAccessorMock, secondFileAccessorMock, thirdFileAccessorMock, pathMock);

        final TorrentFileIO firstFileIO = new TorrentFileIO(firstFileMetaData, firstFileAccessorMock);
        final int firstFileIOBytesWritten = firstFileIO.writePieceToDisk(pieceToWrite);

        final TorrentFileIO secondFileIO = new TorrentFileIO(secondFileMetaData, secondFileAccessorMock);
        final int secondFileIOBytesWritten = secondFileIO.writePieceToDisk(pieceToWrite);

        final TorrentFileIO thirdFileIO = new TorrentFileIO(thirdFileMetaData, thirdFileAccessorMock);
        final int thirdFileIOBytesWritten = thirdFileIO.writePieceToDisk(pieceToWrite);

        EasyMock.verify(firstFileAccessorMock, secondFileAccessorMock, thirdFileAccessorMock, pathMock);

        Assert.assertEquals(3, firstFileIOBytesWritten);
        Assert.assertEquals(3, secondFileIOBytesWritten);
        Assert.assertEquals(4, thirdFileIOBytesWritten);
    }
}