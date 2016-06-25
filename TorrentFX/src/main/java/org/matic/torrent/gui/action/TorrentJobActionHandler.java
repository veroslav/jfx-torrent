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
package org.matic.torrent.gui.action;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.queue.TorrentStatus;

public class TorrentJobActionHandler {

	public void onRequestTorrentStateChange(final QueuedTorrentManager torrentManager,
                                            final ObservableList<TorrentView> selectedTorrents,
                                            final TorrentStatus requestedStatus, final Button startButton,
                                            final Button stopButton) {
        startButton.setDisable(requestedStatus == TorrentStatus.ACTIVE);
        stopButton.setDisable(requestedStatus == TorrentStatus.STOPPED);

        selectedTorrents.stream().forEach(tv ->
                torrentManager.requestStatusChange(tv.getInfoHash(), requestedStatus));
	}
}