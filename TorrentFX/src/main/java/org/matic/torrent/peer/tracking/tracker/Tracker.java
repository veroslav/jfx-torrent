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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.matic.torrent.peer.tracking.TrackableTorrent;

/**
 * An abstract representation of a peer tracker (either TCP or UDP).
 * Comparison is made based on next announce time, so that we can
 * correctly schedule next tracker announcement depending on the
 * shortest next announce time.  
 * 
 * @author vedran
 *
 */
public abstract class Tracker implements Comparable<Tracker> {
	
	/* Possible states a tracker can be in:
	 * 	WAITING - Tracker is active and waiting for next announce
	 * 	ANNOUNCING - Waiting for a response to an announcement
	 *  ERROR - Last response was error or the tracker isn't reachable
	 */
	public enum State {
		WAITING, ANNOUNCING, ERROR
	}
	
	/*
	 * Possible states that can be sent on a tracker update
	 */
	public enum Event {				
		STARTED, COMPLETED, STOPPED, NONE		
	}
	
	public enum Type {
		TCP, UDP
	}
		
	protected final String url;
	
	protected final Set<TrackableTorrent> trackedTorrents;
	
	protected long lastResponse;
	protected long nextAnnounce;
	
	/**
	 * Create a peer tracker 
	 * 
	 * @param url The tracker URL
	 * @param responseListener Receiver of the tracker responses
	 */
	protected Tracker(final String url) {		
		this.url = url;
						
		trackedTorrents = new HashSet<>();
		nextAnnounce = System.nanoTime();		
		lastResponse = 0;
	}

	public abstract boolean isScrapeSupported();
	
	public abstract Type getType();
	
	/**
	 * Make an announce request against the tracker
	 * 
	 * @param announceRequest Announcement request
	 */
	protected abstract void announce(final AnnounceRequest announceRequest);
	
	/**
	 * Make a scrape request against the tracker (only if supported)
	 * 
	 * @param torrents Torrents to be scraped
	 */
	protected abstract void scrape(final Set<TrackableTorrent> torrents);

	@Override
	public final int compareTo(final Tracker other) {
		if(this.nextAnnounce < other.nextAnnounce) {
			return -1;
		}
		if(this.nextAnnounce > other.nextAnnounce) {
			return 1;
		}
		return 0;
	}
	
	public void setLastResponse(final long lastResponse) {
		this.lastResponse = lastResponse;
	}
	
	public final boolean addTorrent(final TrackableTorrent torrent) {
		synchronized(trackedTorrents) {
			return trackedTorrents.add(torrent);
		}
	}
	
	public final boolean removeTorrent(final TrackableTorrent torrent) {
		synchronized(trackedTorrents) {
			return trackedTorrents.remove(torrent);
		}
	}
	
	protected final boolean isTracking(final TrackableTorrent torrent) {
		synchronized(trackedTorrents) {
			return trackedTorrents.contains(torrent);
		}		
	}
	
	protected Set<TrackableTorrent> getTorrents() {
		synchronized(trackedTorrents) {
			return trackedTorrents.stream().collect(Collectors.toSet());
		}
	}
	
	protected final void setNextAnnounce(final long nextAnnounce) {
		this.nextAnnounce = nextAnnounce;
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