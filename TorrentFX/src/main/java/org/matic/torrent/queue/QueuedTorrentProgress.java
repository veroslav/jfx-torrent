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
package org.matic.torrent.queue;

import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class QueuedTorrentProgress {
	
	private final BinaryEncodedDictionary torrentState;

	public QueuedTorrentProgress(final BinaryEncodedDictionary torrentState) {
		this.torrentState = torrentState;
	}
	
	public String getName() {
		return torrentState.get(BinaryEncodingKeys.KEY_NAME).toString();
	}
	
	public void setName(final String name) {
		torrentState.put(BinaryEncodingKeys.KEY_NAME, new BinaryEncodedString(name));
	}
	
	public void addTrackerUrls(final Set<String> trackerUrls) {
		final BinaryEncodedList trackerList = new BinaryEncodedList();		
		trackerUrls.forEach(t -> trackerList.add(new BinaryEncodedString(t)));		
		torrentState.put(BinaryEncodingKeys.KEY_ANNOUNCE_LIST, trackerList);
	}
	
	public Set<String> getTrackerUrls() {
		final BinaryEncodedList trackerList = (BinaryEncodedList)torrentState.get(
				BinaryEncodingKeys.KEY_ANNOUNCE_LIST);
		
		final Set<String> trackerUrls = new LinkedHashSet<>();
		
		if(trackerList != null && trackerList.size() > 0) {			
			trackerList.stream().forEach(t -> trackerUrls.add(t.toString()));
		}
		
		return trackerUrls;
	}

    protected void setAddedOn(final long addedOnMillis) {
        torrentState.put(BinaryEncodingKeys.STATE_KEY_ADDED_ON, new BinaryEncodedInteger(addedOnMillis));
    }

    public long getAddedOn() {
        return ((BinaryEncodedInteger)torrentState.get(BinaryEncodingKeys.STATE_KEY_ADDED_ON)).getValue();
    }

	public void setStatus(final TorrentStatus status) {
		torrentState.put(BinaryEncodingKeys.STATE_KEY_TORRENT_STATUS, new BinaryEncodedString(status.name()));
	}
	
	protected TorrentStatus getStatus() {
		return TorrentStatus.valueOf(torrentState.get(BinaryEncodingKeys.STATE_KEY_TORRENT_STATUS).toString());
	}

	public byte[] toExportableValue() throws IOException {
		return torrentState.toExportableValue();
	}
}