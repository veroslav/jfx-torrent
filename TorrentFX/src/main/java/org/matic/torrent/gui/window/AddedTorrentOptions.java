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

package org.matic.torrent.gui.window;

import javafx.scene.control.TreeItem;

import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrent.State;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;

/**
 * A bean containing all of the options selected by the user, during the addition of
 * a new torrent.
 * 
 * @author vedran
 *
 */
public final class AddedTorrentOptions {
	
	private final TreeItem<TorrentFileEntry> torrentContents;
	private final QueuedTorrentProgress progress;
	private final QueuedTorrentMetaData metaData;

	private final boolean createSubfolder;
	private final boolean skipHashCheck;
	private final boolean addToTopQueue;
	private final boolean startTorrent;
	
	private final String label;
	private final String name;
	private final String path;

	public AddedTorrentOptions(final QueuedTorrentMetaData metaData, 
			final TreeItem<TorrentFileEntry> torrentContents, final String name, final String path,
			final String label, final boolean startTorrent, final boolean createSubfolder, 
			final boolean addToTopQueue, final boolean skipHashCheck) {
		this.metaData = metaData;
		this.torrentContents = torrentContents;
		this.name = name;
		this.path = path;
		this.label = label;
		
		this.startTorrent = startTorrent;
		this.createSubfolder = createSubfolder;
		this.addToTopQueue = addToTopQueue;
		this.skipHashCheck = skipHashCheck;
		
		final BinaryEncodedDictionary state = new BinaryEncodedDictionary();
		populateState(state);
		
		progress = new QueuedTorrentProgress(state);
	}
	
	public final QueuedTorrentMetaData getMetaData() {
		return metaData;
	}
	
	public final QueuedTorrentProgress getProgress() {
		return progress;
	}
	
	public final TreeItem<TorrentFileEntry> getTorrentContents() {
		return torrentContents;
	}
	
	public final boolean shouldCreateSubfolder() {
		return createSubfolder;
	}
	
	public final boolean shouldAddToTopQueue() {
		return addToTopQueue;
	}
	
	public final boolean shouldSkipHashCheck() {
		return skipHashCheck;
	}
	
	private void populateState(final BinaryEncodedDictionary state) {
		if(name != null) {
			state.put(BinaryEncodingKeys.KEY_NAME, new BinaryEncodedString(name));
		}
		if(path != null) {
			state.put(BinaryEncodingKeys.KEY_PATH, new BinaryEncodedString(path));
		}
		if(label != null) {
			state.put(BinaryEncodingKeys.STATE_KEY_LABEL, new BinaryEncodedString(label));
		}
		
		final QueuedTorrent.State targetState = startTorrent? State.ACTIVE : State.STOPPED;
		state.put(BinaryEncodingKeys.STATE_KEY_TORRENT_STATE, new BinaryEncodedString(targetState.name()));
		
		final BinaryEncodedList trackerList = new BinaryEncodedList();
		metaData.getAnnounceList().stream().flatMap(l -> ((BinaryEncodedList)l).stream()).forEach(
				u -> trackerList.add(new BinaryEncodedString(u.toString())));
		
		final String announceUrl = metaData.getAnnounceUrl();
		
		if(announceUrl != null) {		
			trackerList.add(new BinaryEncodedString(announceUrl));
		}
		
		state.put(BinaryEncodingKeys.KEY_ANNOUNCE_LIST, trackerList);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (addToTopQueue ? 1231 : 1237);
		result = prime * result + (createSubfolder ? 1231 : 1237);		
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((metaData == null) ? 0 : metaData.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + (skipHashCheck ? 1231 : 1237);
		result = prime * result + (startTorrent ? 1231 : 1237);
		result = prime * result
				+ ((torrentContents == null) ? 0 : torrentContents.hashCode());
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
		AddedTorrentOptions other = (AddedTorrentOptions) obj;
		if (addToTopQueue != other.addToTopQueue)
			return false;
		if (createSubfolder != other.createSubfolder)
			return false;		
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (metaData == null) {
			if (other.metaData != null)
				return false;
		} else if (!metaData.equals(other.metaData))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (skipHashCheck != other.skipHashCheck)
			return false;
		if (startTorrent != other.startTorrent)
			return false;
		if (torrentContents == null) {
			if (other.torrentContents != null)
				return false;
		} else if (!torrentContents.equals(other.torrentContents))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AddNewTorrentOptions [torrentContents=" + torrentContents
				+ ", metaData=" + metaData + ", createSubfolder=" + createSubfolder
				+ ", skipHashCheck=" + skipHashCheck + ", addToTopQueue=" + addToTopQueue
				+ ", startTorrent=" + startTorrent + ", label=" + label
				+ ", name=" + name + ", path=" + path + "]";
	}
}