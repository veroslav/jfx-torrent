/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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

import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.peer.ClientProperties;

import java.util.concurrent.atomic.AtomicLong;

public final class TrackerSession extends TrackableSession {

	private Tracker.Event lastAcknowledgedTrackerEvent = Tracker.Event.STOPPED;
	private final Tracker tracker;
	
	private Tracker.Status trackerStatus = Tracker.Status.UNKNOWN;
	private String trackerMessage = null;

	private final AtomicLong lastAnnounceResponse = new AtomicLong(0);
	private final AtomicLong lastScrapeResponse = new AtomicLong(0);
		
	private final int key = ClientProperties.generateUniqueId();
	private final int transactionId = ClientProperties.generateUniqueId();	

	public TrackerSession(final TorrentView torrentView, final Tracker tracker) {
        super(torrentView);
		this.tracker = tracker;
        super.minInterval.set(Tracker.MIN_INTERVAL_DEFAULT_VALUE);
	}

    @Override
    public String getStatus() {
        return Tracker.getStatusMessage(trackerStatus);
    }
	
	public synchronized void setTrackerMessage(final String trackerMessage) {
		this.trackerMessage = trackerMessage;
	}
	
	public synchronized String getTrackerMessage() {
		return trackerMessage;
	}
	
	public void setTrackerStatus(final Tracker.Status trackerStatus) {
		synchronized(this.trackerStatus) {
			this.trackerStatus = trackerStatus;
		}
	}
	
	public Tracker.Status getTrackerStatus() {
		synchronized(this.trackerStatus) {
			return trackerStatus;
		}
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
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public int getKey() {
		return key;
	}
	
	public TorrentView getTorrentView() {
		return torrentView;
	}	
	
	/**
	 * Return the last announced tracker event acknowledged by the tracker
	 * 
	 * @return Tracker event
	 */
	public Tracker.Event getLastAcknowledgedEvent() {
		synchronized(lastAcknowledgedTrackerEvent) {
			return lastAcknowledgedTrackerEvent;
		}
	}

	public void setLastAcknowledgedEvent(final Tracker.Event trackerEvent) {
		synchronized(lastAcknowledgedTrackerEvent) {			
			this.lastAcknowledgedTrackerEvent = trackerEvent;
		}
	}

	public Tracker getTracker() {
		return tracker;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((torrentView == null) ? 0 : torrentView.hashCode());
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
		if (torrentView == null) {
			if (other.torrentView != null)
				return false;
		} else if (!torrentView.equals(other.torrentView))
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
        return "TrackerSession{" +
                "lastAcknowledgedTrackerEvent=" + lastAcknowledgedTrackerEvent +
                ", tracker=" + tracker +
                ", trackerStatus=" + trackerStatus +
                ", trackerMessage='" + trackerMessage + '\'' +
                ", lastAnnounceResponse=" + lastAnnounceResponse +
                ", lastScrapeResponse=" + lastScrapeResponse +
                ", key=" + key +
                ", transactionId=" + transactionId +
                '}';
    }
}