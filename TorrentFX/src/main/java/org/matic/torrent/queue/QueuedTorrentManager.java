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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.TrackerSession;
import org.matic.torrent.utils.ResourceManager;

public final class QueuedTorrentManager {

	//TODO: Simplify this to a Set<QueuedTorrent> (don't need TrackerSession anymore)
	//TODO: Move trackers from QueuedTorrent to Map<QueuedTorrent, Set<String> trackers>
	private final Map<QueuedTorrent, Set<TrackerSession>> queuedTrackerSessions = new HashMap<>();

	/**
	 * Add a torrent to be managed
	 * 
	 * @param torrent Target torrent
	 * @return Whether this torrent was successfully added
	 */
	public boolean add(final QueuedTorrent torrent) {
		if(queuedTrackerSessions.containsKey(torrent)) {
			return false;
		}
		
		final TrackerManager trackerManager = ResourceManager.INSTANCE.getTrackerManager();
		torrent.getTrackers().forEach(t -> {
			final TrackerSession trackerSession = trackerManager.addTracker(
				t, torrent, torrent.getStatus() != QueuedTorrent.Status.STOPPED);
			
			if(trackerSession != null) {
				queuedTrackerSessions.compute(torrent, (key, value) -> {
					final Set<TrackerSession> sessions = value == null? new HashSet<>() : value;
					sessions.add(trackerSession);
					return sessions;
				});
			}
		});
		
		return true;
	}
	
	/**
	 * Remove and stop managing a torrent 
	 * 
	 * @param queuedTorrent Queued torrent to remove
	 * @return Whether the target torrent was successfully removed
	 */
	public boolean remove(final QueuedTorrent queuedTorrent) {
		final boolean removed = !queuedTrackerSessions.remove(queuedTorrent).isEmpty();
		
		if(removed) {
			ResourceManager.INSTANCE.getTrackerManager().removeTorrent(queuedTorrent.getInfoHash());
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
		return queuedTrackerSessions.keySet().stream().filter(t -> t.getInfoHash().equals(infoHash)).findFirst();
	}
	
	protected int getQueueSize() {
		return queuedTrackerSessions.size();
	}
}