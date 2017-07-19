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

import org.matic.torrent.io.DataPiece;
import org.matic.torrent.io.DataPieceIdentifier;

import java.util.Optional;
import java.util.TreeMap;

/**
 * A simple cache for data pieces.
 *
 * @author Vedran Matic
 */
public final class DataPieceCache {

    private final TreeMap<DataPieceIdentifier, DataPiece> cache = new TreeMap<>();
    private long currentSize;
    private long maxSize;

    //TODO: Add cache timeout (TTL)

    /**
     * Create a new instance.
     *
     * @param maxSize Max cache size (in bytes)
     */
    public DataPieceCache(final long maxSize) {
        this.maxSize = maxSize;
    }

    protected int getItemCount() {
        return cache.size();
    }

    protected long getSize() {
        return currentSize;
    }

    public synchronized void setMaxSize(final long maxSize) {
        if(maxSize < this.maxSize) {
            trimCache(maxSize);
        }
        this.maxSize = maxSize;
    }

    public synchronized Optional<DataPiece> get(final DataPieceIdentifier itemKey) {
        return Optional.ofNullable(cache.get(itemKey));
    }

    public synchronized boolean put(final DataPieceIdentifier itemKey, final DataPiece item) {
        if(cache.containsKey(itemKey)) {
            return false;
        }

        itemKey.setCachingTime(System.currentTimeMillis());
        cache.put(itemKey, item);

        final int itemLength = item.getLength();
        currentSize += itemLength;

        if(currentSize > maxSize) {
            trimCache(maxSize);
        }

        return true;

        /*while (currentSize + itemLength > maxSize) {
            final DataPiece oldestEntry = cache.remove(cache.lastEntry());
            currentSize -= oldestEntry.getLength();
        }*/
    }

    private void trimCache(final long targetSize) {
        while (targetSize < currentSize) {

            //System.out.println(cache.firstEntry());

            final DataPiece oldestEntry = cache.remove(cache.firstEntry().getKey());
            currentSize -= oldestEntry.getLength();
        }
    }
}