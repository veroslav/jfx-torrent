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

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.cache.DataPieceCache;

import java.io.IOException;

/**
 * A helper that manages piece storing and retrieval. When saving data, it updates its internal
 * cache, so that if the same data is requested in the near future, it can be retrieved directly
 * from it. Otherwise, it loads the data from the disk.
 *
 * @author Vedran Matic
 */
public final class TorrentDiskFileProxy {

    private final DataPieceCache<InfoHash> cache;
    private final TorrentFileIO fileIO;

    /**
     * Create a new instance.
     *
     * @param fileIO For writing/reading piece data to/from the disk
     * @param cache For writing/reading piece data to/from the cache
     */
    public TorrentDiskFileProxy(final TorrentFileIO fileIO, final DataPieceCache<InfoHash> cache) {
        this.fileIO = fileIO;
        this.cache = cache;
    }

    /**
     * Get a piece's data, either from cache if it has been cached, or directly from the disk.
     *
     * @param infoHash Torrent identifier used as a cache key
     * @param pieceIndex Target data piece's index within the torrent
     * @param pieceLength How many bytes to read into the piece data
     * @return The read data piece
     * @throws IOException If any I/O error occurs while reading the piece data from the disk
     */
    public DataPiece retrievePiece(final InfoHash infoHash, final int pieceIndex,
                                   final int pieceLength) throws IOException {
        final DataPiece cachedPiece = cache.getItem(infoHash);
        if(cachedPiece != null) {
            return cachedPiece;
        }

        final DataPiece pieceFromDisk = fileIO.readPieceFromDisk(pieceLength, pieceIndex);
        cache.addItem(infoHash, pieceFromDisk);

        return pieceFromDisk;
    }

    /**
     * Store a piece's data in both the cache and on the disk.
     *
     * @param piece The piece data to be stored
     * @param infoHash Torrent identifier used as a cache key
     * @throws IOException If any I/O error occurs while writing the piece data to the disk
     */
    public void storePiece(final DataPiece piece, final InfoHash infoHash) throws IOException {
        cache.addItem(infoHash, piece);
        fileIO.writePieceToDisk(piece);
    }
}