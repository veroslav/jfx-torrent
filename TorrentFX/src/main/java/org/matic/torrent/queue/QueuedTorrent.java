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

public final class QueuedTorrent {
	
	public enum State {
		ACTIVE, STOPPED, ERROR
	}

	private final QueuedTorrentMetaData metaData;
	private final QueuedTorrentProgress progress;
	
	public QueuedTorrent(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {		
		this.metaData = metaData;
		this.progress = progress;		
	}
	
	public final QueuedTorrentMetaData getMetaData() {
		return metaData;
	}
	
	public final QueuedTorrentProgress getProgress() {
		return progress;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((metaData == null) ? 0 : metaData.hashCode());
		result = prime * result + ((progress == null) ? 0 : progress.hashCode());
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
		if (metaData == null) {
			if (other.metaData != null)
				return false;
		} else if (!metaData.equals(other.metaData))
			return false;		
		if (progress == null) {
			if (other.progress != null)
				return false;
		} else if (!progress.equals(other.progress))
			return false;
		return true;
	}	
}