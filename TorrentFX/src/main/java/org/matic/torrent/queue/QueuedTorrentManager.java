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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.value.ChangeListener;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.utils.ResourceManager;

public final class QueuedTorrentManager {

	private final Map<QueuedTorrent, ChangeListener<QueuedTorrent.Status>> statusChangeListeners = new ConcurrentHashMap<>();
	private final Map<QueuedTorrent, Set<String>> queuedTorrents = new HashMap<>();

	/**
	 * Add a torrent to be managed
	 * 
	 * @param torrent Target torrent
	 * @param trackerUrls Torrent's trackers
	 * @return Whether this torrent was successfully added
	 */
	public boolean add(final QueuedTorrent torrent, final Set<String> trackerUrls) {
		if(queuedTorrents.putIfAbsent(torrent, trackerUrls) != null) {
			return false;
		}
		
		addTorrentStatusChangeListener(torrent);
		
		final TrackerManager trackerManager = ResourceManager.INSTANCE.getTrackerManager();
		trackerUrls.forEach(t ->
			trackerManager.addTracker(t, torrent, torrent.getStatus() != QueuedTorrent.Status.STOPPED));
		
		return true;
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
			torrent.statusProperty().removeListener(statusChangeListeners.remove(torrent));
			ResourceManager.INSTANCE.getTrackerManager().removeTorrent(torrent);
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
		return queuedTorrents.keySet().stream().filter(t -> t.getInfoHash().equals(infoHash)).findFirst();
	}
	
	protected int getQueueSize() {
		return queuedTorrents.size();
	}
	
	private void onTorrentStatusChanged(final QueuedTorrent torrent, QueuedTorrent.Status oldValue,
			QueuedTorrent.Status newValue) {
		if(oldValue == newValue || (newValue != QueuedTorrent.Status.ACTIVE &&
				newValue != QueuedTorrent.Status.STOPPED)) {
			return;
		}
		
		ResourceManager.INSTANCE.getTrackerManager().issueTorrentEvent(torrent, 
				newValue == QueuedTorrent.Status.ACTIVE? Tracker.Event.STARTED : Tracker.Event.STOPPED);		
	}
	
	private void addTorrentStatusChangeListener(final QueuedTorrent torrent) {
		final ChangeListener<QueuedTorrent.Status> statusChangeListener = 
				(obs, oldV, newV) -> onTorrentStatusChanged(torrent, oldV, newV);
		statusChangeListeners.put(torrent, statusChangeListener);
		torrent.statusProperty().addListener(statusChangeListener);
	}
}