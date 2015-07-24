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

import javafx.collections.ObservableList;

import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.TrackerSession;
import org.matic.torrent.utils.ResourceManager;

public final class QueuedTorrentManager {

	private final Map<QueuedTorrent, Set<TrackerSession>> queuedTrackerSessions = new HashMap<>();
	//private List<TrackerSession> activeTorrentTrackerSessions = null;
	
	public QueuedTorrentManager() {	}
	
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
		
		final InfoHash infoHash = torrent.getInfoHash();
		final TrackerManager trackerManager = ResourceManager.INSTANCE.getTrackerManager();
		torrent.getTrackers().forEach(t -> {
			final TrackerSession trackerSession = trackerManager.addTracker(
				t, infoHash, torrent.getStatus() != QueuedTorrent.Status.STOPPED);
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
	 * Update tracker view beans with the latest tracker statistics for a torrent
	 * 
	 * @param queuedTorrent Target torrent
	 * @param trackerViews List of tracker views to update
	 */
	public final void trackerSnapshot(final QueuedTorrent queuedTorrent, 
			final ObservableList<TrackerView> trackerViews) {		
		queuedTrackerSessions.get(queuedTorrent).stream().forEach(ts -> {
			final Optional<TrackerView> match = trackerViews.stream().filter(tv ->
				tv.getTrackerName().equals(ts.getTracker().getUrl())
			).findFirst();
			if(match.isPresent()) {
				final TrackerView trackerView = match.get();
				
				//System.out.println("trackerSnapshot(): leechers = " + ts.getLeechers());
				
				/*
				 	Instant start = Instant.now();
					Thread.sleep(63553);
					Instant end = Instant.now();
					System.out.println(Duration.between(start, end));
				 */
				
				trackerView.statusProperty().set(ts.getTrackerMessage());
				trackerView.leechersProperty().set(ts.getLeechers());
				trackerView.seedsProperty().set(ts.getSeeders());
				trackerView.downloadedProperty().set(ts.getDownloaded());
				trackerView.intervalProperty().set(ts.getInterval());
				
				trackerView.nextUpdateProperty().set(ts.getInterval() - 
						(System.currentTimeMillis() - ts.getLastTrackerResponse()) / 1000);
				
				final Long minInterval = ts.getMinInterval();
				trackerView.minIntervalProperty().set(minInterval != null? minInterval : 0);
			}
		});
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