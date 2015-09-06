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

import static org.easymock.EasyMock.*;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.tracking.listeners.PeerFoundListener;

public final class TrackerManagerTest {
	
	private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEF0123")); 
	private final QueuedTorrent torrent = new QueuedTorrent(infoHash, 1, QueuedTorrent.State.ACTIVE);
		
	private TrackerManager unitUnderTest;
	private TrackerSession httpTrackerSession;
	//private TrackerSession udpTrackerSession;
	private PeerFoundListener peerFoundListenerMock;
	private ScheduledExecutorService requestSchedulerMock;
	private UdpConnectionManager udpConnectionManagerMock;
	
	//Mandatory response fields	
	private final long interval = 60 * 30 * 1000;
	private final long incomplete = 77;
	private final long complete = 12;
		
	private final String trackerId = "trackerId";
	
	@Before
	public void setup() throws URISyntaxException {				
		udpConnectionManagerMock = createMock(UdpConnectionManager.class);
		requestSchedulerMock = createMock(ScheduledExecutorService.class);
		peerFoundListenerMock = createMock(PeerFoundListener.class);
		
		/*udpTrackerSession = new TrackerSession(torrent, new UdpTracker(
				new URI("udp://localhost:12345"), udpConnectionManagerMock));*/
		httpTrackerSession = new TrackerSession(torrent, new HttpTracker("http://localhost:12345"));		
		
		unitUnderTest = new TrackerManager(udpConnectionManagerMock, requestSchedulerMock);
		unitUnderTest.addPeerListener(peerFoundListenerMock);
	}
	
	@After
	public void cleanup() {
		unitUnderTest.removePeerListener(peerFoundListenerMock);
		reset(udpConnectionManagerMock, requestSchedulerMock, peerFoundListenerMock);
	}

	@Test
	public void testAnnounceReceivedAndNoPeers() {
		/*final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				null, interval, interval, trackerId, complete, incomplete, Collections.emptySet());
		final ScheduledFuture<?> futureMock = createMock(ScheduledFuture.class);
		
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(0), eq(15000), eq(TimeUnit.MILLISECONDS))).andReturn(futureMock);
		
		replay(requestSchedulerMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrent);
		//unitUnderTest.onAnnounceResponseReceived(response, udpTrackerSession);
		
		verify(requestSchedulerMock);
		
		Assert.assertTrue(trackerSessionCreated);*/
	}
	
	@Test
	public void testAnnounceReceivedAndNoMatchingSession() {
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				null, interval, interval, trackerId, complete, incomplete, Collections.emptySet());
				
		replay(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);
		
		unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);
		
		verify(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);
	}
	
	@Test
	public void testAnnounceReceivedAndSessionStopped() {
		
	}
	
	@Test
	public void testAnnounceReceivedAndIsError() {
		
	}
	
	@Test
	public void testAnnounceReceivedAndIsOK() {
		
	}
	
	@Test
	public void testScrapeReceivedAndSessionActive() {
		
	}
	
	@Test
	public void testScrapeReceivedAndSessionStopped() {
		
	}
	
	@Test
	public void testOnUdpTrackerRequestError() {
		
	}
	
	@Test
	public void testIssueAnnounce() {
		
	}
	
	@Test
	public void testIssueNotAllowedAnnounce() {
		
	}
	
	@Test
	public void testIssueAnnounceInvalidTracker() {
		
	}
	
	@Test
	public void testIssueTorrentEvent() {
		
	}
	
	@Test
	public void testAddHttpTracker() {
		
	}
	
	@Test
	public void testAddUdpTracker() {
		
	}
	
	@Test
	public void testAddInvalidTracker() {
		
	}
	
	@Test
	public void testAddTrackerAndTorrentStopped() {
		
	}
	
	@Test
	public void testAddAlreadyAddedTracker() {
		
	}
	
	@Test
	public void testRemoveTracker() {
		
	}
	
	@Test
	public void testRemoveNonExistingTracker() {
		
	}
	
	@Test
	public void testRemoveTorrentSession() {
		
	}
	
	@Test
	public void testRemoveNonExistingSession() {
		
	}
}
