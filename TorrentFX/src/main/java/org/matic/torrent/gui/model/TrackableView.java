/* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
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
import org.matic.torrent.tracking.TrackableSession;

public abstract class TrackableView {

    protected final IntegerProperty downloaded = new SimpleIntegerProperty(0);
    protected final IntegerProperty leechers = new SimpleIntegerProperty(0);
    protected final IntegerProperty seeders = new SimpleIntegerProperty(0);

    protected final LongProperty minInterval = new SimpleLongProperty(0);
    protected final LongProperty nextUpdate = new SimpleLongProperty(0);
    protected final LongProperty interval = new SimpleLongProperty(0);

    protected final StringProperty status = new SimpleStringProperty();

    protected long lastUserRequestedUpdate = 0;
    protected long lastResponse;

    protected final TrackableSession trackableSession;

    protected TrackableView(final TrackableSession trackerSession) {
        this.trackableSession = trackerSession;
    }

    public abstract void update();

    public abstract String getName();

    public abstract boolean isUserManaged();

    public final TorrentView getTorrentView() {
        return trackableSession.getTorrentView();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(final String status) {
        this.status.set(status);
    }

    public final long getLastResponse() {
        return lastResponse;
    }

    public void setLastUserRequestedUpdate(final long lastUserRequestedUpdate) {
        this.lastUserRequestedUpdate = lastUserRequestedUpdate;
    }

    public long getLastUserRequestedUpdate() {
        return lastUserRequestedUpdate;
    }

    public LongProperty nextUpdateProperty() {
        return nextUpdate;
    }

    public final long getNextUpdate() {
        return nextUpdate.get();
    }

    public LongProperty intervalProperty() {
        return interval;
    }

    public final long getInterval() {
        return interval.get();
    }

    public LongProperty minIntervalProperty() {
        return minInterval;
    }

    public final long getMinInterval() {
        return minInterval.get();
    }

    public IntegerProperty downloadedProperty() {
        return downloaded;
    }

    public final int getDownloaded() {
        return downloaded.get();
    }

    public IntegerProperty leechersProperty() {
        return leechers;
    }

    public final int getLeechers() {
        return leechers.get();
    }

    public IntegerProperty seedsProperty() {
        return seeders;
    }

    public final int getSeeders() {
        return seeders.get();
    }
}