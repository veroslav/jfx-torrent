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

import org.matic.torrent.io.cache.CachedDataPieceIdentifier;

import java.util.Objects;

public final class WriteDataPieceRequest {

    private final CachedDataPieceIdentifier pieceIdentifier;
    private final DataPiece dataPiece;

    public WriteDataPieceRequest(final CachedDataPieceIdentifier pieceIdentifier, final DataPiece dataPiece) {
        this.pieceIdentifier = pieceIdentifier;
        this.dataPiece = dataPiece;
    }

    public CachedDataPieceIdentifier getCachedDataPieceIdentifier() {
        return pieceIdentifier;
    }

    public DataPiece getDataPiece() {
        return dataPiece;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WriteDataPieceRequest that = (WriteDataPieceRequest) o;
        return Objects.equals(pieceIdentifier, that.pieceIdentifier) &&
                Objects.equals(dataPiece, that.dataPiece);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceIdentifier, dataPiece);
    }
}