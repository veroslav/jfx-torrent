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

import org.matic.torrent.queue.QueuedFileMetaData;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A class for writing and reading a file that is part of a torrent. It has the
 * ability to write/read at any part of the file, which is useful when pieces
 * that are residing across the file need to be read/written from/to the disk.
 *
 * @author Vedran Matic
 */
public final class TorrentFileIO {

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String FILE_ACCESS_MODE = "rw";

    private final RandomAccessFile fileAccessor;
    private final QueuedFileMetaData fileMetaData;

    /**
     * Create a new instance.
     *
     * @param filePath Absolute path to the torrent file on the disk
     * @param fileMetaData File's meta data
     * @throws IOException If any errors occur during I/O operations on the file
     */
    public TorrentFileIO(final Path filePath, final QueuedFileMetaData fileMetaData) throws IOException {

        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);

        fileAccessor = new RandomAccessFile(filePath.toFile(), FILE_ACCESS_MODE);
        this.fileMetaData = fileMetaData;

        //TODO: Call fileAccessor.setLength(fileMetaData.getLength()) based on user preference in Properties
    }

    /**
     * Write as much as possible of a data piece into the correct place of the file on the disk.
     *
     * @param dataPiece Data piece to write into the file on the disk
     * @return How many bytes were written
     * @throws IOException If any errors occur during the file writing
     */
    public int writePieceToDisk(final DataPiece dataPiece) throws IOException {
        final long pieceIndex = dataPiece.getIndex();
        final long pieceLength = dataPiece.getLength();

        //Piece's total position offset within the torrent file
        final long pieceBeginPosition = pieceIndex * pieceLength;
        final long pieceEndPosition = pieceBeginPosition + pieceLength;

        final long fileBeginPosition = fileMetaData.getOffset();
        final long fileEndPosition = fileBeginPosition + fileMetaData.getLength();

        //Check whether the piece is inside this file
        if(pieceEndPosition < fileBeginPosition || pieceBeginPosition > fileEndPosition) {
            return 0;
        }

        fileAccessor.seek(pieceBeginPosition < fileBeginPosition? 0: pieceBeginPosition - fileBeginPosition);

        //Calculate how much of the piece data is contained in this file
        final int writeDataLength = (int)(pieceBeginPosition < fileBeginPosition?
                pieceEndPosition - fileBeginPosition : fileEndPosition - pieceBeginPosition);

        final int pieceBytesStart = pieceBeginPosition < fileBeginPosition?
                (int)(pieceBeginPosition + (pieceEndPosition - fileBeginPosition)) : 0;

        fileAccessor.write(dataPiece.getPieceBytes(), pieceBytesStart, writeDataLength);
        return writeDataLength;
    }

    /**
     * Read as much as possible of a data piece from this file on the disk.
     *
     * @param pieceLength The length of data piece
     * @param pieceIndex The data piece index relative to the torrent's contents
     * @return Read data piece bytes
     * @throws IOException If any errors occur during the file reading
     */
    public byte[] readPieceFromDisk(final int pieceLength, final int pieceIndex) throws IOException {

        //Piece's total position offset within the torrent file
        final long pieceBeginPosition = pieceIndex * pieceLength;
        final long pieceEndPosition = pieceBeginPosition + pieceLength;

        final long fileBeginPosition = fileMetaData.getOffset();
        final long fileEndPosition = fileBeginPosition + fileMetaData.getLength();

        //Check whether the piece is inside this file
        if(pieceEndPosition < fileBeginPosition || pieceBeginPosition > fileEndPosition) {
            return EMPTY_BYTES;
        }

        fileAccessor.seek(pieceBeginPosition < fileBeginPosition? 0: pieceBeginPosition - fileBeginPosition);

        //Calculate how much of the piece data is contained in this file
        final int readDataLength = (int)(pieceBeginPosition < fileBeginPosition?
                pieceEndPosition - fileBeginPosition : fileEndPosition - pieceBeginPosition);

        final byte[] pieceBytes = new byte[readDataLength];
        fileAccessor.read(pieceBytes, 0, pieceBytes.length);
        return pieceBytes;
    }
}