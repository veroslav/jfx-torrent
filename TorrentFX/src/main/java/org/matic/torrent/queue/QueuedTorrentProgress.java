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

package org.matic.torrent.queue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;

public class QueuedTorrentProgress {
	
	private final BinaryEncodedDictionary torrentState;
	
	private final IntegerProperty priority = new SimpleIntegerProperty(0);
	private final ObjectProperty<QueuedTorrent.State> state;

	public QueuedTorrentProgress(final BinaryEncodedDictionary torrentState) {
		this.torrentState = torrentState;
		
		final String stateValue = ((BinaryEncodedString)torrentState.get(
				BinaryEncodingKeys.STATE_KEY_TORRENT_STATE)).toString();
		this.state = new SimpleObjectProperty<>(QueuedTorrent.State.valueOf(stateValue));
	}
	
	public final String getName() {
		return torrentState.get(BinaryEncodingKeys.KEY_NAME).toString();
	}
	
	public final void setName(final String name) {
		torrentState.put(BinaryEncodingKeys.KEY_NAME, new BinaryEncodedString(name));
	}
	
	public final void addTrackerUrls(final Set<String> trackerUrls) {		
		final BinaryEncodedList trackerList = new BinaryEncodedList();		
		trackerUrls.forEach(t -> trackerList.add(new BinaryEncodedString(t)));		
		torrentState.put(BinaryEncodingKeys.KEY_ANNOUNCE_LIST, trackerList);
	}
	
	public final Set<String> getTrackerUrls() {
		final BinaryEncodedList trackerList = (BinaryEncodedList)torrentState.get(
				BinaryEncodingKeys.KEY_ANNOUNCE_LIST);
		
		final Set<String> trackerUrls = new HashSet<>();
		
		if(trackerList != null && trackerList.size() > 0) {			
			trackerList.stream().forEach(t -> trackerUrls.add(t.toString()));
		}
		
		return trackerUrls;
	}
	
	public final int getPriority() {
		return priority.get();
	}
	
	public final void setPriority(final int priority) {
		this.priority.set(priority);
	}
	
	public IntegerProperty priorityProperty() {
		return priority;
	}
	
	public void setState(final QueuedTorrent.State state) {
		this.state.set(state);
	}
	
	public QueuedTorrent.State getState() {
		return state.get();
	}
	
	public ObjectProperty<QueuedTorrent.State> stateProperty() {
		return state;
	}

	public final byte[] toExportableValue() throws IOException {		
		return torrentState.toExportableValue();
	}
}