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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.StateKeeper;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.beans.TrackerSessionViewBean;

import javafx.beans.value.ChangeListener;

public final class QueuedTorrentManager {

	private final Map<QueuedTorrent, ChangeListener<QueuedTorrent.State>> stateChangeListeners = new ConcurrentHashMap<>();
	private final Map<QueuedTorrent, Set<String>> queuedTorrents = new HashMap<>();
	
	private final TrackerManager trackerManager;
	
	public QueuedTorrentManager(final TrackerManager trackerManager) {
		this.trackerManager = trackerManager;		
	}

	/**
	 * Add a torrent to be managed
	 * 
	 * @param torrent Target torrent
	 * @return A set of view beans for the newly created tracker sessions 
	 */
	public Set<TrackerSessionViewBean> add(final QueuedTorrent torrent) {		
		final Set<String> trackerUrls = torrent.getProgress().getTrackerUrls();
		
		synchronized(queuedTorrents) {
			if(queuedTorrents.putIfAbsent(torrent, trackerUrls) != null) {
				return Collections.emptySet();
			}			
		}
		
		addTorrentStateChangeListener(torrent);	
		
		final Set<TrackerSessionViewBean> sessionViewBeans = new HashSet<>();
		trackerUrls.forEach(t -> sessionViewBeans.add(trackerManager.addTracker(t, torrent)));
		
		return sessionViewBeans;
	}
	
	/**
	 * Remove and stop managing a torrent 
	 * 
	 * @param torrent Queued torrent to remove
	 * @return Whether the target torrent was successfully removed
	 */
	public boolean remove(final QueuedTorrent torrent) {
		final boolean removed = queuedTorrents.remove(torrent) != null;
		
		if(removed) {
			torrent.getProgress().stateProperty().removeListener(stateChangeListeners.remove(torrent));
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
		return queuedTorrents.keySet().stream().filter(t -> t.getMetaData().getInfoHash().equals(infoHash)).findFirst();
	}
	
	/**
	 * Store the torrents' progress and properties when shutting down the client 
	 */
	public final void storeState() {
		synchronized(queuedTorrents) {
			final Set<QueuedTorrent> torrents = queuedTorrents.keySet();
		
			if(!torrents.isEmpty()) {
				torrents.forEach(StateKeeper::store);
			}
		}
	}
	
	protected int getQueueSize() {
		return queuedTorrents.size();
	}
	
	private void onTorrentStateChanged(final QueuedTorrent torrent, QueuedTorrent.State oldValue,
			QueuedTorrent.State newValue) {
		if(oldValue == newValue || (newValue != QueuedTorrent.State.ACTIVE &&
				newValue != QueuedTorrent.State.STOPPED)) {
			return;
		}
		
		trackerManager.issueTorrentEvent(torrent, 
				newValue == QueuedTorrent.State.ACTIVE? Tracker.Event.STARTED : Tracker.Event.STOPPED);		
	}
	
	private void addTorrentStateChangeListener(final QueuedTorrent torrent) {
		final ChangeListener<QueuedTorrent.State> stateChangeListener = 
				(obs, oldV, newV) -> onTorrentStateChanged(torrent, oldV, newV);
		stateChangeListeners.put(torrent, stateChangeListener);
		torrent.getProgress().stateProperty().addListener(stateChangeListener);
	}
}