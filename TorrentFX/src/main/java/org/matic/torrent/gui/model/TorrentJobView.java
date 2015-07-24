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

package org.matic.torrent.gui.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TreeItem;

import org.matic.torrent.queue.QueuedTorrent;

public final class TorrentJobView {
	
	private final List<TrackerView> trackerContents = new ArrayList<>();
	private final TreeItem<TorrentFileEntry> torrentContentTree;
	private final QueuedTorrent queuedTorrent;
	private final String fileName;
	
	private final IntegerProperty priority;

	public TorrentJobView(final QueuedTorrent queuedTorrent, final String fileName,
			final TreeItem<TorrentFileEntry> torrentContents) {
		this.torrentContentTree = torrentContents;
		this.fileName = fileName;
		this.priority = new SimpleIntegerProperty(queuedTorrent.getPriority());
		
		this.queuedTorrent = queuedTorrent;
	}
	
	public final QueuedTorrent getQueuedTorrent() {
		return queuedTorrent;
	}
	
	public final IntegerProperty priorityProperty() {
		return priority;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public TreeItem<TorrentFileEntry> getTorrentContents() {
		return torrentContentTree;
	}
	
	public final List<TrackerView> getTrackerContents() {
		return trackerContents;
	}
}