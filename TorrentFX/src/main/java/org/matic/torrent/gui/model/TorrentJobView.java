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

import org.matic.torrent.queue.QueuedTorrent;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class TorrentJobView {
	
	private final List<TrackerView> trackerContents = new ArrayList<>();
	private final QueuedTorrent queuedTorrent;
	private final String fileName;
	
	private final IntegerProperty priority;

	public TorrentJobView(final QueuedTorrent queuedTorrent, final String fileName) {		
		this.fileName = fileName;
		this.priority = new SimpleIntegerProperty(queuedTorrent.getProgress().getPriority());
		
		this.queuedTorrent = queuedTorrent;
	}
	
	public final QueuedTorrent getQueuedTorrent() {
		return queuedTorrent;
	}
	
	public final IntegerProperty priorityProperty() {
		return priority;
	}
	
	public final int getPriority() {
		return priority.get();
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public final List<TrackerView> getTrackerContents() {
		return trackerContents;
	}
}