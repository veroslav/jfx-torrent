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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class TrackerView {
	
	private final StringProperty trackerName;
	private final StringProperty status;
	
	private final LongProperty minInterval;
	private final LongProperty interval;
	
	private final IntegerProperty downloaded;
	private final IntegerProperty leechers;
	private final IntegerProperty seeds;
	
	private final LongProperty nextUpdate;

	public TrackerView(final String trackerName) {
		this.minInterval = new SimpleLongProperty(0);
		this.trackerName = new SimpleStringProperty(trackerName);
		this.downloaded = new SimpleIntegerProperty(0);
		this.nextUpdate = new SimpleLongProperty(0);
		this.interval = new SimpleLongProperty(0);
		this.leechers = new SimpleIntegerProperty(0);
		this.status = new SimpleStringProperty("");
		this.seeds = new SimpleIntegerProperty(0);				
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
	
	public String getStatus() {
		return status.get();
	}
	
	public final StringProperty statusProperty() {
		return status;
	}
	
	public String getTrackerName() {
		return trackerName.get();
	}
	
	public final StringProperty trackerNameProperty() {
		return trackerName;
	}
}