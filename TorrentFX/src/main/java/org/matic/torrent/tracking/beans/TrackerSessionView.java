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
package org.matic.torrent.tracking.beans;

import org.matic.torrent.queue.TorrentStatus;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerSession;

public final class TrackerSessionView extends TrackableSessionView {
	
	private static final String TRACKER_MESSAGE_NULL = "null";		//Sometimes returned as a tracker message
	private Tracker.Status trackerStatus;		

	public TrackerSessionView(final TrackerSession trackerSession) {
		super(trackerSession);
		updateValues();
	}
	
	@Override
	public void updateValues() {
		minInterval = trackerSession.getMinInterval();
		interval = trackerSession.getInterval();
		
		downloaded = trackerSession.getDownloaded();
		leechers = trackerSession.getLeechers();
		seeders = trackerSession.getSeeders();
		
		trackerStatus = trackerSession.getTrackerStatus();
		lastTrackerResponse = trackerStatus == Tracker.Status.CONNECTION_TIMEOUT?
				trackerSession.getTracker().getLastResponse() : trackerSession.getLastAnnounceResponse();
		nextUpdateValue = trackerSession.getInterval() - (System.currentTimeMillis() - lastTrackerResponse);
		
		final TorrentStatus torrentStatus = trackerSession.getTorrent().getStatus();
		final String trackerMessage = trackerSession.getTrackerMessage();
		final String statusMessage = trackerMessage != null && !trackerMessage.equals(TRACKER_MESSAGE_NULL)?
				trackerMessage : Tracker.getStatusMessage(trackerStatus);
		
		final boolean isTrackerScraped = trackerStatus == Tracker.Status.SCRAPE_OK ||
				trackerStatus == Tracker.Status.SCRAPE_NOT_SUPPORTED;

		displayedMessage = "";
		if((trackerStatus != Tracker.Status.UPDATING && nextUpdateValue >= 1000 &&
				(torrentStatus == TorrentStatus.ACTIVE))) {
			displayedMessage = statusMessage;
		}
		else if(torrentStatus == TorrentStatus.STOPPED && isTrackerScraped) {
			displayedMessage = Tracker.getStatusMessage(trackerStatus);
		}
	}
	
	@Override
	public String getName() {
		return trackerSession.getTracker().getUrl();
	}
}