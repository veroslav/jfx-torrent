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

package org.matic.torrent.tracking;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.net.udp.UdpConnectionListener;
import org.matic.torrent.net.udp.UdpResponse;
import org.matic.torrent.tracking.Tracker.Event;
import org.matic.torrent.tracking.listeners.HttpTrackerResponseListener;
import org.matic.torrent.tracking.listeners.PeerFoundListener;
import org.matic.torrent.utils.ResourceManager;

public final class TrackerManager implements HttpTrackerResponseListener, UdpConnectionListener {
	
	private static final long ANNOUNCE_DELAY_ON_TRACKER_ERROR = 1800;  //Seconds to wait on tracker error
	
	private static final int SCHEDULER_POOL_COUNT = 1;
	private final ScheduledExecutorService announceScheduler = 
			Executors.newScheduledThreadPool(SCHEDULER_POOL_COUNT);
	private final ExecutorService tcpRequestExecutor = Executors.newCachedThreadPool();
	
	private final Set<PeerFoundListener> peerListeners;
	private final Set<TorrentTracker> trackedTorrents;	
	
	private final Map<TorrentTracker, Future<?>> scheduledRequests;
	
	public TrackerManager() {
		peerListeners = new CopyOnWriteArraySet<>();
		trackedTorrents = new HashSet<>();
		
		scheduledRequests = new ConcurrentHashMap<>();
	}
	
	@Override
	public final void onResponseReceived(final TrackerResponse response, final TorrentTracker trackedTorrent) {
		trackedTorrent.setLastTrackerResponse(System.nanoTime());
		final Set<PwpPeer> peers = response.getPeers();
		if(!peers.isEmpty()) {
			//peerListeners.stream().forEach(l -> l.onPeersFound(peers));
		}
		synchronized(trackedTorrents) {
			final Tracker tracker = trackedTorrent.getTracker();
			tracker.setLastResponse(System.nanoTime());					
			if(trackedTorrents.contains(trackedTorrent) && 
					trackedTorrent.getLastTrackerEvent() != Tracker.Event.STOPPED) {
				
				//Check whether it was an error response before scheduling
				if(response.getType() != TrackerResponse.Type.OK) {
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							trackedTorrent.getLastTrackerEvent(), 0, 0, 0);
					scheduleAnnouncement(trackedTorrent, new AnnounceRequest(
							trackedTorrent, announceParameters), ANNOUNCE_DELAY_ON_TRACKER_ERROR);
				}
				else {
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							Tracker.Event.UPDATE, 0, 0, 0);
					scheduleAnnouncement(trackedTorrent, new AnnounceRequest(
							trackedTorrent, announceParameters), response.getInterval());
				}
			}
		}			
	}
	
	@Override
	public void onUdpResponseReceived(final UdpResponse response) {
		//TODO: Extract Tracker and InfoHash from UdpResponse and pass them instead of null
		onResponseReceived(parseUdpTrackerResponse(response), null);
	}
	
	@Override
	public Set<UdpResponse.Type> messageNotificationMask() {
		return new HashSet<UdpResponse.Type>(Arrays.asList(
				UdpResponse.Type.CONNECTION_ACCEPTED, UdpResponse.Type.ANNOUNCE));
	}
	
	/**
	 * Issue a tracker announce manually (explicitly by the user or when torrent state changes)
	 * 
	 * @param tracker Target announce tracker
	 * @param infoHash Info hash of the torrent to be announced
	 * @param announceParameters Request parameters
	 * @return Whether request was successful (false if currently not allowed)
	 */
	public boolean issueAnnounce(final Tracker tracker, final InfoHash infoHash, 
			final AnnounceParameters announceParameters) {
		synchronized(trackedTorrents) {
			final Optional<TorrentTracker> match = trackedTorrents.stream().filter(
					t -> t.getInfoHash().equals(infoHash) && t.getTracker().equals(tracker)).findFirst();
			if(!match.isPresent()) {
				return false;
			}
			synchronized(scheduledRequests) {
				final TorrentTracker trackedTorrent = match.get();
				final boolean announceAllowed = announceAllowed(trackedTorrent, announceParameters.getTrackerEvent());
				if(announceAllowed) {
					scheduleAnnouncement(trackedTorrent, new AnnounceRequest(trackedTorrent, announceParameters), 0);
				}
				return announceAllowed;
			}			
		}		
	}
	
	/**
	 * Get a list of all trackers that are tracking a torrent
	 * 
	 * @param infoHash Info hash of the tracked torrent
	 * @return Torrent trackers
	 */
	public List<TorrentTracker> getTrackers(final InfoHash infoHash) {
		return trackedTorrents.stream().filter(t -> t.getInfoHash().equals(infoHash)).collect(Collectors.toList());
	}
	
	/**
	 * Add a new tracker for a torrent
	 * 
	 * @param trackerUrl URL of the tracker being added
	 * @param infoHash Info hash of the torrent being tracked
	 * @return Whether either the tracker or a new torrent was added 
	 */
	public boolean addForTracking(final String trackerUrl, final InfoHash infoHash) {
		final Tracker tracker = initTracker(trackerUrl);
		if(tracker == null) {
			return false;
		}
		
		final TorrentTracker trackedTorrent = new TorrentTracker(infoHash, tracker); 
		
		synchronized(trackedTorrents) {			
			if(!trackedTorrents.add(trackedTorrent)) {
				return false;
			}			
		}
		
		final AnnounceParameters announceParameters = new AnnounceParameters( 
				Tracker.Event.STARTED, 0, 0, 0);
		scheduleAnnouncement(trackedTorrent, new AnnounceRequest(trackedTorrent, announceParameters), 0);
		
		return true;
	}
	
	/**
	 * Stop tracking a torrent for all trackers that are serving it.
	 * Also remove trackers that no longer serve any torrents.
	 * 
	 * @param torrent Info hash of the torrent to remove
	 * @return Whether the torrent was successfully removed
	 */
	public final boolean removeTorrent(final InfoHash torrentInfoHash) {
		synchronized(trackedTorrents) {		
			//Check if any tracked torrents match supplied info hash
			final List<TorrentTracker> matchList = trackedTorrents.stream().filter(
					t -> t.getInfoHash().equals(torrentInfoHash))
					.collect(Collectors.toList());
			
			if(matchList.isEmpty()) {
				//No trackers are tracking this torrent
				return false;
			}
			
			//Issue STOPPED announce to affected trackers, if not already STOPPED
			for(final TorrentTracker trackedTorrent : matchList) {				
				synchronized(scheduledRequests) {
					final Future<?> trackerRequest = scheduledRequests.remove(trackedTorrent);
					if(trackerRequest != null) {
						trackerRequest.cancel(false);
					}
					trackedTorrents.remove(trackedTorrent);
					
					if(trackedTorrent.getLastTrackerEvent() != Tracker.Event.STOPPED) {
						final AnnounceParameters announceParameters = new AnnounceParameters( 
								Tracker.Event.STOPPED, 0, 0, 0);
						scheduleAnnouncement(trackedTorrent, 
								new AnnounceRequest(trackedTorrent, announceParameters), 0);
					}
				}
			}
			
			return true;
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
	
	private boolean announceAllowed(final TorrentTracker trackedTorrent, final Tracker.Event targetEvent) {
		final Long minInterval = trackedTorrent.getMinInterval();
		final long minAnnounceWaitInterval = minInterval != null? 
				minInterval : trackedTorrent.getInterval();
		final boolean announceAllowed = (System.nanoTime() - 
				trackedTorrent.getLastTrackerResponse()) > minAnnounceWaitInterval;
				
		if(announceAllowed) {
			final Tracker.Event lastTrackerEvent = trackedTorrent.getLastTrackerEvent();
			switch(targetEvent) {
			case COMPLETED:
				if(lastTrackerEvent == Event.COMPLETED || lastTrackerEvent == Event.STOPPED) {
					return false;
				}
			case STARTED:
				if(lastTrackerEvent == Event.STARTED || lastTrackerEvent == Event.UPDATE) {
					return false;
				}
			case STOPPED:
				if(lastTrackerEvent == Event.STOPPED) {
					return false;
				}
			case UPDATE:
				if(lastTrackerEvent == Event.STOPPED) {
					return false;
				}	
			}
			return true;
		}
		return false;
	}
	
	private Tracker initTracker(final String trackerUrl) {
		if(trackerUrl.toLowerCase().startsWith(NetworkUtilities.HTTP_PROTOCOL) ||
				trackerUrl.toLowerCase().startsWith(NetworkUtilities.HTTPS_PROTOCOL)) {			
			return new HttpTracker(trackerUrl, this);
		}
		else if(trackerUrl.toLowerCase().startsWith(NetworkUtilities.UDP_PROTOCOL)) {			
			try {
				return new UdpTracker(trackerUrl, ResourceManager.INSTANCE.getUdpTrackerConnectionManager());
			} catch (final URISyntaxException use) {
				//Invalid tracker url, simply ignore the tracker
				System.err.println("Invalid UDP Tracker URL format (" + trackerUrl + "): " + use);
				return null;
			} 
		}
		return null;
	}
	
	private void scheduleAnnouncement(final TorrentTracker trackedTorrent, final AnnounceRequest trackerRequest,
			final long delay) {		
		synchronized(scheduledRequests) {
			final Future<?> existingTrackerRequest = scheduledRequests.remove(trackedTorrent);
			if(existingTrackerRequest != null) {
				existingTrackerRequest.cancel(false);
			}
			
			final Future<?> scheduledRequest = delay == 0? announceScheduler.submit(() ->
					announce(trackedTorrent, trackerRequest.getAnnounceParameters())) :
				announceScheduler.schedule(() -> announce(trackedTorrent, 
						trackerRequest.getAnnounceParameters()), delay, TimeUnit.SECONDS);		
					
			scheduledRequests.put(trackedTorrent, scheduledRequest);
		}
	}
	
	private void announce(final TorrentTracker trackedTorrent, final AnnounceParameters announceParameters) {	
		/* Send TCP requests in their own thread, because sending a TCP request
		   blocks until a response is received */
		final Tracker tracker = trackedTorrent.getTracker();
		if(tracker.getType() == Tracker.Type.TCP) {
			tcpRequestExecutor.execute(() -> tracker.announce(announceParameters, trackedTorrent));
		}
		else {
			tracker.announce(announceParameters, trackedTorrent);
		}
	}
	
	private TrackerResponse parseUdpTrackerResponse(final UdpResponse response) {
		//TODO: Implement method
		return null;
	}
}