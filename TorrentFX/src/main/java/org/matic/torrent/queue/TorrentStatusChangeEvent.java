/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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
package org.matic.torrent.queue;

import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.queue.enums.TorrentStatus;

/**
 * An event that is triggered when a torrent's state change has been requested.
 *
 * @author Vedran Matic
 */
public final class TorrentStatusChangeEvent {

    private final TorrentView torrentView;
    private final TorrentStatus newStatus;
    private final TorrentStatus oldStatus;

    public TorrentStatusChangeEvent(final TorrentView torrentView,
                                    final TorrentStatus oldStatus, final TorrentStatus newStatus) {
        this.torrentView = torrentView;
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
    }

    public TorrentView getTorrentView() {
        return torrentView;
    }

    public TorrentStatus getNewStatus() {
        return newStatus;
    }

    public TorrentStatus getOldStatus() {
        return oldStatus;
    }
}