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
import org.matic.torrent.transfer.DataBlockRequest;

public final class DataPieceRequest {

    private final DataBlockRequest blockRequest;
    private final PeerView requester;

    public DataPieceRequest(final DataBlockRequest blockRequest, final PeerView requester) {
        this.blockRequest = blockRequest;
        this.requester = requester;
    }

    public DataBlockRequest getBlockRequest() {
        return blockRequest;
    }

    public PeerView getRequester() {
        return requester;
    }
}