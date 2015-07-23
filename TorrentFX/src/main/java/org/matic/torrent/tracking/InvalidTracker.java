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

package org.matic.torrent.tracking;

/**
 * A dummy tracker to be used when resolving the tracker URL gives an error.
 * This might be due to an unsupported protocol or an invalid URL.
 * 
 * @author vedran
 *
 */
public final class InvalidTracker extends Tracker {

	/**
	 * @see Tracker#Tracker(String)
	 */
	protected InvalidTracker(final String url) {
		super(url);
	}

	/**
	 * @see Tracker#isScrapeSupported()
	 */
	@Override
	public boolean isScrapeSupported() {
		return false;
	}

	/**
	 * @see Tracker#getType()
	 */
	@Override
	public Type getType() {
		return Type.INVALID;
	}

	/**
	 * @see Tracker#getId()
	 */
	@Override
	public long getId() {
		return 0;
	}

	/**
	 * @see Tracker#setId(long)
	 */
	@Override
	public void setId(final long id) {}

	/**
	 * @see Tracker#announce(AnnounceParameters, TrackerSession)
	 */
	@Override
	protected void announce(final AnnounceParameters announceParameters,
			final TrackerSession trackerSession) {}

	/**
	 * @see Tracker#scrape(TrackerSession...)
	 */
	@Override
	protected void scrape(final TrackerSession... trackerSessions) {}

	/**
	 * @see Tracker#connect(int)
	 */
	@Override
	protected int connect(final int transactionId) {
		return -1;
	}
}