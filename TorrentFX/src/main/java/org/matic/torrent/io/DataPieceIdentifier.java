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

import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.transfer.DataBlockIdentifier;

import java.util.Objects;
import java.util.Optional;

public class DataPieceIdentifier implements Comparable<DataPieceIdentifier> {

    private final DataBlockIdentifier dataBlockIdentifier;
    private final DataPiece dataPiece;
    private final PeerView targetPeer;

    private long cachingTime = 0;

    public DataPieceIdentifier(final DataPiece dataPiece, final DataBlockIdentifier blockIdentifier,
                               final PeerView targetPeer) {
        this.dataBlockIdentifier = blockIdentifier;
        this.targetPeer = targetPeer;
        this.dataPiece = dataPiece;
    }

    /**
     * Update with a timestamp when/if caching of this piece takes place.
     *
     * @param cachingTime Time of caching
     */
    public void setCachingTime(final long cachingTime) {
        this.cachingTime = cachingTime;
    }

    public Optional<DataPiece> getDataPiece() {
        return Optional.ofNullable(dataPiece);
    }

    public Optional<DataBlockIdentifier> getBlockIdentifier() {
        return Optional.ofNullable(dataBlockIdentifier);
    }

    public PeerView getTargetPeer() {
        return targetPeer;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataPieceIdentifier that = (DataPieceIdentifier) o;
        return Objects.equals(dataPiece.getIndex(), that.dataPiece.getIndex()) &&
                Objects.equals(targetPeer.getInfoHash(), that.targetPeer.getInfoHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataPiece.getIndex(), targetPeer.getInfoHash());
    }

    //TODO: Implement equals(), hashCode() and compareTo() correctly
    @Override
    public int compareTo(final DataPieceIdentifier other) {
        if(cachingTime <= other.cachingTime) {
            return -1;
        }
        else {
            return 1;
        }
    }
}