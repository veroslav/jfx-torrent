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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class QueuedTorrent {
	
	public enum State {
		ACTIVE, STOPPED, ERROR
	}

    private final List<QueuedTorrentStatusChangeListener> stateChangeListeners = new CopyOnWriteArrayList<>();
	private final QueuedTorrentMetaData metaData;
	private final QueuedTorrentProgress progress;
	
	public QueuedTorrent(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
		this.metaData = metaData;
		this.progress = progress;
        progress.stateProperty().addListener((obs, oldV, newV) -> notifyListenersOnStateChange(oldV, newV));
	}
	
	public final QueuedTorrentMetaData getMetaData() {
		return metaData;
	}
	
	public final QueuedTorrentProgress getProgress() {
		return progress;
	}

    public final void addStateChangeListener(final QueuedTorrentStatusChangeListener listener) {
        stateChangeListeners.add(listener);
    }

    public final void removeStateChangeListener(final QueuedTorrentStatusChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    private void notifyListenersOnStateChange(final State oldState, final State newState) {
        stateChangeListeners.forEach(l -> l.stateChanged(this, oldState, newState));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueuedTorrent that = (QueuedTorrent) o;

        return metaData != null ? metaData.equals(that.metaData) : that.metaData == null;

    }

    @Override
    public int hashCode() {
        return metaData != null ? metaData.hashCode() : 0;
    }
}