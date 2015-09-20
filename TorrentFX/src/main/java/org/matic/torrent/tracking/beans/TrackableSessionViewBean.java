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
package org.matic.torrent.tracking.beans;

import org.matic.torrent.queue.QueuedTorrent;

public abstract class TrackableSessionViewBean {
	
	protected String displayedMessage;
	protected long lastTrackerResponse;
	protected long nextUpdateValue;
	protected long minInterval;
	protected long interval;
	
	protected int downloaded;
	protected int leechers;
	protected int seeders;
	
	protected final QueuedTorrent torrent;
	
	protected TrackableSessionViewBean(final QueuedTorrent torrent) {
		this.torrent = torrent;
	}
	
	public abstract void updateValues();	
	public abstract String getName();
	
	public final QueuedTorrent getTorrent() {
		return torrent;
	}
	
	public final String getStatus() {
		return displayedMessage;
	}
	
	public final long getLastTrackerResponse() {
		return lastTrackerResponse;
	}
	
	public final long getNextUpdate() {
		return nextUpdateValue;
	}
	
	public final long getInterval() {
		return interval;
	}
	
	public final long getMinInterval() {
		return minInterval;
	}
	
	public final int getDownloaded() {
		return downloaded;
	}
	
	public final int getLeechers() {
		return leechers;
	}
	
	public final int getSeeders() {
		return seeders;
	}
}