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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.net.udp.UdpRequest;
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
	private final Map<Integer, List<TrackerSession>> scheduledUdpScrapes = new ConcurrentHashMap<>();
	
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
		final long responseTime = System.currentTimeMillis();
		trackerSession.getTracker().setLastResponse(responseTime);
		trackerSession.setLastTrackerResponse(responseTime);
		
		//START TEST
		//final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {				
			if(trackerSessions.contains(trackerSession) && 
					trackerSession.getLastTrackerEvent() != Tracker.Event.STOPPED) {
				trackerSession.setTrackerMessage(announceResponse.getMessage());
				
				//Check whether it was an error response before scheduling
				if(announceResponse.getType() != TrackerResponse.Type.OK) {
					
					System.err.println("Announce returned an error ( " + announceResponse.getType() + "): "
							+ trackerSession.getTracker() + ", msg = " + announceResponse.getMessage());
					
					trackerSession.setTrackerStatus(Tracker.Status.ERROR);	
					trackerSession.setTrackerMessage(announceResponse.getMessage());
					
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							trackerSession.getLastTrackerEvent(), 0, 0, 0);
					trackerSession.setLastTrackerEvent(Event.STOPPED);
					scheduleAnnouncement(trackerSession, announceParameters, ANNOUNCE_DELAY_ON_TRACKER_ERROR);
				}
				else {
					trackerSession.setTrackerStatus(Tracker.Status.WORKING);
					trackerSession.setTrackerMessage("announce ok");
					trackerSession.setInterval(announceResponse.getInterval());
					trackerSession.setMinInterval(announceResponse.getMinInterval());
					
					final AnnounceParameters announceParameters = new AnnounceParameters( 
							Tracker.Event.UPDATE, 0, 0, 0);
					scheduleAnnouncement(trackerSession, announceParameters, announceResponse.getInterval());
				}
			}
			else {
				//Cancel any scheduled requests as we have STOPPED
				System.out.println("onAnnounceResponseReceived(" + trackerSession.getTracker().getUrl() + 
						": Cancelling due to STOPPED");
				
				trackerSession.setTrackerStatus(Tracker.Status.UNKNOWN);
				trackerSession.setTrackerMessage(announceResponse.getMessage());
				
				scheduledRequests.compute(trackerSession, (session, announcement) -> {
					final Future<?> future = announcement.getFuture(); 
					future.cancel(false);
					return null;
				});
			}
		}
		
		/*System.out.println("SYNCHRONIZED.onAnnounceResponseReceived(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");*/
	}	
	
	/**
	 * @see HttpTrackerResponseListener#onScrapeResponseReceived(Tracker, ScrapeResponse)
	 */
	@Override
	public void onScrapeResponseReceived(final Tracker tracker, final ScrapeResponse scrapeResponse) {
		
		System.out.println("onScrapeResponseReceived(" + tracker.getUrl() + "): " 
				+ scrapeResponse.getScrapeStatistics());
		
		final long responseTime = System.currentTimeMillis();
		tracker.setLastScrape(responseTime);
		
		final Map<TrackerSession, ScrapeStatistics> scrapeStatistics = scrapeResponse.getScrapeStatistics();
		scrapeStatistics.entrySet().stream().forEach(stat -> {
			final TrackerSession trackerSession = stat.getKey();
			final ScrapeStatistics scrapeStat = stat.getValue();
			
			trackerSession.setLeechers(scrapeStat.getIncomplete());
			trackerSession.setSeeders(scrapeStat.getComplete());
			trackerSession.setDownloaded(scrapeStat.getDownloaded());
			
			if(trackerSession.getLastTrackerEvent() == Tracker.Event.STOPPED) {
				trackerSession.setLastTrackerResponse(responseTime);
				trackerSession.setTrackerStatus(Tracker.Status.SCRAPE_OK);
				trackerSession.setTrackerMessage("Scrape ok");
			}
		});
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
	 * @see UdpTrackerResponseListener#onUdpTrackerRequestError(UdpRequest, String)
	 */
	@Override
	public final void onUdpTrackerRequestError(final UdpRequest request, final String message) {
		
		System.err.println("onUdpTrackerRequestError(transaction_id = " + request.getId() + "): " + message);
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(8 + message.length());
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeInt(UdpTracker.ACTION_ERROR);
			dos.writeInt(request.getId());
			dos.write(message.getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		} 
		catch (final IOException ioe) {
			System.err.println(ioe.getMessage());
			return;
		}
		
		final UdpTrackerResponse response = new UdpTrackerResponse(baos.toByteArray(), UdpTracker.ACTION_ERROR, message);
		onUdpTrackerError(response);
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
		//final long start = System.currentTimeMillis();
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
			
			/*System.out.println("SYNCHRONIZED.issueAnnounce(): Released lock after " 
					+ (System.currentTimeMillis() - start) + " ms.");*/
			
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
	 * @return Created tracker session or null, if session already exists 
	 */
	public TrackerSession addTracker(final String trackerUrl, final InfoHash infoHash,
			final boolean shouldAnnounce) {
		final Tracker tracker = initTracker(trackerUrl);		
		final TrackerSession trackerSession = new TrackerSession(infoHash, tracker); 		
		
		//START TEST
		//final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {			
			if(!trackerSessions.add(trackerSession)) {
				return null;
			}
			
			if(tracker.getType() == Tracker.Type.INVALID) {
				trackerSession.setTrackerStatus(Tracker.Status.ERROR);
				trackerSession.setTrackerMessage("Invalid URL");
				return trackerSession;
			}
			
			if(shouldAnnounce) {
				final AnnounceParameters announceParameters = new AnnounceParameters( 
						Tracker.Event.STARTED, 0, 0, 0);
				scheduleAnnouncement(trackerSession, announceParameters, 0);
			}
						
			//Always scrape tracker statistics, if supported
			if(tracker.isScrapeSupported()) {				
				scheduleScrape(tracker, trackerSession);
			}
			else {
				trackerSession.setTrackerStatus(Tracker.Status.SCRAPE_NOT_SUPPORTED);
				trackerSession.setTrackerMessage("Scrape not supported");
			}
		}
		
		/*System.out.println("SYNCHRONIZED.addTorrentTracker(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");*/
		
		return trackerSession;
	}
	
	/**
	 * Remove a tracker that is tracking a torrent
	 * 
	 * @param trackerUrl Tracker's URL
	 * @param infoHash Target torrent's info hash
	 * @return Whether the tracker was removed
	 */
	public final boolean removeTracker(final String trackerUrl, final InfoHash infoHash) {		
		synchronized(trackerSessions) {
			final List<TrackerSession> match = trackerSessions.stream().filter(
					t -> t.getInfoHash().equals(infoHash) && t.getTracker().getUrl().equals(trackerUrl))
						.collect(Collectors.toList());
			
			return removeSessions(match) > 0;
		}		
	}
	
	/**
	 * Remove a torrent from all trackers that are tracking it.
	 * 
	 * @param torrentInfoHash Info hash of the torrent to remove
	 * @return Whether the torrent was successfully removed
	 */
	public final boolean removeTorrent(final InfoHash torrentInfoHash) {
		
		//START TEST
		//final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {		
			//Check if any tracked torrents match supplied info hash
			final List<TrackerSession> matchList = trackerSessions.stream().filter(
					t -> t.getInfoHash().equals(torrentInfoHash))
					.collect(Collectors.toList());
			
			return removeSessions(matchList) > 0;
			
			/*System.out.println("SYNCHRONIZED.removeTorrent(): Released lock after " 
					+ (System.currentTimeMillis() - start) + " ms.");*/			
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
	
	private int removeSessions(final List<TrackerSession> sessionList) {
		if(sessionList.isEmpty()) {			
			return 0;
		}
		
		int removedCount = 0;
		
		//Remove and issue STOPPED announce to affected tracker(s), if not already STOPPED
		for(final TrackerSession trackerSession : sessionList) {											
			if(!trackerSessions.remove(trackerSession)) {
				continue;
			}
			
			++removedCount;
			
			if(trackerSession.getLastTrackerEvent() != Tracker.Event.STOPPED) {					
				final AnnounceParameters announceParameters = new AnnounceParameters( 
						Tracker.Event.STOPPED, 0, 0, 0);
				scheduleAnnouncement(trackerSession, announceParameters, 0);
			}
			else {
				scheduledRequests.computeIfPresent(trackerSession, (key, value) -> {
					//Cancel any pending requests for this tracker session
					
					System.out.println("remove(List<TrackerSession>): Canceling requests for transaction_id = " +
							trackerSession.getTransactionId());
					
					value.getFuture().cancel(false);
					return null;
				});
			}
			final Tracker tracker = trackerSession.getTracker();
			if(tracker.getType() == Tracker.Type.UDP) {
				//Remove any pending scrape requests
				scheduledUdpScrapes.computeIfPresent(tracker.getScrapeTransactionId(), 
					(id, sessions) -> {
						sessions.remove(trackerSession);
						return sessions.isEmpty()? null : sessions;
					}
				);
			}
		}
		
		return removedCount;
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
				
				//START TEST
				//final long start = System.currentTimeMillis();
				//END TEST
				
				synchronized(trackerSessions) {
					final long responseTime = System.currentTimeMillis();
					trackerSession.getTracker().setId(connectionId);
					trackerSession.getTracker().setLastResponse(responseTime);
					trackerSession.setLastTrackerResponse(responseTime);
					
					final AnnounceParameters params = scheduledRequests.get(trackerSession).getAnnounceParameters();				
					scheduleAnnouncement(trackerSession, params, 0);
				}
				
				/*System.out.println("SYNCHRONIZED.onUdpTrackerConnect(): Released lock after " 
						+ (System.currentTimeMillis() - start) + " ms.");*/
			}		
			else {
				//This was a response to a previous scrape request
				
				System.out.println("onUdpTrackerConnectScrape(transaction_id = " + transactionId +
						", connection_id = " + connectionId + ")");
									
				final List<TrackerSession> scrapeSessions = scheduledUdpScrapes.remove(transactionId);
				if(scrapeSessions != null && !scrapeSessions.isEmpty()) {
					final long responseTime = System.currentTimeMillis();
					
					final Tracker tracker = scrapeSessions.get(0).getTracker();
					tracker.setLastResponse(responseTime);
					tracker.setId(connectionId);
					
					scrapeSessions.stream().forEach(s -> s.setLastTrackerResponse(responseTime));
					scheduleScrape(tracker, scrapeSessions.stream().toArray(TrackerSession[]::new));						
				}				
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
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(trackerResponse.getData()))) {
			final int actionId = dis.readInt();			
			if(actionId != UdpTracker.ACTION_SCRAPE) {
				return;
			}
			
			final int transactionId = dis.readInt();
			final List<TrackerSession> scrapedTrackerSessions = scheduledUdpScrapes.remove(transactionId);
			
			if(scrapedTrackerSessions == null || scrapedTrackerSessions.isEmpty()) {
				System.err.println("No pending scrape found for transaction_id: " + transactionId);
				return;
			}
			
			final Map<TrackerSession, ScrapeStatistics> sessionStatistics = new HashMap<>();
			
			for(final TrackerSession trackerSession : scrapedTrackerSessions) {
				final int complete = dis.readInt();
				final int downloaded = dis.readInt();
				final int incomplete = dis.readInt();
				
				final ScrapeStatistics scrapeStatistics = new ScrapeStatistics(
						complete, downloaded, incomplete, null);
				
				sessionStatistics.put(trackerSession, scrapeStatistics);
			}
			
			final ScrapeResponse scrapeResponse = new ScrapeResponse(TrackerResponse.Type.OK, 
					null, Collections.emptyMap(), sessionStatistics);
			onScrapeResponseReceived(scrapedTrackerSessions.get(0).getTracker(), scrapeResponse);
		} 
		catch (final IOException ioe) {
			System.err.println("onUdpTrackerScrape(): " + ioe.getMessage());
			return;
		}
	}
	
	private void onUdpTrackerError(final UdpTrackerResponse trackerResponse) {		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(trackerResponse.getData()))) {
			final int actionId = dis.readInt();			
			if(actionId != UdpTracker.ACTION_ERROR) {
				System.err.println("Invalid error action id: " + actionId);
				return;
			}
			final int transactionId = dis.readInt();
			final int messageLength = dis.available();
			final byte[] messageBytes = new byte[messageLength];
			dis.read(messageBytes);
			
			final long lastResponse = System.currentTimeMillis();
			final Consumer<TrackerSession> sessionStatusUpdate = (ts) -> {
				ts.setLastTrackerResponse(lastResponse);
				ts.setTrackerStatus(Tracker.Status.ERROR);
				ts.setTrackerMessage(new String(messageBytes,
						ClientProperties.STRING_ENCODING_CHARSET));
			};
			
			//Update matching sessions if the originating request was a scrape
			scheduledUdpScrapes.computeIfPresent(transactionId, (id, scrapes) -> {
				
				System.out.println("onUdpTrackerError(transaction_id = " + transactionId +
						"): Removing matching scrapes");
				
				scrapes.forEach(scrape -> sessionStatusUpdate.accept(scrape));
				if(!scrapes.isEmpty()) {
					scrapes.get(0).getTracker().setLastResponse(lastResponse);
				}
				return null;
			});
			
			//Update matching tracker session if the originating request was an announce
			synchronized(trackerSessions) {
				trackerSessions.stream().filter(ts -> ts.getTransactionId() == transactionId).forEach(
					match -> sessionStatusUpdate.accept(match));
			}
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
			try {
				new URL(trackerUrl);				
			}
			catch(final MalformedURLException mue) {
				//Invalid tracker url, return an instance of InvalidTracker
				return new InvalidTracker(trackerUrl);
			}
			
			final HttpTracker httpTracker = new HttpTracker(trackerUrl);
			httpTracker.addListener(this);
			return httpTracker;
		}
		else if(trackerUrl.toLowerCase().startsWith(NetworkUtilities.UDP_PROTOCOL)) {			
			try {
				final URI udpTrackerUri = new URI(trackerUrl);
				return new UdpTracker(udpTrackerUri);
			} catch (final URISyntaxException use) {				
				System.err.println("Invalid UDP Tracker URL format (" + trackerUrl + "): " + use);
				
				//Invalid tracker url, return an instance of InvalidTracker
				return new InvalidTracker(trackerUrl);
			} 
		}
		//Unsupported tracker protocol, return an instance of InvalidTracker
		return new InvalidTracker(trackerUrl);
	}
	
	private void scheduleAnnouncement(final TrackerSession trackerSession,
			final AnnounceParameters announceParameters, final long delay) {
		
		//START TEST
		//final long start = System.currentTimeMillis();
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
			final Runnable request = () -> {
				trackerSession.setTrackerMessage("announcing...");
				tracker.announce(announceParameters, trackerSession);
			};
			
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
		
		/*System.out.println("SYNCHRONIZED.scheduleRequests.compute(): Released lock after " +
				(System.currentTimeMillis() -start) + " ms.");*/
	}
	
	private void scheduleScrape(final Tracker tracker, final TrackerSession... torrents) {
		
		//START TEST
		//final long start = System.currentTimeMillis();
		//END TEST
		
		synchronized(trackerSessions) {
			if(tracker.isScrapeSupported()) {				
				final int scrapeTransactionId = ClientProperties.generateUniqueId();
				tracker.setScrapeTransactionId(scrapeTransactionId);
				
				if(tracker.getType() == Tracker.Type.UDP) {
					scheduledUdpScrapes.compute(scrapeTransactionId, (id, trackerSessions) -> {
						return Arrays.stream(torrents).collect(Collectors.toList());
					});
				}
				
				System.out.println("scheduleScrape(tracker = " + tracker.getUrl() + 
						", transaction_id = " + tracker.getScrapeTransactionId() + ")");
				
				final Runnable request = () -> tracker.scrape(torrents);
				requestScheduler.submit(() -> sendRequest(tracker, scrapeTransactionId, request , torrents));				
			}
		}
		
		/*System.out.println("SYNCHRONIZED.scheduleScrape(): Released lock after " 
				+ (System.currentTimeMillis() - start) + " ms.");*/
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
					Arrays.stream(trackerSessions).forEach(ts -> {											
						ts.setTrackerStatus(Tracker.Status.CONNECTION_TIMEOUT);
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