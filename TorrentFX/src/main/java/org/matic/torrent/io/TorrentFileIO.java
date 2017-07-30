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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    private final QueuedFileMetaData fileMetaData;
    private final Path filePath;

    private RandomAccessFile fileAccessor;

    /**
     * Create a new instance.
     *
     * @param filePath Absolute path to the torrent file on the disk
     * @param fileMetaData File's meta data
     * @throws IOException If any errors occur during I/O operations on the file
     */
    public TorrentFileIO(final Path filePath, final QueuedFileMetaData fileMetaData) throws IOException {
        this(filePath, fileMetaData, new RandomAccessFile(filePath.toFile(), FILE_ACCESS_MODE));
    }

    //A constructor to use in unit tests
    protected TorrentFileIO(final Path filePath, final QueuedFileMetaData fileMetaData,
                            final RandomAccessFile fileAccessor) throws IOException {
        this.filePath = filePath;
        this.fileMetaData = fileMetaData;
        this.fileAccessor = fileAccessor;

        //TODO: Call fileAccessor.setLength(fileMetaData.getLength()) based on user preference in Properties
        final long fileLength = fileMetaData.getLength();
        if(fileAccessor.length() < fileLength) {
            fileAccessor.setLength(fileLength);
        }
    }

    /**
     * Create and setup file accessor resources.
     */
    public void setup() {
        if(fileAccessor == null) {
            try {
                fileAccessor = new RandomAccessFile(filePath.toFile(), FILE_ACCESS_MODE);
            } catch (final FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
        }
    }

    /**
     * Close and release all file accessor resources on shutdown.
     */
    public void cleanup() {
        if(fileAccessor != null) {
            try {
                fileAccessor.close();
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }
            fileAccessor = null;
        }
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
        final long pieceEndPosition = pieceBeginPosition + pieceLength - 1;

        final long fileBeginPosition = fileMetaData.getOffset();
        final long fileEndPosition = fileBeginPosition + fileMetaData.getLength() - 1;

        //Check whether the piece is inside this file
        if(pieceEndPosition < fileBeginPosition || pieceBeginPosition > fileEndPosition) {
            return 0;
        }

        fileAccessor.seek(pieceBeginPosition < fileBeginPosition? 0: pieceBeginPosition - fileBeginPosition);

        //Calculate how much of the piece data is contained in this file
        final int writeDataLength = getFileAccessorInputDataLength(
                pieceBeginPosition, pieceEndPosition, fileBeginPosition, fileEndPosition);

        final int pieceBytesStart = (pieceBeginPosition < fileBeginPosition)?
                (pieceEndPosition <= fileEndPosition? (int)(pieceLength - writeDataLength) :
                        (int)(fileBeginPosition - pieceBeginPosition)): 0;

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
        final long pieceEndPosition = pieceBeginPosition + pieceLength - 1;

        final long fileBeginPosition = fileMetaData.getOffset();
        final long fileEndPosition = fileBeginPosition + fileMetaData.getLength() - 1;

        //Check whether the piece is inside this file
        if(pieceEndPosition < fileBeginPosition || pieceBeginPosition > fileEndPosition) {
            return EMPTY_BYTES;
        }

        fileAccessor.seek(pieceBeginPosition < fileBeginPosition? 0: pieceBeginPosition - fileBeginPosition);

        //Calculate how much of the piece data is contained in this file
        final int readDataLength = getFileAccessorInputDataLength(
                pieceBeginPosition, pieceEndPosition, fileBeginPosition, fileEndPosition);

        final byte[] pieceBytes = new byte[readDataLength];
        fileAccessor.read(pieceBytes, 0, pieceBytes.length);
        return pieceBytes;
    }

    //Calculate how many of the piece's bytes we can write into this file
    private int getFileAccessorInputDataLength(long pieceBeginPosition, long pieceEndPosition,
                                               long fileBeginPosition, long fileEndPosition) {
        //Check whether any parts of the piece belong to the previous file
        if(pieceBeginPosition < fileBeginPosition) {
            if(pieceEndPosition > fileEndPosition) {
                return (int)(fileEndPosition - fileBeginPosition + 1);
            } else {
                return (int)(pieceEndPosition - fileBeginPosition + 1);
            }
        }
        //Check whether any parts of the piece belong to the next file
        else if(pieceEndPosition > fileEndPosition) {
            return (int)((pieceEndPosition - pieceBeginPosition + 1) - (pieceEndPosition - fileEndPosition));
        }
        //All of the piece's blocks belong to this file
        else {
            return (int)(pieceEndPosition - pieceBeginPosition + 1);
        }
    }
}