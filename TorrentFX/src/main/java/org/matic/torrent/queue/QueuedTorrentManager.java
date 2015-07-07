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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.tracking.TrackedTorrent;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.utils.ResourceManager;

public final class QueuedTorrentManager {

	private final Set<QueuedTorrent> queuedTorrents;
	
	private List<TrackedTorrent> activeTorrentTrackers = null;
	private InfoHash activeInfoHash = null; 
	
	public QueuedTorrentManager() {		 
		queuedTorrents = new TreeSet<>();		
	}
	
	/**
	 * Add a torrent to be managed
	 * 
	 * @param torrent Target torrent
	 * @return Whether this torrent was successfully added
	 */
	public boolean add(final QueuedTorrent torrent) {
		final boolean added = queuedTorrents.add(torrent);
		
		if(added) {
			final InfoHash infoHash = torrent.getInfoHash();
			final TrackerManager trackerManager = ResourceManager.INSTANCE.getTrackerManager();
			torrent.getTrackers().forEach(t -> trackerManager.addForTracking(t, infoHash));
			activeTorrentTrackers = trackerManager.getTrackers(infoHash);
			activeInfoHash = infoHash;
		}
		
		return added;
	}
	
	/**
	 * Remove and stop managing a torrent 
	 * 
	 * @param infoHash Info hash of the torrent to remove
	 * @return Whether the target torrent was successfully removed
	 */
	public boolean remove(final InfoHash infoHash) {
		final boolean removed = queuedTorrents.removeIf(qt -> qt.getInfoHash().equals(infoHash));
		
		if(removed) {
			ResourceManager.INSTANCE.getTrackerManager().removeTorrent(infoHash);
		}
		
		return removed;
	}
	
	/**
	 * Update tracker view beans with the latest tracker statistics for a torrent
	 * 
	 * @param infoHash Target torrent's info hash
	 * @param trackerViews List of tracker views to update
	 */
	public void updateTrackerStatistics(final InfoHash infoHash, final List<TrackerView> trackerViews) {
		if(activeInfoHash != infoHash) {
			activeInfoHash = infoHash;
			activeTorrentTrackers = ResourceManager.INSTANCE.getTrackerManager().getTrackers(infoHash);
		}
		trackerViews.forEach(tv -> activeTorrentTrackers.stream().filter(
			tt -> tt.getTracker().getUrl().equalsIgnoreCase(tv.getTrackerName())).forEach(m -> {
				//Update view with values from matching torrent tracker
				tv.leechersProperty().set(m.getLeechers());
				tv.seedsProperty().set(m.getSeeders());
				tv.nextUpdateProperty().set((System.nanoTime() - m.getLastTrackerResponse()) - m.getInterval()); 
			}
		));
	}
	
	protected boolean contains(final QueuedTorrent torrent) {
		return queuedTorrents.contains(torrent);
	}
	
	protected int getQueueSize() {
		return queuedTorrents.size();
	}
}
