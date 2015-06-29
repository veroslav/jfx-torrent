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

package org.matic.torrent.tracking;

import java.util.Set;

import org.matic.torrent.hash.InfoHash;

/**
 * An abstract representation of a peer tracker (either TCP or UDP).
 * Comparison is made based on next announce time, so that we can
 * correctly schedule next tracker announcement depending on the
 * shortest next announce time.  
 * 
 * @author vedran
 *
 */
public abstract class Tracker {
	
	public enum Event {
		STOPPED, STARTED, COMPLETED, UPDATE
	}
	
	public enum Type {
		TCP, UDP
	}
	
	protected volatile long lastResponse;
	private final String url;
	
	/**
	 * Create a peer tracker 
	 * 
	 * @param url The tracker URL
	 */
	protected Tracker(final String url) {		
		this.url = url;	
		lastResponse = 0;
	}

	public abstract boolean isScrapeSupported();
	
	public abstract Type getType();
	
	public String getUrl() {
		return url;
	}
	
	/**
	 * Make an announce request against the tracker
	 * 
	 * @param announceParameters Announcement parameters
	 * @param trackedTorrent Tracked torrent
	 */
	protected abstract void announce(final AnnounceParameters announceParameters,
			final TorrentTracker trackedTorrent);
	
	/**
	 * Make a scrape request against the tracker (only if supported)
	 * 
	 * @param torrents Torrents to be scraped
	 */
	protected abstract void scrape(final Set<InfoHash> torrentInfoHashes);

	public void setLastResponse(final long lastResponse) {
		this.lastResponse = lastResponse;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tracker other = (Tracker) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
}