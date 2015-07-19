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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
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
import org.matic.torrent.net.udp.UdpTrackerResponse;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.tracking.Tracker.Event;
import org.matic.torrent.tracking.listeners.HttpTrackerResponseListener;
import org.matic.torrent.tracking.listeners.PeerFoundListener;
import org.matic.torrent.tracking.listeners.UdpTrackerResponseListener;
import org.matic.torrent.utils.UnitConverter;

public final class TrackerManager implements HttpTrackerResponseListener, UdpTrackerResponseListener {
	
	private static final long UDP_TRACKER_CONNECTION_ID_TIMEOUT = 60000; //60 s		
	private static final int UDP_TRACKER_RESPONSE_PEER_LENGTH = 6; //6 bytes [IP (4) + PORT (2)]
	private static final long ANNOUNCE_DELAY_ON_TRACKER_ERROR = 1800;  //Seconds to wait on tracker error
	
	private static final int SCHEDULER_POOL_COUNT = 1;
	private final ScheduledExecutorService requestScheduler = 
			Executors.newScheduledThreadPool(SCHEDULER_POOL_COUNT);
	private final ExecutorService requestExecutor = Executors.newCachedThreadPool();
	
	private final Map<TrackerSession, ScheduledAnnouncement> scheduledRequests = new ConcurrentHashMap<>();
	private final Set<PeerFoundListener> peerListeners = new CopyOnWriteArraySet<>();
	private final Set<TrackerSession> trackerSessions = new HashSet<>();	
	
	/**
	 * @see HttpTrackerResponseListener#onAnnounceResponseReceived(AnnounceResponse, TrackedTorrent)
	 */
	@Override
	public final void onAnnounceResponseReceived(final AnnounceResponse announceResponse, 
			final TrackerSession trackerSession) {
		final Set<PwpPeer> peers = announceResponse.getPeers();
		if(!peers.isEmpty()) {
			//peerListeners.stream().forEach(l -> l.onPeersFound(peers));
		}
		
		synchronized(trackerSession) {
			final long responseTime = System.currentTimeMillis();
			trackerSession.getTracker().setLastResponse(responseTime);
			trackerSession.setLastTrackerResponse(responseTime);			
		}
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {				
			if(trackerSessions.contains(trackerSession) && 
					trackerSession.getLastTrackerEvent() != Tracker.Event.STOPPED) {
				//Check whether it was an error response before scheduling
				if(announceResponse.getType() != TrackerResponse.Type.OK) {
					
					System.err.println("Announce returned an error ( " + announceResponse.getType() + "): "
							+ trackerSession.getTracker() + ", msg = " + announceResponse.getMessage());
					
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							trackerSession.getLastTrackerEvent(), 0, 0, 0);
					scheduleAnnouncement(trackerSession, announceParameters, ANNOUNCE_DELAY_ON_TRACKER_ERROR);
				}
				else {
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							Tracker.Event.UPDATE, 0, 0, 0);
					scheduleAnnouncement(trackerSession, announceParameters, announceResponse.getInterval());
				}
			}
			else {
				//Cancelling any scheduled requests as we have STOPPED
				System.out.println("onAnnounceResponseReceived(" + trackerSession.getTracker().getUrl() + 
				": Cancelling due to STOPPED");
				scheduledRequests.compute(trackerSession, (session, announcement) -> {
					final Future<?> future = announcement.getFuture(); 
					future.cancel(false);
					return null;
				});
			}
		}
		
		System.out.println("SYNCHRONIZED.onAnnounceResponseReceived(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");
	}	
	
	/**
	 * @see HttpTrackerResponseListener#onScrapeResponseReceived(Tracker, ScrapeResponse)
	 */
	@Override
	public void onScrapeResponseReceived(final Tracker tracker, final ScrapeResponse scrapeResponse) {
		//TODO: Implement method
		
		System.out.println("onScrapeResponseReceived(): " + scrapeResponse.getScrapeStatistics());
	}

	/**
	 * @see UdpTrackerResponseListener#onUdpTrackerResponseReceived(UdpTrackerResponse)
	 */
	@Override
	public final void onUdpTrackerResponseReceived(final UdpTrackerResponse response) {
		switch(response.getAction()) {
		case UdpTracker.ACTION_CONNECT:
			onUdpTrackerConnect(response);
			break;
		case UdpTracker.ACTION_ANNOUNCE:
			onUdpTrackerAnnounce(response);
			break;
		case UdpTracker.ACTION_SCRAPE:
			onUdpTrackerScrape(response);
			break;
		case UdpTracker.ACTION_ERROR:
			onUdpTrackerError(response);
			break;
		default:
			return;
		}	
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
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {
			final Optional<TrackerSession> match = trackerSessions.stream().filter(
					t -> t.getInfoHash().equals(infoHash) && t.getTracker().equals(tracker)).findFirst();
			if(!match.isPresent()) {
				return false;
			}
			final TrackerSession trackerSession = match.get();
			final boolean announceAllowed = announceAllowed(trackerSession);
			if(announceAllowed) {
				scheduleAnnouncement(trackerSession, announceParameters, 0);
			}
			
			System.out.println("SYNCHRONIZED.issueAnnounce(): Released lock after " 
					+ (System.currentTimeMillis() - start) + " ms.");
			
			return announceAllowed;
		}
	}
	
	/**
	 * Get a list of all trackers that are tracking a torrent
	 * 
	 * @param infoHash Info hash of the tracked torrent
	 * @return Torrent tracker sessions
	 */
	public List<TrackerSession> getTrackers(final InfoHash infoHash) {
		return trackerSessions.stream().filter(t -> t.getInfoHash().equals(infoHash)).collect(Collectors.toList());
	}
	
	/**
	 * Add a new tracker for a torrent
	 * 
	 * @param trackerUrl URL of the tracker being added
	 * @param infoHash Info hash of the torrent being tracked
	 * @param shouldAnnounce Whether to announce to the tracker
	 * @return Whether either the tracker or a new torrent was added 
	 */
	public boolean addTorrentTracker(final String trackerUrl, final InfoHash infoHash,
			final boolean shouldAnnounce) {
		final Tracker tracker = initTracker(trackerUrl);
		if(tracker == null) {
			return false;
		}
		
		final TrackerSession trackerSession = new TrackerSession(infoHash, tracker); 
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {			
			if(!trackerSessions.add(trackerSession)) {
				return false;
			}
			if(shouldAnnounce) {
				final AnnounceParameters announceParameters = new AnnounceParameters( 
						Tracker.Event.STARTED, 0, 0, 0);
				scheduleAnnouncement(trackerSession, announceParameters, 0);
			}
			else {
				//Torrent is stopped, we'll only scrape tracker statistics for now
				scheduleScrape(tracker, trackerSession);
			}
		}
		
		System.out.println("SYNCHRONIZED.addTorrentTracker(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");
		
		return true;
	}
	
	public final boolean removeTracker(final Tracker tracker, final InfoHash infoHash) {
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {
			final Optional<TrackerSession> match = trackerSessions.stream().filter(
					t -> t.getInfoHash().equals(infoHash) && t.getTracker().equals(tracker)).findFirst();
			
			if(!match.isPresent()) {
				return false;
			}
			
			final TrackerSession trackerSession = match.get();
			final boolean removed = trackerSessions.remove(trackerSession);
			
			//Issue STOPPED announce to affected tracker, if not already STOPPED
			if(trackerSession.getLastTrackerEvent() != Tracker.Event.STOPPED) {
				final AnnounceParameters announceParameters = new AnnounceParameters( 
						Tracker.Event.STOPPED, 0, 0, 0);
				scheduleAnnouncement(trackerSession, announceParameters, 0);
			}
			
			System.out.println("SYNCHRONIZED.removeTracker(): Released lock after " 
					+ (System.currentTimeMillis() - start) + " ms.");
			
			return removed;
		}		
	}
	
	/**
	 * Stop tracking a torrent for all trackers that are serving it.
	 * Also remove trackers that no longer serve any torrents.
	 * 
	 * @param torrent Info hash of the torrent to remove
	 * @return Whether the torrent was successfully removed
	 */
	public final boolean removeTorrent(final InfoHash torrentInfoHash) {
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {		
			//Check if any tracked torrents match supplied info hash
			final List<TrackerSession> matchList = trackerSessions.stream().filter(
					t -> t.getInfoHash().equals(torrentInfoHash))
					.collect(Collectors.toList());
			
			if(matchList.isEmpty()) {
				//No trackers are tracking this torrent
				return false;
			}
			
			//Issue STOPPED announce to affected trackers, if not already STOPPED
			for(final TrackerSession trackerSession : matchList) {								
				trackerSessions.remove(trackerSession);
				
				if((trackerSession.getTracker().getType() == Tracker.Type.UDP &&
						trackerSession.getTracker().getId() != UdpTracker.DEFAULT_CONNECTION_ID) || 
						trackerSession.getLastTrackerEvent() != Tracker.Event.STOPPED) {
					
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							Tracker.Event.STOPPED, 0, 0, 0);
					scheduleAnnouncement(trackerSession, announceParameters, 0);
				}				
			}
			
			System.out.println("SYNCHRONIZED.removeTorrent(): Released lock after " 
					+ (System.currentTimeMillis() - start) + " ms.");
			
			return true;
		}
	}
	
	/**
	 * Add a listener to be notified when a new peer has been obtained
	 * 
	 * @param peerListener The listener to add
	 * @return Whether the listener was successfully added
	 */
	public final boolean addPeerListener(final PeerFoundListener peerListener) {
		return peerListeners.add(peerListener);
	}
	
	/**
	 * Remove a listener that was notified when a new peer has been obtained
	 * 
	 * @param peerListener The listener to remove
	 * @return Whether the listener was successfully removed
	 */
	public final boolean removePeerListener(final PeerFoundListener peerListener) {
		return peerListeners.remove(peerListener);
	}
	
	/**
	 * Cleanup after closing down the tracker manager
	 */
	public final void stop() {
		requestScheduler.shutdownNow();
		requestExecutor.shutdownNow();
	}
	
	private void onUdpTrackerConnect(final UdpTrackerResponse trackerResponse) {		
		Optional<TrackerSession> match = null;
		long connectionId = -1;		
		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(trackerResponse.getData()))) {
			final int actionId = dis.readInt();
			final int transactionId = dis.readInt();			
			connectionId = dis.readLong();
			
			if(actionId != UdpTracker.ACTION_CONNECT) {
				return;
			}
			
			match = scheduledRequests.keySet().stream().filter(
					t -> t.getTransactionId() == transactionId).findFirst();
			
			if(match != null && match.isPresent()) {
				//This was a response to a previous announce request
				
				System.out.println("onUdpTrackerConnectAnnounce(transaction_id = " + transactionId +
						", connection_id = " + connectionId + ", match = " + match + ")");
				
				final TrackerSession trackerSession = match.get();
				synchronized(trackerSession) {
					final long responseTime = System.currentTimeMillis();
					trackerSession.getTracker().setId(connectionId);
					trackerSession.getTracker().setLastResponse(responseTime);
					trackerSession.setLastTrackerResponse(responseTime);
				}	
				
				//START TEST
				final long start = System.currentTimeMillis();
				//END TEST
				
				synchronized(trackerSessions) {
					final AnnounceParameters params = scheduledRequests.get(trackerSession).getAnnounceParameters();				
					scheduleAnnouncement(trackerSession, params, 0);
				}
				
				System.out.println("SYNCHRONIZED.onUdpTrackerConnect(): Released lock after " 
						+ (System.currentTimeMillis() - start) + " ms.");
			}		
			else {
				//This was a response to a previous scrape request
				
				System.out.println("onUdpTrackerConnectScrape(transaction_id = " + transactionId +
						", connection_id = " + connectionId + ")");
				
				//TODO: Find matching transaction_id in Map<transaction_id, ScrapeRequest> where ScrapeRequest(Tracker, TrackerSession...)				
			}
		}
		catch(final IOException ioe) {
			System.err.println("onUdpTrackerConnect() Error: " + ioe.getMessage());
			return;
		}
	}
	
	private void onUdpTrackerAnnounce(final UdpTrackerResponse trackerResponse) {		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(trackerResponse.getData()))) {
			final int actionId = dis.readInt();			
			if(actionId != UdpTracker.ACTION_ANNOUNCE) {
				return;
			}
			final int transactionId = dis.readInt();
			final Optional<TrackerSession> match = scheduledRequests.keySet().stream().filter(
					t -> t.getTransactionId() == transactionId).findFirst();
			if(match != null && match.isPresent()) {
				final int interval = dis.readInt();
				final int leechers = dis.readInt();
				final int seeders = dis.readInt();
				
				final TrackerSession trackerSession = match.get();
				
				System.out.println("onUdpTrackerAnnounce(transaction_id: " + transactionId + 
						"): " + leechers + " leechers and " + seeders + " seeders. " +
						"Announce interval: " + interval);
				
				final AnnounceResponse announceResponse = new AnnounceResponse(TrackerResponse.Type.OK,
						null, interval, null, null, seeders, leechers, extractUdpTrackerPeers(dis, trackerSession.getInfoHash()));		
				onAnnounceResponseReceived(announceResponse, trackerSession);
			}
		}
		catch(final IOException ioe) {
			return;
		}		
	}
	
	private void onUdpTrackerScrape(final UdpTrackerResponse trackerResponse) {
		
		System.out.println("onUdpTrackerScrape()...");
		
		
		/*TODO: Implement method
		synchronized(trackerSession) {
				final long responseTime = System.currentTimeMillis();
				trackerSession.getTracker().setId(connectionId);
				trackerSession.getTracker().setLastResponse(responseTime);
				trackerSession.setLastTrackerResponse(responseTime);
			}
		*/
	}
	
	private void onUdpTrackerError(final UdpTrackerResponse trackerResponse) {
		//TODO: implement method
		
		System.out.print("onUdpTrackerError(): Message = " + trackerResponse.getMessage() + ", Data {");
		for(final byte b : trackerResponse.getData()) {
			System.out.print(b + ":");
		}
		System.out.println("}");
		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(trackerResponse.getData()))) {
			final int actionId = dis.readInt();			
			if(actionId != UdpTracker.ACTION_ERROR) {
				System.err.println("Invalid error action id: " + actionId);
				return;
			}
			final int transactionId = dis.readInt();
			
			System.err.println("transactionId: " + transactionId);
			
			final int messageLength = dis.available();
			
			System.err.println("messageLength: " + messageLength);
			
			final byte[] messageBytes = new byte[messageLength];
			dis.read(messageBytes);
			
			/*TODO:
			 * synchronized(trackerSession) {
				final long responseTime = System.currentTimeMillis();
				trackerSession.getTracker().setId(connectionId);
				trackerSession.getTracker().setLastResponse(responseTime);
				trackerSession.setLastTrackerResponse(responseTime);
			}
			 */
			
			System.err.println("message: " + new String(messageBytes));
		}
		catch(final IOException ioe) {
			ioe.printStackTrace();
			return;
		}
	}
	
	private Set<PwpPeer> extractUdpTrackerPeers(final DataInputStream dis, final InfoHash infoHash) throws IOException {
		final Set<PwpPeer> peers = new HashSet<>();		
		while(dis.available() >= UDP_TRACKER_RESPONSE_PEER_LENGTH) {
			final String peerIp = InetAddress.getByAddress(
					UnitConverter.getBytes(dis.readInt())).getHostAddress();
			final int peerPort = dis.readShort();
			peers.add(new PwpPeer(peerIp, peerPort, infoHash));
		}		
		return peers;
	}
	
	private boolean announceAllowed(final TrackerSession trackerSession) {
		final Long minInterval = trackerSession.getMinInterval();
		final long minAnnounceWaitInterval = minInterval != null? 
				minInterval : trackerSession.getInterval();
		return (System.nanoTime() - trackerSession.getLastTrackerResponse()) > minAnnounceWaitInterval;
	}
	
	private boolean isValidTrackerEvent(final Tracker.Event lastTrackerEvent, final Tracker.Event targetEvent) {		
		switch(targetEvent) {
		case COMPLETED:
			if(lastTrackerEvent == Event.COMPLETED || lastTrackerEvent == Event.STOPPED) {
				return false;
			}
			break;
		case STARTED:
			if(lastTrackerEvent == Event.STARTED || lastTrackerEvent == Event.UPDATE) {
				return false;
			}
			break;
		case STOPPED:
			if(lastTrackerEvent == Event.STOPPED) {
				return false;
			}
			break;
		case UPDATE:
			if(lastTrackerEvent == Event.STOPPED) {
				return false;
			}	
			break;
		}
		return true;
	}
	
	private Tracker initTracker(final String trackerUrl) {
		if(trackerUrl.toLowerCase().startsWith(NetworkUtilities.HTTP_PROTOCOL) ||
				trackerUrl.toLowerCase().startsWith(NetworkUtilities.HTTPS_PROTOCOL)) {			
			return new HttpTracker(trackerUrl, this);
		}
		else if(trackerUrl.toLowerCase().startsWith(NetworkUtilities.UDP_PROTOCOL)) {			
			try {
				return new UdpTracker(trackerUrl);
			} catch (final URISyntaxException use) {
				//Invalid tracker url, simply ignore the tracker
				System.err.println("Invalid UDP Tracker URL format (" + trackerUrl + "): " + use);
				return null;
			} 
		}
		return null;
	}
	
	private void scheduleAnnouncement(final TrackerSession trackerSession,
			final AnnounceParameters announceParameters, final long delay) {
		
		/*System.out.println("scheduleAnnouncement(url = " + trackerSession.getTracker().getUrl() + ", " +
				announceParameters.getTrackerEvent() + ", delay = " + delay + ")");*/
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		scheduledRequests.compute(trackerSession, (session, announcement) -> {
			if(!isValidTrackerEvent(trackerSession.getLastTrackerEvent(),
					announceParameters.getTrackerEvent())) {
				
				System.out.println(trackerSession.getTracker() + "Invalid tracker event: last: " 
						+ trackerSession.getLastTrackerEvent() +
						", current: " + announceParameters.getTrackerEvent());
				
				return announcement;
			}
			if(announcement != null) {
				announcement.getFuture().cancel(false);
			}
			
			final Tracker tracker = trackerSession.getTracker();			
			final Runnable request = () -> tracker.announce(announceParameters, trackerSession);
			
			if(tracker.getType() == Tracker.Type.TCP) {
				final Future<?> future = delay == 0? requestScheduler.submit(() ->
						sendRequest(tracker, trackerSession.getTransactionId(), request, trackerSession)) :
					requestScheduler.schedule(() -> sendRequest(tracker, trackerSession.getTransactionId(), 
							request, trackerSession), delay, TimeUnit.SECONDS);
				return new ScheduledAnnouncement(announceParameters, future);
			}
			else {
				final Future<?> future = requestScheduler.scheduleAtFixedRate(
						() -> sendRequest(tracker, trackerSession.getTransactionId(), request, trackerSession), 
						delay * 1000, UdpTracker.CONNECTION_ATTEMPT_DELAY, TimeUnit.MILLISECONDS);
				return new ScheduledAnnouncement(announceParameters, future);
			}
		});
		
		System.out.println("SYNCHRONIZED.scheduleRequests.compute(): Released lock after " +
				(System.currentTimeMillis() -start) + " ms.");
	}
	
	private void scheduleScrape(final Tracker tracker, final TrackerSession... torrents) {
		
		//START TEST
		final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {
			if(tracker.isScrapeSupported()) {				
				tracker.setScrapeTransactionId(ClientProperties.generateUniqueId());
				
				System.out.println("scheduleScrape(tracker = " + tracker.getUrl() + 
						", transaction_id = " + tracker.getScrapeTransactionId() + ")");
				
				final Runnable request = () -> {					
					tracker.scrape(torrents);
					tracker.setLastScrape(System.currentTimeMillis());
				};
				requestExecutor.submit(() -> sendRequest(tracker, tracker.getScrapeTransactionId(), request , torrents));				
			}
		}
		
		System.out.println("SYNCHRONIZED.scheduleScrape(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");
	}
	
	private void sendRequest(final Tracker tracker, final int transactionId,
			final Runnable request, final TrackerSession... trackerSessions) {
		
		/* Send TCP requests in their own thread, because sending a TCP request
		   blocks until a response is received */		
		if(tracker.getType() == Tracker.Type.TCP) {
			requestExecutor.execute(request);
		}
		else {
			if(isValidConnection(tracker)) {
				request.run();
			}
			else {
				final int connectionAttempt = tracker.connect(transactionId);
				if(connectionAttempt > UdpTracker.MAX_CONNECTION_ATTEMPTS) {
					
					System.out.println("sendRequest(): Connection timed out, cancelling...");
					
					Arrays.stream(trackerSessions).forEach(ts -> {
						scheduledRequests.compute(ts, (session, announcement) -> {
							if(announcement != null) {
								announcement.getFuture().cancel(false);
							}
							return announcement;
						});
					});
				}
			}
		}
	}
	
	private boolean isValidConnection(final Tracker tracker) {
		synchronized(trackerSessions) {			
			return (tracker.getId() != UdpTracker.DEFAULT_CONNECTION_ID) &&
				((System.currentTimeMillis() - tracker.getLastResponse()) <= 
				UDP_TRACKER_CONNECTION_ID_TIMEOUT);
		}
	}
}