/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015 Vedran Matic
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

import org.matic.torrent.gui.model.TorrentJobView;
import org.matic.torrent.gui.table.TorrentJobTable;
import org.matic.torrent.queue.QueuedTorrent;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;

public class TorrentJobActionHandler {

	public void onChangeTorrentState(final QueuedTorrent.State newStatus, final Button startButton,
			final Button stopButton, final TorrentJobTable torrentJobTable) {
		startButton.setDisable(newStatus == QueuedTorrent.State.ACTIVE);
		stopButton.setDisable(newStatus == QueuedTorrent.State.STOPPED);
		
		final ObservableList<TorrentJobView> selectedTorrentJobs = torrentJobTable.getSelectedJobs();
		
		if(selectedTorrentJobs.size() > 0) {
			selectedTorrentJobs.stream().map(
					TorrentJobView::getQueuedTorrent).forEach(t -> t.setState(newStatus));			
		}
	}
}