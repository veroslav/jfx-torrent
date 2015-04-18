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

package org.matic.torrent.peer.tracking.tracker;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.matic.torrent.net.pwp.PwpPeer;

public final class TrackerResponse {
	
	public enum Type {
		INVALID_URL, READ_WRITE_ERROR, NORMAL, TRACKER_ERROR, INVALID_RESPONSE, WARNING
	}

	private final Optional<String> trackerId;
	private final Optional<Long> minInterval;
	private final Optional<String> message;
	
	private final Set<PwpPeer> peers;
	
	private final long incomplete;
	private final long interval;
	private final long complete;
	private final Type type;
	
	/**
	 * Constructor for building an error response
	 * 
	 * @param type Type of tracker error
	 * @param errorMessage Error message detailing the error
	 */
	public TrackerResponse(final Type type, final String errorMessage) {
		this(type, Optional.of(errorMessage), 0, Optional.of(0L), 
				Optional.ofNullable(null), 0, 0, Collections.emptySet());
	}
	
	/**
	 * Constructor for building a normal response
	 * 
	 * @param type Type of response (either NORMAL or WARNING)
	 * @param warningMessage null if NORMAL, warning message otherwise
	 */
	public TrackerResponse(final Type type, final Optional<String> warningMessage, final long interval,
			final Optional<Long> minInterval, final Optional<String> trackerId, final long complete,
			final long incomplete, final Set<PwpPeer> peers) {
		this.type = type;
		this.message = warningMessage;
		this.interval = interval;
		this.minInterval = minInterval; 
		this.trackerId = trackerId;
		this.incomplete = incomplete;
		this.complete = complete;
		this.peers = peers;
	}
	
	public final Set<PwpPeer> getPeers() {
		return peers;
	}
	
	public final long getIncomplete() {
		return incomplete;
	}
	
	public final long getComplete() {
		return complete;
	}
	
	public final Optional<String> getTrackerId() {
		return trackerId;
	}
	
	public final Optional<Long> getMinInterval() {
		return minInterval;
	}
	
	public final long getInterval() {
		return interval;
	}
	
	public final Type getType() {
		return type;
	}
	
	public final Optional<String> getMessage() {
		return message;
	}
}