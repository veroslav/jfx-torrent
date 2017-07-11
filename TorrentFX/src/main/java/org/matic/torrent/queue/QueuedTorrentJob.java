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
package org.matic.torrent.queue;

import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.transfer.TransferController;

/**
 * This class is a simple bean that groups relevant queued torrent info under the same roof.
 *
 * @author Vedran Matic
 */
public final class QueuedTorrentJob {

    private final TransferController transferController;
    private final TorrentView torrentView;
    private final QueuedTorrent torrent;

    public QueuedTorrentJob(final QueuedTorrent torrent, final TorrentView torrentView,
                            final TransferController transferController) {
        this.torrent = torrent;
        this.transferController = transferController;
        this.torrentView = torrentView;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public TorrentView getTorrentView() {
        return torrentView;
    }

    public QueuedTorrent getTorrent() {
        return torrent;
    }
}