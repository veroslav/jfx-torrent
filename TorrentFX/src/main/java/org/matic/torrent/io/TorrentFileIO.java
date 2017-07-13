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

    private static final String FILE_ACCESS_MODE = "rw";

    private final RandomAccessFile fileAccessor;
    private final QueuedFileMetaData fileMetaData;

    public TorrentFileIO(final Path filePath, final QueuedFileMetaData fileMetaData) throws FileNotFoundException {
        fileAccessor = new RandomAccessFile(filePath.toFile(), FILE_ACCESS_MODE);
        this.fileMetaData = fileMetaData;

        //TODO: Call fileAccessor.setLength(fileMetaData.getLength()) based on user preference in Properties
    }

    public void writePieceToDisk(final DataPiece dataPiece) throws IOException {
        fileAccessor.seek(dataPiece.getFileOffset());
        fileAccessor.write(dataPiece.getPieceBytes());
    }

    public DataPiece readPieceFromDisk(final int pieceLength, final int pieceIndex) throws IOException {
        final byte[] pieceBytes = new byte[pieceLength];
        final long piecePosition = fileMetaData.getOffset() - pieceIndex * pieceLength;

        fileAccessor.seek(piecePosition);
        fileAccessor.read(pieceBytes, 0, pieceLength);

        final DataPiece dataPiece = new DataPiece(pieceBytes, pieceIndex, piecePosition);
        return dataPiece;
    }
}