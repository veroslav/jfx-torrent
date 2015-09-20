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


public final class TrackerManagerTest {
	
	/*private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEF0123")); 
	private final QueuedTorrent torrent = new QueuedTorrent(infoHash, 1, QueuedTorrent.State.ACTIVE);
		
	private UdpConnectionManager udpConnectionManagerMock = createMock(UdpConnectionManager.class);	
	
	private ScheduledExecutorService requestSchedulerMock = createMock(ScheduledExecutorService.class);
	private PeerFoundListener peerFoundListenerMock = createMock(PeerFoundListener.class);	
	
	private TrackerSession httpTrackerSession;
	private TrackerSession udpTrackerSession;
	private UdpTracker udpTracker;
	
	private final String trackerMessage = "Tracker message";
	
	private TrackerManager unitUnderTest = new TrackerManager(udpConnectionManagerMock, requestSchedulerMock);
	
	//Mandatory response fields	
	private final long minInterval = 30 * 30 * 1000;
	private final long interval = 60 * 30 * 1000;	
	private final long incomplete = 77;
	private final long complete = 12;
		
	private final String trackerId = "trackerId";
	
	@Before
	public void setup() throws URISyntaxException {	
		unitUnderTest.addPeerListener(peerFoundListenerMock);
		torrent.setState(QueuedTorrent.State.ACTIVE);
		udpTracker = new UdpTracker(new URI("udp://localhost:12345"), udpConnectionManagerMock);
		udpTrackerSession = new TrackerSession(torrent, udpTracker);
		
		httpTrackerSession = new TrackerSession(torrent, new HttpTracker("http://localhost:12345/announce"));
	}
	
	@After
	public void cleanup() {
		unitUnderTest.removePeerListener(peerFoundListenerMock);		
		reset(udpConnectionManagerMock, peerFoundListenerMock, requestSchedulerMock);
	}
	
	@SuppressWarnings({ "unchecked" })
	@Test
	public void testOnAnnounceResponseAndOnePeer() {
		final Set<PwpPeer> peers = new HashSet<>(Arrays.asList(new PwpPeer("128.0.0.1", 42, infoHash)));		
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				trackerMessage, interval, minInterval, trackerId, complete, incomplete, peers);
		
		//Expect an HTTP tracker announcement after a response to an earlier announcement request
		expect(requestSchedulerMock.schedule(anyObject(Runnable.class), 
				eq(interval), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		//Expect three HTTP tracker submits (initial announcement + two scrape requests)
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(3);
		
		//Expect notification when a new peer is obtained
		peerFoundListenerMock.onPeersFound(peers);
		expectLastCall();
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		
		//Validate correctness of the connection request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> initialRequest = 
				unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker());
		Assert.assertNotNull(initialRequest);
		final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();
		
		final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
		Assert.assertNotNull(initialRequestParameters);
		Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());
		
		//We need to set correct last tracker event before receiving the tracker's response
		httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);		
		unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session
		Assert.assertEquals(Tracker.Status.WORKING, httpTrackerSession.getTrackerStatus());
		Assert.assertEquals(trackerMessage, httpTrackerSession.getTrackerMessage());
		Assert.assertEquals(minInterval, httpTrackerSession.getMinInterval());
		Assert.assertEquals(interval, httpTrackerSession.getInterval());
		
		//Validate correctness of announce request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> scheduledRequest = 
				unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker());
		Assert.assertNotNull(scheduledRequest);
		final ScheduledAnnouncement scheduledAnnounceRequest = scheduledRequest.getValue();
		
		final AnnounceParameters regularRequestParameters = scheduledAnnounceRequest.getAnnounceParameters();		
		Assert.assertEquals(Tracker.Event.UPDATE, regularRequestParameters.getTrackerEvent());
		
		final AnnounceParameters scheduledAnnounceParameters = scheduledAnnounceRequest.getAnnounceParameters();
		Assert.assertNotNull(scheduledAnnounceParameters);
		Assert.assertEquals(Tracker.Event.UPDATE, scheduledAnnounceParameters.getTrackerEvent());
	}

	@SuppressWarnings({ "unchecked" })	
	@Test
	public void testOnAnnounceResponseAndNoPeers() {
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				null, interval, minInterval, trackerId, complete, incomplete, Collections.emptySet());
		
		//Expect an UDP connection request
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(0L), eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		//Expect an UDP tracker announcement after a response to an earlier announcement request
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(interval), eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		//Expect two UDP tracker scrape requests (for each ONE connection and ONE announcement request)
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
				
		final boolean trackerSessionCreated = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrent);
		
		//Validate correctness of the connection request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> initialRequest = 
				unitUnderTest.getScheduledRequest(torrent, udpTrackerSession.getTracker());
		Assert.assertNotNull(initialRequest);
		final ScheduledAnnouncement connectionRequest = initialRequest.getValue();
		
		final AnnounceParameters connectionRequestParameters = connectionRequest.getAnnounceParameters();
		Assert.assertNotNull(connectionRequestParameters);
		Assert.assertEquals(Tracker.Event.STARTED, connectionRequestParameters.getTrackerEvent());
		
		//We need to set correct last tracker event before receiving the tracker's response
		udpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);		
		unitUnderTest.onAnnounceResponseReceived(response, udpTrackerSession);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session
		Assert.assertEquals(Tracker.Status.WORKING, udpTrackerSession.getTrackerStatus());
		Assert.assertEquals(null, udpTrackerSession.getTrackerMessage());
		Assert.assertEquals(minInterval, udpTrackerSession.getMinInterval());
		Assert.assertEquals(interval, udpTrackerSession.getInterval());
		
		//Validate correctness of announce request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> normalRequest = 
				unitUnderTest.getScheduledRequest(torrent, udpTrackerSession.getTracker());
		Assert.assertNotNull(normalRequest);
		final ScheduledAnnouncement announceRequest = normalRequest.getValue();		
		
		final AnnounceParameters announceParameters = announceRequest.getAnnounceParameters();
		Assert.assertNotNull(announceParameters);
		Assert.assertEquals(Tracker.Event.UPDATE, announceParameters.getTrackerEvent());		
	}
	
	@Test
	public void testAnnounceReceivedAndNoMatchingSession() {
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				null, interval, interval, trackerId, complete, incomplete, Collections.emptySet());
				
		replay(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);
		
		unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);
		
		verify(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnAnnounceResponseAndSessionStopped() {		
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
				trackerMessage, interval, minInterval, trackerId, complete, incomplete, Collections.emptySet());
		
		//Expect an initial announce and scrape request
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		
		//Validate correctness of the connection request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> initialRequest = 
				unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker());
		Assert.assertNotNull(initialRequest);
		final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();
		
		final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
		Assert.assertNotNull(initialRequestParameters);
		Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());
		
		//We need to set correct last tracker event before receiving the tracker's response
		httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);
		torrent.setState(QueuedTorrent.State.STOPPED);
		unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Check that any scheduled requests for the session have been cancelled and removed
		Assert.assertNull(unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker()));
		
		//Validate correctness of tracker session (should be reset as the session has been stopped)
		Assert.assertEquals(Tracker.Event.STOPPED, httpTrackerSession.getLastAcknowledgedEvent());
		Assert.assertEquals(Tracker.Status.UNKNOWN, httpTrackerSession.getTrackerStatus());
		Assert.assertEquals("", httpTrackerSession.getTrackerMessage());
		Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, httpTrackerSession.getMinInterval());
		Assert.assertEquals(0, httpTrackerSession.getInterval());		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnAnnounceResponseAndIsError() {
		final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.READ_WRITE_ERROR,
				trackerMessage, interval, minInterval, trackerId, complete, incomplete, Collections.emptySet());
		
		//Expect an initial announce and two scrape requests
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(3);
		
		//Expect an HTTP announcement scheduled after a tracker error has occurred
		expect(requestSchedulerMock.schedule(anyObject(Runnable.class), eq(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR),
				eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		
		//Validate correctness of the connection request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> initialRequest = 
				unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker());
		Assert.assertNotNull(initialRequest);
		final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();
		
		final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
		Assert.assertNotNull(initialRequestParameters);
		Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());
		
		unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session
		Assert.assertEquals(Tracker.Status.TRACKER_ERROR, httpTrackerSession.getTrackerStatus());
		Assert.assertEquals(trackerMessage, httpTrackerSession.getTrackerMessage());
		Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, httpTrackerSession.getMinInterval());
		Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, httpTrackerSession.getInterval());
		
		//Validate correctness of announce request parameters
		final Entry<TrackerSession, ScheduledAnnouncement> regularRequest = 
				unitUnderTest.getScheduledRequest(torrent, httpTrackerSession.getTracker());
		Assert.assertNotNull(regularRequest);
		final ScheduledAnnouncement scheduledAnnounceRequest = regularRequest.getValue();
				
		final AnnounceParameters regularRequestParameters = scheduledAnnounceRequest.getAnnounceParameters();
		Assert.assertNotNull(regularRequestParameters);
		Assert.assertEquals(Tracker.Event.STARTED, regularRequestParameters.getTrackerEvent());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnHttpScrapeResponse() {
		final int downloaded = 42;
		final int leechers = 18;
		final int seeders = 10;	
		
		final String name = "torrent_name";
		
		final ScrapeStatistics stats = new ScrapeStatistics(seeders, downloaded, leechers, name);
		final Map<TrackerSession, ScrapeStatistics> scrapeStats = new HashMap<>();
		scrapeStats.put(httpTrackerSession, stats);
		final ScrapeResponse scrapeResponse = new ScrapeResponse(TrackerResponse.Type.OK, null,
				Collections.emptyMap(), scrapeStats);
		
		//Expect initial announce and scrape requests
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		
		unitUnderTest.onScrapeResponseReceived(httpTrackerSession.getTracker(), scrapeResponse);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session (should be updated with scraped values)
		Assert.assertEquals(downloaded, httpTrackerSession.getDownloaded());
		Assert.assertEquals(leechers, httpTrackerSession.getLeechers());
		Assert.assertEquals(seeders, httpTrackerSession.getSeeders());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnUdpScrapeResponse() throws IOException {	
		final int downloaded = 42;
		final int leechers = 18;
		final int seeders = 10;	
		
		//Expect an UDP tracker scrape request
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		torrent.setState(QueuedTorrent.State.STOPPED);
		final boolean trackerSessionCreated = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrent);
		
		final TrackerSession scrapedSession = unitUnderTest.getScheduledUdpScrapeEntry(torrent, udpTracker);
		Assert.assertNotNull(scrapedSession);
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream daos = new DataOutputStream(baos);
		daos.writeInt(UdpTracker.ACTION_SCRAPE);
		daos.writeInt(scrapedSession.getTracker().getScrapeTransactionId());
		daos.writeInt(seeders);		
		daos.writeInt(downloaded);
		daos.writeInt(leechers);
		
		final UdpTrackerResponse scrapeResponse = new UdpTrackerResponse(baos.toByteArray(), UdpTracker.ACTION_SCRAPE, null);
	
		unitUnderTest.onUdpTrackerResponseReceived(scrapeResponse);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session (should be updated with scraped values)
		Assert.assertEquals(downloaded, scrapedSession.getDownloaded());
		Assert.assertEquals(leechers, scrapedSession.getLeechers());
		Assert.assertEquals(seeders, scrapedSession.getSeeders());
		Assert.assertEquals(scrapedSession.getTrackerStatus(), Tracker.Status.SCRAPE_OK);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOnScrapeResponseError() throws IOException {
		final String errorMessage = "Scrape error";
		
		//Expect an UDP tracker scrape request
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		torrent.setState(QueuedTorrent.State.STOPPED);
		final boolean trackerSessionCreated = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrent);
		
		final TrackerSession scrapedSession = unitUnderTest.getScheduledUdpScrapeEntry(torrent, udpTracker);
		Assert.assertNotNull(scrapedSession);
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream daos = new DataOutputStream(baos);
		daos.writeInt(UdpTracker.ACTION_ERROR);
		daos.writeInt(scrapedSession.getTracker().getScrapeTransactionId());
		daos.write(errorMessage.getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		
		final UdpTrackerResponse scrapeResponse = new UdpTrackerResponse(baos.toByteArray(), UdpTracker.ACTION_ERROR, null);
		
		unitUnderTest.onUdpTrackerResponseReceived(scrapeResponse);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session
		Assert.assertEquals(Tracker.Status.TRACKER_ERROR, scrapedSession.getTrackerStatus());
		Assert.assertEquals(errorMessage, scrapedSession.getTrackerMessage());
		Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, scrapedSession.getMinInterval());
		Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, scrapedSession.getInterval());
		Assert.assertNull(unitUnderTest.getScheduledUdpScrapeEntry(torrent, udpTracker));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testOnUdpTrackerAnnounceError() throws IOException {
		final String errorMessage = "Announce error";
		
		//Expect an UDP connection request
		final ScheduledFuture futureMock = createMock(ScheduledFuture.class);
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(0L), eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(futureMock);
		
		//Expect a cancellation of original announce request when re-scheduling on tracker error
		expect(futureMock.cancel(false)).andReturn(true);
		
		//Expect three UDP tracker scrape requests
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(3);
		
		//Expect an UDP announcement scheduled after a tracker error has occurred
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), eq(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR),
				eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock, futureMock);
		
		final boolean trackerSessionCreated = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrent);
		
		final Entry<TrackerSession, ScheduledAnnouncement> announceRequest = 
				unitUnderTest.getScheduledRequest(torrent, udpTrackerSession.getTracker());
		Assert.assertNotNull(announceRequest);
		
		final ScheduledAnnouncement originalAnnouncement = announceRequest.getValue();
		final TrackerSession trackerSession = announceRequest.getKey();		
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream daos = new DataOutputStream(baos);
		daos.writeInt(UdpTracker.ACTION_ERROR);
		daos.writeInt(trackerSession.getTransactionId());
		daos.write(errorMessage.getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		
		final UdpTrackerResponse announceResponse = new UdpTrackerResponse(baos.toByteArray(), UdpTracker.ACTION_ERROR, null);
		
		unitUnderTest.onUdpTrackerResponseReceived(announceResponse);
		
		verify(requestSchedulerMock, peerFoundListenerMock, futureMock);
		
		Assert.assertTrue(trackerSessionCreated);
		
		//Validate correctness of tracker session
		Assert.assertEquals(Tracker.Status.TRACKER_ERROR, trackerSession.getTrackerStatus());
		Assert.assertEquals(errorMessage, trackerSession.getTrackerMessage());
		Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, trackerSession.getMinInterval());
		Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, trackerSession.getInterval());
		
		final Entry<TrackerSession, ScheduledAnnouncement> onErrorRequest = 
				unitUnderTest.getScheduledRequest(torrent, udpTrackerSession.getTracker());
		Assert.assertNotNull(onErrorRequest);
		Assert.assertEquals(originalAnnouncement.getAnnounceParameters(), onErrorRequest.getValue().getAnnounceParameters());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testIssueAnnounce() {
		//Expect an announce and a scrape request
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);
		final boolean announceAllowed = unitUnderTest.issueAnnounce(httpTrackerSession, Tracker.Event.UPDATE);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(announceAllowed);
	}
	
	@Test
	public void testIssueNotAllowedAnnounce() {			
		replay(requestSchedulerMock, peerFoundListenerMock);
				
		httpTrackerSession.setMinInterval(Long.MAX_VALUE);
		httpTrackerSession.setLastAnnounceResponse(System.currentTimeMillis());
		httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);
		final boolean announceAllowed = unitUnderTest.issueAnnounce(httpTrackerSession, Tracker.Event.UPDATE);
		Assert.assertFalse(announceAllowed);
		
		verify(requestSchedulerMock, peerFoundListenerMock);		
	}
	
	@Test
	public void testIssueAnnounceInvalidTracker() {
		final TrackerSession invalidTrackerSession = new TrackerSession(torrent, new InvalidTracker("invalid_url"));
		
		invalidTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		final boolean announceAllowed = unitUnderTest.issueAnnounce(invalidTrackerSession, Tracker.Event.UPDATE);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertFalse(announceAllowed);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testIssueTorrentStoppedEvent() {
		//Expect four scrape requests (two when adding trackers and two when stopping torrent) and two HTTP tracker announces
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(6);
		
		//Expect two connection attempt scheduling for UDP tracker (when adding the torrent and when stopping it) 
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(0L), eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean addedHttpTracker = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		final boolean addedUdpTracker = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);
		
		Assert.assertTrue(addedHttpTracker);
		Assert.assertTrue(addedUdpTracker);
		
		unitUnderTest.getTrackerSession(torrent, httpTrackerSession.getTracker()).setLastAcknowledgedEvent(Tracker.Event.STARTED);
		unitUnderTest.getTrackerSession(torrent, udpTracker).setLastAcknowledgedEvent(Tracker.Event.STARTED);
		
		unitUnderTest.issueTorrentEvent(torrent, Tracker.Event.STOPPED);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testIssueTorrentStartedEvent() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect four scrape requests (two when adding trackers and two when starting torrent) and one HTTP tracker announce
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(5);
		
		//Expect a connection attempt scheduling for UDP tracker when starting the torrent
		expect(requestSchedulerMock.scheduleAtFixedRate(anyObject(Runnable.class), 
				eq(0L), eq(15000L), eq(TimeUnit.MILLISECONDS))).andReturn(createMock(ScheduledFuture.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean addedHttpTracker = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		final boolean addedUdpTracker = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);
		
		Assert.assertTrue(addedHttpTracker);
		Assert.assertTrue(addedUdpTracker);
		
		unitUnderTest.issueTorrentEvent(torrent, Tracker.Event.STARTED);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testIssueInvalidTorrentEvent() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect two scrape requests when adding the trackers
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean addedHttpTracker = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);
		final boolean addedUdpTracker = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);
		
		Assert.assertTrue(addedHttpTracker);
		Assert.assertTrue(addedUdpTracker);
		
		final TrackerSession storedHttpSession = unitUnderTest.getTrackerSession(torrent, httpTrackerSession.getTracker());
		storedHttpSession.setLastAcknowledgedEvent(Tracker.Event.STOPPED);
		storedHttpSession.setMinInterval(Long.MAX_VALUE);
		storedHttpSession.setLastAnnounceResponse(System.currentTimeMillis());
		
		final TrackerSession storedUdpSession = unitUnderTest.getTrackerSession(torrent, udpTracker);
		storedUdpSession.setLastAcknowledgedEvent(Tracker.Event.STOPPED);
		storedUdpSession.setMinInterval(Long.MAX_VALUE);
		storedUdpSession.setLastAnnounceResponse(System.currentTimeMillis());
		
		unitUnderTest.issueTorrentEvent(torrent, Tracker.Event.STOPPED);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
		
	@Test
	public void testAddInvalidTracker() {
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final Tracker invalidTracker = new InvalidTracker("invalid_url");
		final boolean trackerAdded = unitUnderTest.addTracker(invalidTracker.getUrl(), torrent);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
		
		Assert.assertTrue(trackerAdded);	
		
		final TrackerSession session = unitUnderTest.getTrackerSession(torrent, invalidTracker);
		Assert.assertNotNull(session);
		Assert.assertEquals(Tracker.Status.INVALID_URL, session.getTrackerStatus());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAddAlreadyAddedTracker() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect a scrape request after first tracker is added
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean firstAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);		
		Assert.assertTrue(firstAdded);
		
		final boolean secondAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);
		Assert.assertFalse(secondAdded);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveTracker() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect a scrape request after first tracker is added
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final Tracker tracker = httpTrackerSession.getTracker();
		
		final boolean added = unitUnderTest.addTracker(tracker.getUrl(), torrent);		
		Assert.assertTrue(added);
		
		final boolean removed = unitUnderTest.removeTracker(tracker.getUrl(), torrent);
		Assert.assertTrue(removed);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveNonExistingTracker() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect a scrape request after first tracker is added
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final Tracker tracker = httpTrackerSession.getTracker();
		
		final boolean added = unitUnderTest.addTracker(tracker.getUrl(), torrent);		
		Assert.assertTrue(added);
		
		final boolean removed = unitUnderTest.removeTracker(udpTracker.getUrl(), torrent);
		Assert.assertFalse(removed);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@Test
	public void testRemoveNonExistingTrackerSession() {
		final InfoHash nonExistingInfoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEFAAAA")); 
		final QueuedTorrent nonExistingTorrent = new QueuedTorrent(nonExistingInfoHash, 1, QueuedTorrent.State.ACTIVE);
		
		final boolean removed = unitUnderTest.removeTracker("http://non-existing:1234", nonExistingTorrent);
		Assert.assertFalse(removed);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveTorrentSession() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect two scrape requests (one for each of UDP and HTTP trackers respectively)
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class)).times(2);
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean httpTrackerAdded = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);		
		Assert.assertTrue(httpTrackerAdded);
		
		final boolean udpTrackerAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrent);		
		Assert.assertTrue(udpTrackerAdded);
		
		final int removedCount = unitUnderTest.removeTorrent(torrent);
		Assert.assertEquals(2, removedCount);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveNonExistingSession() {
		torrent.setState(QueuedTorrent.State.STOPPED);
		
		//Expect a scrape request after first tracker is added
		expect(requestSchedulerMock.submit(anyObject(Runnable.class))).andReturn(createMock(Future.class));
		
		replay(requestSchedulerMock, peerFoundListenerMock);
		
		final boolean added = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrent);		
		Assert.assertTrue(added);
		
		final InfoHash nonExistingInfoHash = new InfoHash(DatatypeConverter.parseHexBinary("BACDEF0123")); 
		final QueuedTorrent nonExistingTorrent = new QueuedTorrent(nonExistingInfoHash, 1, QueuedTorrent.State.STOPPED);
		
		final int removedCount = unitUnderTest.removeTorrent(nonExistingTorrent);
		Assert.assertEquals(0, removedCount);
		
		verify(requestSchedulerMock, peerFoundListenerMock);
	}*/
}
