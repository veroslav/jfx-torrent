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

package org.matic.torrent.peer.tracking.tracker;

import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class AnnounceRequest {

	private final Tracker.Event event;
	private final TrackableTorrent torrent;
	private final long downloaded;
	private final long uploaded;
	private final long left;

	public AnnounceRequest(final TrackableTorrent torrent, final Tracker.Event event, 
			final long uploaded, final long downloaded, final long left) {
		this.downloaded = downloaded;
		this.uploaded = uploaded;
		this.torrent = torrent;
		this.event = event;		
		this.left = left;		
	}
	
	public final Tracker.Event getEvent() {
		return event;
	}
	
	public final TrackableTorrent getTorrent() {
		return torrent;
	}
	
	public final long getUploaded() {
		return uploaded;
	}
	
	public final long getDownloaded() {
		return downloaded;
	}
	
	public final long getLeft() {
		return left;
	}
}