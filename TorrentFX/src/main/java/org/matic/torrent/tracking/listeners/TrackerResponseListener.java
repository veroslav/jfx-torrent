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

package org.matic.torrent.tracking.listeners;

import org.matic.torrent.tracking.AnnounceResponse;
import org.matic.torrent.tracking.ScrapeResponse;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerSession;

/**
 * A listener for receiving and managing tracker responses
 * 
 * @author vedran
 *
 */
public interface TrackerResponseListener {

	/**
	 * Handle a tracker announce response  
	 * 
	 * @param announceResponse Tracker announce response
	 * @param trackerSession Tracker request submitter session
	 */
	void onAnnounceResponseReceived(final AnnounceResponse announceResponse, final TrackerSession trackerSession);
	
	/**
	 * Handle a tracker scrape response
	 * 
	 * @param tracker Responding tracker
	 * @param scrapeResponse Tracker scrape response
	 */
	void onScrapeResponseReceived(final Tracker tracker, final ScrapeResponse scrapeResponse);
}