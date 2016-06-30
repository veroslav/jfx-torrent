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

import org.matic.torrent.queue.TorrentStatus;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerSession;

public final class TrackerView extends TrackableView {

    private static final String TRACKER_MESSAGE_NULL = "null";		//Sometimes returned as a tracker message
    private Tracker.Status trackerStatus;

	private final TrackerSession trackerSession;

	public TrackerView(final TrackerSession trackerSession) {
        super(trackerSession);
		this.trackerSession = trackerSession;
	}

    @Override
    public boolean isUserManaged() {
        return true;
    }

    @Override
    public void update() {
        super.minInterval.set(super.trackableSession.getMinInterval());
        super.interval.set(super.trackableSession.getInterval());
        super.downloaded.set(super.trackableSession.getDownloaded());
        super.leechers.set(super.trackableSession.getLeechers());
        super.seeders.set(super.trackableSession.getSeeders());

        trackerStatus = trackerSession.getTrackerStatus();
        lastResponse = trackerStatus == Tracker.Status.CONNECTION_TIMEOUT?
                trackerSession.getTracker().getLastResponse() : trackerSession.getLastAnnounceResponse();
        super.nextUpdate.set(super.trackableSession.getInterval() - (System.currentTimeMillis() - lastResponse));

        final TorrentStatus torrentStatus = super.trackableSession.getTorrentView().getStatus();
        final String trackerMessage = trackerSession.getTrackerMessage();
        final String statusMessage = trackerMessage != null && !trackerMessage.equals(TRACKER_MESSAGE_NULL)?
                trackerMessage : Tracker.getStatusMessage(trackerStatus);

        final boolean isTrackerScraped = trackerStatus == Tracker.Status.SCRAPE_OK ||
                trackerStatus == Tracker.Status.SCRAPE_NOT_SUPPORTED;

        String displayedMessage = "";
        if((trackerStatus != Tracker.Status.UPDATING && super.getNextUpdate() >= 1000 &&
                (torrentStatus == TorrentStatus.ACTIVE))) {
            displayedMessage = statusMessage;
        }
        else if(torrentStatus == TorrentStatus.STOPPED && isTrackerScraped) {
            displayedMessage = Tracker.getStatusMessage(trackerStatus);
        }

        super.status.set(displayedMessage);
    }
    @Override
    public String getName() {
        return trackerSession.getTracker().getUrl();
    }
}