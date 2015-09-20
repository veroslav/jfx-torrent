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

package org.matic.torrent.gui.model;

import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.tracking.beans.TrackableSessionViewBean;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class TrackerView {

	private final IntegerProperty downloaded = new SimpleIntegerProperty(0);
	private final IntegerProperty leechers = new SimpleIntegerProperty(0);
	private final IntegerProperty seeds = new SimpleIntegerProperty(0);
	private final LongProperty minInterval = new SimpleLongProperty(0);
	private final LongProperty nextUpdate = new SimpleLongProperty(0);
	private final LongProperty interval = new SimpleLongProperty(0);

	private final StringProperty status = new SimpleStringProperty();
	private final StringProperty trackerName;
	
	private final TrackableSessionViewBean trackerViewBean;

	private long lastUserRequestedUpdate = 0;
	private long lastTrackerResponse = 0;

	public TrackerView(final TrackableSessionViewBean trackerViewBean) {
		this.trackerViewBean = trackerViewBean;
		this.trackerName = new SimpleStringProperty(trackerViewBean.getName());
	}
	
	public final void update() {
		trackerViewBean.updateValues();
		this.lastTrackerResponse = trackerViewBean.getLastTrackerResponse();
		setTorrentState(trackerViewBean.getTorrent().getProperties().getState());
		status.set(trackerViewBean.getStatus());
		nextUpdate.set(trackerViewBean.getNextUpdate());
		interval.set(trackerViewBean.getInterval());
		minInterval.set(trackerViewBean.getMinInterval());		
		downloaded.set(trackerViewBean.getDownloaded());
		leechers.set(trackerViewBean.getLeechers());
		seeds.set(trackerViewBean.getSeeders());		
	}
	
	public final void setLastTrackerResponse(final long lastTrackerResponse) {
		this.lastTrackerResponse = lastTrackerResponse;
	}
	
	public void setStatus(final String status) {
		this.status.set(status);
	}
	
	public final long getLastUserRequestedUpdate() {
		return lastUserRequestedUpdate;
	}
	
	public final void setLastUserRequestedUpdate(final long lastUserRequestedUpdate) {
		this.lastUserRequestedUpdate = lastUserRequestedUpdate;
	}

	public QueuedTorrent getTorrent() {
		return trackerViewBean.getTorrent();
	}
		
	public QueuedTorrent.State getTorrentState() {
		return trackerViewBean.getTorrent().getProperties().getState();
	}
	
	public void setTorrentState(final QueuedTorrent.State torrentState) {
		this.trackerViewBean.getTorrent().getProperties().setState(torrentState);
	}

	public String getStatus() {
		return status.get();
	}
	
	public final StringProperty statusProperty() {
		return status;
	}

	public long getLastTrackerResponse() {
		return lastTrackerResponse;
	}

	public final long getMinInterval() {
		return minInterval.get();
	}
	
	public final LongProperty minIntervalProperty() {
		return minInterval;
	}

	public final long getInterval() {
		return interval.get();
	}
	
	public final LongProperty intervalProperty() {
		return interval;
	}

	public final int getDownloaded() {
		return downloaded.get();
	}
	
	public final IntegerProperty downloadedProperty() {
		return downloaded;
	}
	
	public long getNextUpdate() {
		return nextUpdate.get();
	}
	
	public final LongProperty nextUpdateProperty() {
		return nextUpdate;
	}
	
	public int getLeechers() {
		return leechers.get();
	}
	
	public final IntegerProperty leechersProperty() {
		return leechers;
	}
	
	public int getSeeds() {
		return seeds.get();
	}
	
	public final IntegerProperty seedsProperty() {
		return seeds;
	}
	
	public String getTrackerName() {
		return trackerName.get();
	}
	
	public final StringProperty trackerNameProperty() {
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
				+ trackerName + ", trackerViewBean=" + trackerViewBean
				+ ", lastUserRequestedUpdate=" + lastUserRequestedUpdate
				+ ", lastTrackerResponse=" + lastTrackerResponse + "]";
	}
}