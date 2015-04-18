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

package org.matic.torrent.peer.tracking.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.net.udp.UdpConnectionListener;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpResponse;
import org.matic.torrent.peer.tracking.PeerFoundListener;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class TrackerManager implements TrackerResponseListener, UdpConnectionListener {
	
	private static final int SCHEDULER_POOL_COUNT = 1;
	private final ExecutorService announceScheduler = Executors.newScheduledThreadPool(SCHEDULER_POOL_COUNT);
	private final ExecutorService tcpRequestExecutor = Executors.newCachedThreadPool();
	
	private final UdpConnectionManager udpConnectionManager;
	private final Set<PeerFoundListener> peerListeners;
	private final List<Tracker> trackers;	
	
	public TrackerManager(final UdpConnectionManager udpConnectionManager) {
		this.udpConnectionManager = udpConnectionManager;
		this.udpConnectionManager.addListener(this);
		peerListeners = new CopyOnWriteArraySet<>();
		trackers = new ArrayList<>();
	}
	
	@Override
	public void onUdpResponseReceived(final UdpResponse response) {
		//TODO: Extract Tracker from UdpResponse and pass it instead of null
		onResponseReceived(parseUdpTrackerResponse(response), null);
	}
	
	/**
	 * Add a new tracker or update an existing with a new torrent
	 * 
	 * @param tracker Tracker to add
	 * @return Whether either the tracker or a new torrent was added 
	 */
	public final boolean addTracker(final Tracker tracker) {	
		boolean shouldAnnounce = false;
		synchronized(trackers) {
			final int trackerIndex = trackers.indexOf(tracker);
			if(trackerIndex != -1) {
				//Tracker exists, check for new info hashes
				final Tracker existingTracker = trackers.get(trackerIndex);
				final Predicate<TrackableTorrent> filterCriteria = tracker.getTorrents()::contains;				
				final Set<TrackableTorrent> newTorrents = existingTracker.getTorrents().stream().filter(
						filterCriteria.negate()).collect(Collectors.toSet()); 
				if(newTorrents.size() > 0) {
					newTorrents.stream().forEach(t -> existingTracker.addTorrent(t));
					existingTracker.setNextAnnounce(System.nanoTime());
					shouldAnnounce = true;
				}
				shouldAnnounce = false;
			}
			else {
				//New tracker, simply add it
				shouldAnnounce = trackers.add(tracker);
				tracker.setNextAnnounce(System.nanoTime());
			}			
		}
		if(shouldAnnounce) {			
			scheduleAnnouncement();
		}
		return shouldAnnounce;
	}
	
	/**
	 * Stop tracking a torrent for all trackers that are serving it.
	 * Also remove trackers that no longer serve any torrents.
	 * 
	 * @param torrent Torrent to stop tracking
	 * @return Whether the torrent was successfully removed
	 */
	public final boolean removeTorrent(final TrackableTorrent torrent) {
		synchronized(trackers) {			
			//Collect trackers serving the target torrent
			final Set<Tracker> affectedTrackers = trackers.stream().filter(
					t -> t.removeTorrent(torrent)).collect(Collectors.toSet());
			
			//Remove trackers that no longer serve any torrents
			affectedTrackers.stream().filter(t -> t.getTorrents().isEmpty())
				.forEach(t -> trackers.remove(t));
			
			//Issue STOPPED announce to affected trackers, if not already STOPPED
			if(torrent.getLastTrackerEvent() != Tracker.Event.STOPPED) {
				affectedTrackers.stream().forEach(t -> announce(
						t, buildAnnounceRequest(torrent, Tracker.Event.STOPPED)));
			}
			
			return !affectedTrackers.isEmpty(); 
		}
	}
	
	public final boolean addPeerListener(final PeerFoundListener peerListener) {
		return peerListeners.add(peerListener);
	}
	
	public final boolean removePeerListener(final PeerFoundListener peerListener) {
		return peerListeners.remove(peerListener);
	}
	
	/**
	 * Cleanup after closing down the tracker manager
	 */
	public final void stop() {
		announceScheduler.shutdownNow();
		tcpRequestExecutor.shutdownNow();
	}

	@Override
	public final void onResponseReceived(final TrackerResponse response, final Tracker tracker) {		
		//TODO: Perhaps handle the received response in a dedicated thread?
		final Set<PwpPeer> peers = response.getPeers();
		if(!peers.isEmpty()) {
			peerListeners.stream().forEach(l -> l.onPeersFound(response.getPeers()));
		}
		tracker.setLastResponse(System.nanoTime());
	}	
	
	private AnnounceRequest buildAnnounceRequest(final TrackableTorrent torrent, final Tracker.Event trackerEvent) {
		return new AnnounceRequest(torrent, trackerEvent, 0, 0, 0);
	}
	
	private void scheduleAnnouncement() {
		//TODO: Implement method
	}
	
	private void announce(final Tracker tracker, final AnnounceRequest announceRequest) {
		/* Send TCP requests in their own thread, because sending a TCP request
		   blocks until a response is received */
		if(tracker.getType() == Tracker.Type.TCP) {
			tcpRequestExecutor.execute(() -> tracker.announce(announceRequest));
		}
		else {
			tracker.announce(announceRequest);
		}
	}
	
	private TrackerResponse parseUdpTrackerResponse(final UdpResponse response) {
		//TODO: Implement method
		return null;
	}
}