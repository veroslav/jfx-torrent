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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An abstract representation of a peer tracker (either TCP or UDP).
 * 
 * @author vedran
 *
 */
public abstract class Tracker {
	
	public enum Event {
		STOPPED, STARTED, COMPLETED, UPDATE
	}
	
	public enum Status {
		DISABLED, NOT_ALLOWED, ERROR, WORKING, SCRAPE_OK, SCRAPE_NOT_SUPPORTED, UNKNOWN
	}
	
	public enum Type {
		TCP, UDP
	}
	
	protected static final int NUM_WANTED_PEERS = 200;
	
	private final AtomicInteger scrapeTransactionId = new AtomicInteger(0);
	private final AtomicLong lastResponse = new AtomicLong(0);
	private final AtomicLong lastScrape = new AtomicLong(0);	
	private final String url;
	
	/**
	 * Create a peer tracker 
	 * 
	 * @param url The tracker URL
	 */
	protected Tracker(final String url) {		
		this.url = url;	
	}

	public abstract boolean isScrapeSupported();
	
	public abstract Type getType();
	
	public abstract long getId();
	
	public abstract void setId(final long id);
	
	public String getUrl() {
		return url;
	}
	
	public long getLastResponse() {
		return lastResponse.get();
	}
	
	public void setLastResponse(final long lastResponse) {
		this.lastResponse.set(lastResponse);
	}
	
	public final long getLastScrape() {
		return lastScrape.get();
	}

	public void setLastScrape(final long lastScrape) {
		this.lastScrape.set(lastScrape);
		
	}
	
	public final int getScrapeTransactionId() {
		return scrapeTransactionId.get();
	}
	
	public final void setScrapeTransactionId(final int scrapeTransactionId) {
		this.scrapeTransactionId.set(scrapeTransactionId);
	}

	/**
	 * Make an announce request against the tracker
	 * 
	 * @param announceParameters Announcement parameters
	 * @param trackerSession Tracked torrent's session
	 */
	protected abstract void announce(final AnnounceParameters announceParameters,
			final TrackerSession trackerSession);
	
	/**
	 * Make a scrape request against the tracker (only if supported)
	 * 
	 * @param trackerSessions Torrent sessions to be scraped
	 */
	protected abstract void scrape(final TrackerSession... trackerSessions);

	/**
	 * Send a connection request to this tracker (only supported by UDP trackers)
	 * 
	 * @param transactionId Caller's transaction id
	 * @return Connection attempt count
	 */
	protected abstract int connect(final int transactionId);

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