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
package org.matic.torrent.gui.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.TorrentStatus;
import org.matic.torrent.tracking.beans.TrackableSessionView;

public final class TrackerView {

	private final IntegerProperty downloaded = new SimpleIntegerProperty(0);
	private final IntegerProperty leechers = new SimpleIntegerProperty(0);
	private final IntegerProperty seeds = new SimpleIntegerProperty(0);
	private final LongProperty minInterval = new SimpleLongProperty(0);
	private final LongProperty nextUpdate = new SimpleLongProperty(0);
	private final LongProperty interval = new SimpleLongProperty(0);

	private final StringProperty status = new SimpleStringProperty();
	private final StringProperty trackerName;
	
	private final TrackableSessionView trackableSessionView;

	private long lastUserRequestedUpdate = 0;
	private long lastTrackerResponse = 0;

	public TrackerView(final TrackableSessionView trackableSessionView) {
		this.trackableSessionView = trackableSessionView;
		this.trackerName = new SimpleStringProperty(trackableSessionView.getName());
	}
	
	public void update() {
        trackableSessionView.updateValues();
		this.lastTrackerResponse = trackableSessionView.getLastTrackerResponse();
		//setTorrentStatus(trackableSessionView.getTorrent().getStatus());
		status.set(trackableSessionView.getStatus());
		nextUpdate.set(trackableSessionView.getNextUpdate());
		interval.set(trackableSessionView.getInterval());
		minInterval.set(trackableSessionView.getMinInterval());
		downloaded.set(trackableSessionView.getDownloaded());
		leechers.set(trackableSessionView.getLeechers());
		seeds.set(trackableSessionView.getSeeders());
	}
	
	public void setLastTrackerResponse(final long lastTrackerResponse) {
		this.lastTrackerResponse = lastTrackerResponse;
	}
	
	public void setStatus(final String status) {
		this.status.set(status);
	}
	
	public long getLastUserRequestedUpdate() {
		return lastUserRequestedUpdate;
	}
	
	/*public void setLastUserRequestedUpdate(final long lastUserRequestedUpdate) {
		this.lastUserRequestedUpdate = lastUserRequestedUpdate;
	}*/

	public QueuedTorrent getTorrent() {
		return trackableSessionView.getTorrent();
	}
		
	public TorrentStatus getTorrentStatus() {
		return trackableSessionView.getTorrent().getStatus();
	}
	
	/*public void setTorrentStatus(final TorrentStatus torrentStatus) {
		this.trackableSessionView.getTorrent().getProgress().setStatus(torrentStatus);
	}*/

	public String getStatus() {
		return status.get();
	}
	
	public StringProperty statusProperty() {
		return status;
	}

	public long getLastTrackerResponse() {
		return lastTrackerResponse;
	}

	public long getMinInterval() {
		return minInterval.get();
	}
	
	public LongProperty minIntervalProperty() {
		return minInterval;
	}

	public long getInterval() {
		return interval.get();
	}
	
	public LongProperty intervalProperty() {
		return interval;
	}

	public int getDownloaded() {
		return downloaded.get();
	}
	
	public IntegerProperty downloadedProperty() {
		return downloaded;
	}
	
	public long getNextUpdate() {
		return nextUpdate.get();
	}
	
	public LongProperty nextUpdateProperty() {
		return nextUpdate;
	}
	
	public int getLeechers() {
		return leechers.get();
	}
	
	public IntegerProperty leechersProperty() {
		return leechers;
	}
	
	public int getSeeds() {
		return seeds.get();
	}
	
	public IntegerProperty seedsProperty() {
		return seeds;
	}
	
	public String getTrackerName() {
		return trackerName.get();
	}
	
	public StringProperty trackerNameProperty() {
		return trackerName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((trackerName == null) ? 0 : trackerName.hashCode());
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
		TrackerView other = (TrackerView) obj;
		if (trackerName == null) {
			if (other.trackerName != null)
				return false;
		} else if (!trackerName.equals(other.trackerName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackerView [downloaded=" + downloaded + ", leechers="
				+ leechers + ", seeds=" + seeds + ", minInterval="
				+ minInterval + ", nextUpdate=" + nextUpdate + ", interval="
				+ interval + ", status=" + status + ", trackerName="
				+ trackerName + ", trackableSessionView=" + trackableSessionView
				+ ", lastUserRequestedUpdate=" + lastUserRequestedUpdate
				+ ", lastTrackerResponse=" + lastTrackerResponse + "]";
	}
}