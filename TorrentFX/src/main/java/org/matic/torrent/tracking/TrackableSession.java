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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.matic.torrent.gui.model.TorrentView;

public abstract class TrackableSession {

    protected final AtomicInteger downloaded = new AtomicInteger(0);
    protected final AtomicLong minInterval = new AtomicLong();
    protected final AtomicLong interval = new AtomicLong(0);

    protected final AtomicInteger leechers = new AtomicInteger(0);
    protected final AtomicInteger seeders = new AtomicInteger(0);

    protected TorrentView torrentView;

    protected TrackableSession(final TorrentView torrentView) {
        this.torrentView = torrentView;
    }

    public abstract String getStatus();

    public TorrentView getTorrentView() {
        return torrentView;
    }

    public int getDownloaded() {
        return downloaded.get();
    }

    public void setDownloaded(final int downloaded) {
        this.downloaded.set(downloaded);
    }

    public int getLeechers() {
        return leechers.get();
    }

    public void setLeechers(final int leechers) {
        this.leechers.set(leechers);
    }

    public int getSeeders() {
        return seeders.get();
    }

    public void setSeeders(final int seeders) {
        this.seeders.set(seeders);
    }

    public long getInterval() {
        return interval.get();
    }

    public void setInterval(final long interval) {
        this.interval.set(interval);
    }

    public void setMinInterval(final long minInterval) {
        this.minInterval.set(minInterval);
    }

    public long getMinInterval() {
        return minInterval.get();
    }
}