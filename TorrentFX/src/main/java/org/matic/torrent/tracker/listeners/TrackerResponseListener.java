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

package org.matic.torrent.tracker.listeners;

import org.matic.torrent.tracker.Tracker;
import org.matic.torrent.tracker.TrackerResponse;

/**
 * A listener for receiving and managing tracker responses
 * 
 * @author vedran
 *
 */
public interface TrackerResponseListener {

	/**
	 * Handle a tracker response when it has been received  
	 * 
	 * @param response Tracker response to handle
	 * @param tracker Originating tracker
	 */
	void onResponseReceived(final TrackerResponse response, final Tracker tracker);
}