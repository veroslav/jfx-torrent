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

import java.util.Set;
import java.util.TreeSet;

import org.matic.torrent.codec.InfoHash;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.tracker.TrackerManager;

public final class QueuedTorrentManager {

	private final UdpConnectionManager udpConnectionManager;
	private final Set<QueuedTorrent> queuedTorrents;
	private final TrackerManager trackerManager;
	
	public QueuedTorrentManager() {
		udpConnectionManager = new UdpConnectionManager();
		
		trackerManager = new TrackerManager(udpConnectionManager);
		udpConnectionManager.addListener(trackerManager);
		queuedTorrents = new TreeSet<>();
		
		udpConnectionManager.init();
	}
	
	/**
	 * Unmanage all resources and perform cleanup
	 */
	public void stop() {
		trackerManager.stop();
		udpConnectionManager.stop();
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
			torrent.getTrackers().forEach(t -> trackerManager.addTracker(t, torrent.getInfoHash()));
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
			trackerManager.removeTorrent(infoHash);
		}
		
		return removed;
	}
}
