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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.xml.bind.DatatypeConverter;

import org.matic.torrent.hash.InfoHash;

public final class QueuedTorrent implements Comparable<QueuedTorrent> {

	public enum State {
		ACTIVE, STOPPED, ERROR
	}
	
	private final InfoHash infoHash;
	
	private final ObjectProperty<State> state;	
	
	//TODO: Move priority handling to QueuedTorrentManager
	private int priority;

	public QueuedTorrent(final InfoHash infoHash, final int priority, final State state) {
		this.state = new SimpleObjectProperty<>(state);
		this.infoHash = infoHash;
		this.priority = priority;
	}
	
	public void setState(final State state) {
		this.state.set(state);
	}
	
	public State getState() {
		return state.get();
	}
	
	public ObjectProperty<State> stateProperty() {
		return state;
	}
	
	public InfoHash getInfoHash() {
		return infoHash;
	}
	
	public final int getPriority() {
		return priority;
	}
	
	@Override
	public int compareTo(final QueuedTorrent other) {
		if(priority < other.priority) {
			return -1;
		}
		if(priority > other.priority) {
			return 1;
		}
		return DatatypeConverter.printHexBinary(infoHash.getBytes())
				.compareTo(DatatypeConverter.printHexBinary(other.infoHash.getBytes()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((infoHash == null) ? 0 : infoHash.hashCode());
		result = prime * result + priority;		
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
		QueuedTorrent other = (QueuedTorrent) obj;
		if (infoHash == null) {
			if (other.infoHash != null)
				return false;
		} else if (!infoHash.equals(other.infoHash))
			return false;		
		return true;
	}
}
