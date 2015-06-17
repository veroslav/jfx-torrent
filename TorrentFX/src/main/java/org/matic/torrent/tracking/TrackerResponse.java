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

import java.util.Collections;
import java.util.Set;

import org.matic.torrent.net.pwp.PwpPeer;

public final class TrackerResponse {
	
	public enum Type {
		INVALID_URL, READ_WRITE_ERROR, NORMAL, TRACKER_ERROR, INVALID_RESPONSE, WARNING
	}

	private final String trackerId;
	private final Long minInterval;
	private final String message;
	
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
		this(type, errorMessage, 0, null, null, 0, 0, Collections.emptySet());
	}
	
	/**
	 * Constructor for building a normal response
	 * 
	 * @param type Type of response (either NORMAL or WARNING)
	 * @param warningMessage null if NORMAL, warning message otherwise
	 */
	public TrackerResponse(final Type type, final String warningMessage, final long interval,
			final Long minInterval, final String trackerId, final long complete,
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
	
	public final String getTrackerId() {
		return trackerId;
	}
	
	public final Long getMinInterval() {
		return minInterval;
	}
	
	public final long getInterval() {
		return interval;
	}
	
	public final Type getType() {
		return type;
	}
	
	public final String getMessage() {
		return message;
	}	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (complete ^ (complete >>> 32));
		result = prime * result + (int) (incomplete ^ (incomplete >>> 32));
		result = prime * result + (int) (interval ^ (interval >>> 32));
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result
				+ ((minInterval == null) ? 0 : minInterval.hashCode());
		result = prime * result + ((peers == null) ? 0 : peers.hashCode());
		result = prime * result
				+ ((trackerId == null) ? 0 : trackerId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackerResponse other = (TrackerResponse) obj;
		if (complete != other.complete)
			return false;
		if (incomplete != other.incomplete)
			return false;
		if (interval != other.interval)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (minInterval == null) {
			if (other.minInterval != null)
				return false;
		} else if (!minInterval.equals(other.minInterval))
			return false;
		if (peers == null) {
			if (other.peers != null)
				return false;
		} else if (!peers.equals(other.peers))
			return false;
		if (trackerId == null) {
			if (other.trackerId != null)
				return false;
		} else if (!trackerId.equals(other.trackerId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackerResponse [trackerId=" + trackerId + ", minInterval="
				+ minInterval + ", message=" + message + ", peers=" + peers
				+ ", incomplete=" + incomplete + ", interval=" + interval
				+ ", complete=" + complete + ", type=" + type + "]";
	}	
}