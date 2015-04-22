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

package org.matic.torrent.peer.tracking;

import java.util.Arrays;

import org.matic.torrent.peer.tracking.tracker.Tracker;
import org.matic.torrent.utils.HashUtilities;

public final class TrackableTorrent {
	
	private final String hexInfoHash;
	private final byte[] infoHash;	
	
	private Tracker.Event lastTrackerEvent;
	private int transactionId = 0;	

	public TrackableTorrent(final byte[] infoHash) {
		hexInfoHash = HashUtilities.convertToHexValue(infoHash);
		this.infoHash = new byte[infoHash.length];
		System.arraycopy(infoHash, 0, this.infoHash, 0, infoHash.length);
		lastTrackerEvent = Tracker.Event.STOPPED;
	}
	
	public void setLastTrackerEvent(final Tracker.Event lastTrackerEvent) {
		this.lastTrackerEvent = lastTrackerEvent;
	}
	
	public Tracker.Event getLastTrackerEvent() {
		return lastTrackerEvent;
	}
	
	public final int getTransactionId() {
		return transactionId;
	}
	
	//transactionId = ClientProperties.generateUniqueId();
	public final void setTransactionId(final int transactionId) {
		this.transactionId = transactionId;		
	}	
	
	public final String getInfoHashHexValue() {
		return hexInfoHash;
	}
	
	public final byte[] getInfoHashBytes() {
		final byte[] copyOfInfoHash = new byte[infoHash.length];
		System.arraycopy(infoHash, 0, copyOfInfoHash, 0, copyOfInfoHash.length);
		return copyOfInfoHash;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hexInfoHash == null) ? 0 : hexInfoHash.hashCode());
		result = prime * result + Arrays.hashCode(infoHash);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackableTorrent other = (TrackableTorrent) obj;
		if (hexInfoHash == null) {
			if (other.hexInfoHash != null)
				return false;
		} else if (!hexInfoHash.equals(other.hexInfoHash))
			return false;
		if (!Arrays.equals(infoHash, other.infoHash))
			return false;
		return true;
	}
}