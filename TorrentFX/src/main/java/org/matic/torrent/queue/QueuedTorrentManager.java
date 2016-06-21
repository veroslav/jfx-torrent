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

import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.StateKeeper;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.beans.TrackerSessionViewBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class QueuedTorrentManager implements QueuedTorrentStatusChangeListener {

	private final List<QueuedTorrent> queuedTorrents = new ArrayList<>();
	private final TrackerManager trackerManager;
	
	public QueuedTorrentManager(final TrackerManager trackerManager) {
		this.trackerManager = trackerManager;		
	}

    /**
     * @see QueuedTorrentStatusChangeListener#stateChanged(QueuedTorrent, QueuedTorrent.State, QueuedTorrent.State)
     */
    @Override
    public void stateChanged(final QueuedTorrent torrent,
                             final QueuedTorrent.State oldStatus, final QueuedTorrent.State newStatus) {
        if(oldStatus == newStatus || (newStatus != QueuedTorrent.State.ACTIVE &&
                newStatus != QueuedTorrent.State.STOPPED)) {
            return;
        }
        trackerManager.issueTorrentEvent(torrent,
                newStatus == QueuedTorrent.State.ACTIVE? Tracker.Event.STARTED : Tracker.Event.STOPPED);
    }

	/**
	 * Add a torrent to be managed
	 * 
	 * @param torrent Target torrent
	 * @return A set of view beans for the newly created tracker sessions 
	 */
	public Set<TrackerSessionViewBean> add(final QueuedTorrent torrent) {
		synchronized(queuedTorrents) {
            final int torrentIndex = queuedTorrents.indexOf(torrent);
            final Set<String> trackerUrls = torrent.getProgress().getTrackerUrls();
            if(torrentIndex == -1) {
                //New torrent, add it
                queuedTorrents.add(torrent);
                torrent.addStateChangeListener(this);
                return trackerUrls.stream().map(
                        t -> trackerManager.addTracker(t, torrent)).collect(Collectors.toSet());
            } else {
                //Merge trackers, the torrent already exists
                final QueuedTorrent existingTorrent = queuedTorrents.get(torrentIndex);
                final BinaryEncodedList announceList = existingTorrent.getMetaData().getAnnounceList();
                final Set<BinaryEncodedString> newUrls = trackerUrls.stream().map(BinaryEncodedString::new).filter(
                        url -> !announceList.contains(url)).collect(Collectors.toSet());
                return newUrls.stream().map(url -> {
                    announceList.add(url);
                    return trackerManager.addTracker(url.getValue(), torrent);
                }).collect(Collectors.toSet());
            }
		}
	}
	
	/**
	 * Remove and stop managing a torrent 
	 * 
	 * @param torrent Queued torrent to remove
	 * @return Whether the target torrent was successfully removed
	 */
	public boolean remove(final QueuedTorrent torrent) {
        final boolean removed;
        synchronized (queuedTorrents) {
            removed = queuedTorrents.remove(torrent);
        }

        if(removed) {
            torrent.removeStateChangeListener(this);
            trackerManager.removeTorrent(torrent);
        }

        return removed;
	}
	
	/**
	 * Find a queued torrent matching the target info hash (if any)
	 * 
	 * @param infoHash Info hash of the torrent to find
	 * @return Optionally found torrent
	 */
	public Optional<QueuedTorrent> find(final InfoHash infoHash) {
		return queuedTorrents.stream().filter(t -> t.getMetaData().getInfoHash().equals(infoHash)).findFirst();
	}
	
	/**
	 * Store the torrents' progress and properties when shutting down the client 
	 */
	public final void storeState() {
		synchronized(queuedTorrents) {
            queuedTorrents.forEach(StateKeeper::store);
		}
	}
	
	protected int getQueueSize() {
		synchronized (queuedTorrents) {
            return queuedTorrents.size();
        }
	}
}