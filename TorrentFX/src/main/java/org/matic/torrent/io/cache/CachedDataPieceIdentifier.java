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

import org.matic.torrent.hash.InfoHash;

import java.util.Objects;

public class CachedDataPieceIdentifier implements Comparable<CachedDataPieceIdentifier> {

    private final InfoHash infoHash;
    private final int pieceIndex;

    private long cachingTime = 0;

    public CachedDataPieceIdentifier(final int pieceIndex, final InfoHash infoHash) {
        this.pieceIndex = pieceIndex;
        this.infoHash = infoHash;
    }

    /**
     * Update with a timestamp when/if caching of this piece takes place.
     *
     * @param cachingTime Time of caching
     */
    public void setCachingTime(final long cachingTime) {
        this.cachingTime = cachingTime;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CachedDataPieceIdentifier that = (CachedDataPieceIdentifier) o;
        return Objects.equals(pieceIndex, that.pieceIndex) &&
                Objects.equals(infoHash, that.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceIndex, infoHash);
    }

    @Override
    public int compareTo(final CachedDataPieceIdentifier other) {
        //First check whether the pieces come from the same torrent
        if(!infoHash.equals(other.infoHash)) {
            return infoHash.toString().compareTo(other.infoHash.toString());
        }

        //Then check whether they have the same piece indexes
        final int pieceIndexCompare = Integer.compare(pieceIndex, other.pieceIndex);
        if(pieceIndexCompare != 0) {
            return pieceIndexCompare;
        }

        //Finally, compare them based on their caching time
        /*if(cachingTime < other.cachingTime) {
            return -1;
        }
        else if(cachingTime > other.cachingTime) {
            return 1;
        }*/

        //These pieces are equivalent
        return 0;
    }

    @Override
    public String toString() {
        return "CachedDataPieceIdentifier{" +
                "infoHash=" + infoHash +
                ", pieceIndex=" + pieceIndex +
                '}';
    }
}