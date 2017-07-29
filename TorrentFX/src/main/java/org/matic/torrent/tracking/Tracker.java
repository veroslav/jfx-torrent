/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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
 * An abstract representation of a peer tracker (either TCP, UDP or INVALID).
 * 
 * @author Vedran Matic
 *
 */
public abstract class Tracker {
	
	public enum Event {
		STOPPED, STARTED, COMPLETED, UPDATE
	}
	
	public enum Status {
		UNKNOWN, UPDATING, HOSTNAME_NOT_FOUND, WORKING,
		TRACKER_ERROR, INVALID_URL, SCRAPE_OK,
		SCRAPE_NOT_SUPPORTED, CONNECTION_TIMEOUT
	}
	
	public enum Type {
		TCP, UDP, INVALID
	}
	
	public static final long MIN_INTERVAL_DEFAULT_VALUE = 30000;	//30 seconds
	
	private static final String STATUS_SCRAPE_NOT_SUPPORTED_MESSAGE = "scrape not supported";
	private static final String STATUS_HOSTNAME_NOT_FOUND_MESSAGE = "hostname not found";
	private static final String STATUS_CONNECTION_TIMEOUT_MESSAGE = "connect timeout";
	private static final String STATUS_TRACKER_ERROR_MESSAGE = "tracker error";
	private static final String STATUS_INVALID_URL_MESSAGE = "invalid url";
	public static final String STATUS_UPDATING_MESSAGE = "updating...";
	private static final String STATUS_SCRAPE_OK_MESSAGE = "scrape ok";
	public static final String STATUS_WORKING_MESSAGE = "working";
	
	protected static final int NUM_WANTED_PEERS = 200;
	
	private final AtomicInteger scrapeTransactionId = new AtomicInteger(0);
	private final AtomicLong lastResponse = new AtomicLong(0);
	private final AtomicLong lastScrape = new AtomicLong(0);	
	private final String url;
	
	/**
	 * Create a tracker 
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
	
	public static String getStatusMessage(final Status status) {
		switch(status) {
			case UPDATING:
				return STATUS_UPDATING_MESSAGE;
			case HOSTNAME_NOT_FOUND:
				return STATUS_HOSTNAME_NOT_FOUND_MESSAGE;
			case WORKING:
				return STATUS_WORKING_MESSAGE;
			case INVALID_URL:
				return STATUS_INVALID_URL_MESSAGE;
			case SCRAPE_OK:
				return STATUS_SCRAPE_OK_MESSAGE;
			case SCRAPE_NOT_SUPPORTED:
				return STATUS_SCRAPE_NOT_SUPPORTED_MESSAGE;
			case CONNECTION_TIMEOUT:
				return STATUS_CONNECTION_TIMEOUT_MESSAGE;
			case TRACKER_ERROR:
				return STATUS_TRACKER_ERROR_MESSAGE;
			default:
				return "";
		}
	}
	
	public String getUrl() {
		return url;
	}
	
	public long getLastResponse() {
		return lastResponse.get();
	}
	
	public void setLastResponse(final long lastResponse) {
		this.lastResponse.set(lastResponse);
	}
	
	public long getLastScrape() {
		return lastScrape.get();
	}

	public void setLastScrape(final long lastScrape) {
		this.lastScrape.set(lastScrape);
	}
	
	public int getScrapeTransactionId() {
		return scrapeTransactionId.get();
	}
	
	public void setScrapeTransactionId(final int scrapeTransactionId) {
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