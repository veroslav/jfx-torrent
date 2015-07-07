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

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.peer.ClientProperties;

public final class TrackedTorrent {

	private volatile Tracker.Event lastTrackerEvent;
	
	private final InfoHash infoHash;	
	private final Tracker tracker;
	
	private volatile int leechers = 0;
	private volatile int seeders = 0;	
			
	private volatile long lastTrackerResponse = 0;
	private volatile long interval = 0;
	private Long minInterval = null;	
	
	private int transactionId = ClientProperties.generateUniqueId();
	
	//TODO: Move connectionAttempts to UdpTracker?
	private int connectionAttempts = 0;

	public TrackedTorrent(final InfoHash infoHash, final Tracker tracker) {
		this.infoHash = infoHash;
		this.tracker = tracker;
		
		lastTrackerEvent = Tracker.Event.STOPPED;
	}
	
	public final int updateConnectionAttempts() {
		return ++connectionAttempts;
	}

	public final void setTransactionId(final int transactionId) {
		this.transactionId = transactionId;
	}
	
	public final int getLeechers() {
		return leechers;
	}

	public final void setLeechers(final int leechers) {
		this.leechers = leechers;
	}

	public final int getSeeders() {
		return seeders;
	}

	public final void setSeeders(final int seeders) {
		this.seeders = seeders;
	}

	public final Long getMinInterval() {
		return minInterval;
	}

	public final void setMinInterval(final Long minInterval) {
		this.minInterval = minInterval;
	}

	public final long getInterval() {
		return interval;
	}

	public final void setInterval(final long interval) {
		this.interval = interval;
	}

	public long getLastTrackerResponse() {
		return lastTrackerResponse;
	}

	public void setLastTrackerResponse(final long lastTrackerResponse) {
		this.lastTrackerResponse = lastTrackerResponse;
		connectionAttempts = 0;
	}
	
	public final int getTransactionId() {
		return transactionId;
	}
	
	public InfoHash getInfoHash() {
		return infoHash;
	}
	
	public Tracker.Event getLastTrackerEvent() {
		return lastTrackerEvent;
	}

	public void setLastTrackerEvent(final Tracker.Event lastTrackerEvent) {
		this.lastTrackerEvent = lastTrackerEvent;
	}

	public Tracker getTracker() {
		return tracker;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((infoHash == null) ? 0 : infoHash.hashCode());
		result = prime * result + ((tracker == null) ? 0 : tracker.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackedTorrent other = (TrackedTorrent) obj;
		if (infoHash == null) {
			if (other.infoHash != null)
				return false;
		} else if (!infoHash.equals(other.infoHash))
			return false;
		if (tracker == null) {
			if (other.tracker != null)
				return false;
		} else if (!tracker.equals(other.tracker))
			return false;
		return true;
	}
}