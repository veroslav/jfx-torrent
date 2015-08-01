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

import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.QueuedTorrent;

public final class TrackerSession {

	private volatile Tracker.Event lastTrackerEvent = Tracker.Event.STOPPED;
	
	private final QueuedTorrent queuedTorrent;	
	private final Tracker tracker;
	
	private Tracker.Status trackerStatus = Tracker.Status.UNKNOWN;
	private String trackerMessage = null;
	
	private AtomicInteger downloaded = new AtomicInteger(0);
	private AtomicInteger leechers = new AtomicInteger(0);
	private AtomicInteger seeders = new AtomicInteger(0);	
			
	private AtomicLong minInterval = new AtomicLong(Tracker.MIN_INTERVAL_DEFAULT_VALUE);
	private AtomicLong lastAnnounceResponse = new AtomicLong(0);
	private AtomicLong lastScrapeResponse = new AtomicLong(0);
	private AtomicLong interval = new AtomicLong(0);	
	
	private int transactionId = ClientProperties.generateUniqueId();	

	public TrackerSession(final QueuedTorrent queuedTorrent, final Tracker tracker) {
		this.queuedTorrent = queuedTorrent;
		this.tracker = tracker;
	}
	
	public final synchronized void setTrackerMessage(final String trackerMessage) {
		this.trackerMessage = trackerMessage;
	}
	
	public final synchronized String getTrackerMessage() {
		return trackerMessage;
	}
	
	public final void setTrackerStatus(final Tracker.Status trackerStatus) {
		synchronized(this.trackerStatus) {
			this.trackerStatus = trackerStatus;
		}
	}
	
	public final Tracker.Status getTrackerStatus() {
		synchronized(this.trackerStatus) {
			return trackerStatus;
		}
	}
	
	public final void setTransactionId(final int transactionId) {
		this.transactionId = transactionId;
	}
	
	public final int getDownloaded() {
		return downloaded.get();
	}

	public final void setDownloaded(final int downloaded) {
		this.downloaded.set(downloaded);
	}

	public final int getLeechers() {
		return leechers.get();
	}

	public final void setLeechers(final int leechers) {
		this.leechers.set(leechers);
	}

	public final int getSeeders() {
		return seeders.get();
	}

	public final void setSeeders(final int seeders) {
		this.seeders.set(seeders);
	}

	public final long getMinInterval() {
		return minInterval.get();
	}

	public final void setMinInterval(final long minInterval) {
		this.minInterval.set(minInterval);
	}

	public final long getInterval() {
		return interval.get();
	}

	public final void setInterval(final long interval) {
		this.interval.set(interval);
	}

	public long getLastTrackerResponse() {
		return Math.max(lastAnnounceResponse.get(), lastScrapeResponse.get());
	}
	
	public long getLastAnnounceResponse() {
		return lastAnnounceResponse.get();
	}

	public void setLastAnnounceResponse(final long lastAnnounceResponse) {
		this.lastAnnounceResponse.set(lastAnnounceResponse);		
	}
	
	public void setLastScrapeResponse(final long lastScrapeResponse) {
		this.lastScrapeResponse.set(lastScrapeResponse);		
	}
	
	public final int getTransactionId() {
		return transactionId;
	}
	
	public QueuedTorrent getTorrent() {
		return queuedTorrent;
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
				+ ((queuedTorrent == null) ? 0 : queuedTorrent.hashCode());
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
		TrackerSession other = (TrackerSession) obj;
		if (queuedTorrent == null) {
			if (other.queuedTorrent != null)
				return false;
		} else if (!queuedTorrent.equals(other.queuedTorrent))
			return false;
		if (tracker == null) {
			if (other.tracker != null)
				return false;
		} else if (!tracker.equals(other.tracker))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackerSession [infoHash=" + queuedTorrent + ", tracker=" + tracker
				+ "]";
	}
}