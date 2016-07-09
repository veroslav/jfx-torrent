/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpTrackerResponse;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.tracking.listeners.PeerFoundListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TrackerManagerTest {

    private static final String HTTP_TRACKER_URL = "http://localhost:12345/announce";
    private static final String UDP_TRACKER_URL = "udp://localhost:12345";

    //private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEF0123"));

    private UdpConnectionManager udpConnectionManagerMock = EasyMock.createMock(UdpConnectionManager.class);

    private ScheduledExecutorService requestSchedulerMock = EasyMock.createMock(ScheduledExecutorService.class);
    private PeerFoundListener peerFoundListenerMock = EasyMock.createMock(PeerFoundListener.class);

    private TorrentView torrentView;
    private QueuedTorrent torrent;

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
        udpTracker = new UdpTracker(new URI(UDP_TRACKER_URL), udpConnectionManagerMock);

        torrent = buildTorrent("ABCDEF0123", TorrentStatus.ACTIVE);
        torrentView = new TorrentView(torrent);

        udpTrackerSession = new TrackerSession(torrentView, udpTracker);
        httpTrackerSession = new TrackerSession(torrentView, new HttpTracker(HTTP_TRACKER_URL));
    }

    @After
    public void cleanup() {
        unitUnderTest.removePeerListener(peerFoundListenerMock);
        EasyMock.reset(udpConnectionManagerMock, peerFoundListenerMock, requestSchedulerMock);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testOnAnnounceResponseAndOnePeer() {
        final Set<PwpPeer> peers = new HashSet<>(Arrays.asList(new PwpPeer("128.0.0.1", 42, torrent.getInfoHash())));
        final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
                trackerMessage, interval, minInterval, trackerId, complete, incomplete, peers);

        //Expect an HTTP tracker announcement after a response to an earlier announcement request
        EasyMock.expect(requestSchedulerMock.schedule(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(interval), EasyMock.eq(TimeUnit.MILLISECONDS))).andReturn(
                EasyMock.createMock(ScheduledFuture.class));

        //Expect three HTTP tracker submits (initial announcement + two scrape requests)
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class))).andReturn(
                EasyMock.createMock(Future.class)).times(3);

        //Expect notification when a new peer is obtained
        peerFoundListenerMock.onPeersFound(peers);
        EasyMock.expectLastCall();

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView trackerView = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrentView);

        //Validate correctness of the connection request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> initialRequest =
                unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker());
        Assert.assertNotNull(initialRequest);
        final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();

        final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
        Assert.assertNotNull(initialRequestParameters);
        Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());

        //We need to set correct last tracker event before receiving the tracker's response
        httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);
        unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(HTTP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Validate correctness of tracker session
        Assert.assertEquals(Tracker.Status.WORKING, httpTrackerSession.getTrackerStatus());
        Assert.assertEquals(trackerMessage, httpTrackerSession.getTrackerMessage());
        Assert.assertEquals(minInterval, httpTrackerSession.getMinInterval());
        Assert.assertEquals(interval, httpTrackerSession.getInterval());

        //Validate correctness of announce request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> scheduledRequest =
                unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker());
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
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(0L), EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS)))
                .andReturn(EasyMock.createMock(ScheduledFuture.class));

        //Expect an UDP tracker announcement after a response to an earlier announcement request
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(interval), EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS)))
                .andReturn(EasyMock.createMock(ScheduledFuture.class));

        //Expect two UDP tracker scrape requests (for each ONE connection and ONE announcement request)
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView trackerView = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrentView);

        //Validate correctness of the connection request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> initialRequest =
                unitUnderTest.getScheduledRequest(torrentView, udpTrackerSession.getTracker());
        Assert.assertNotNull(initialRequest);
        final ScheduledAnnouncement connectionRequest = initialRequest.getValue();

        final AnnounceParameters connectionRequestParameters = connectionRequest.getAnnounceParameters();
        Assert.assertNotNull(connectionRequestParameters);
        Assert.assertEquals(Tracker.Event.STARTED, connectionRequestParameters.getTrackerEvent());

        //We need to set correct last tracker event before receiving the tracker's response
        udpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);
        unitUnderTest.onAnnounceResponseReceived(response, udpTrackerSession);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(UDP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Validate correctness of tracker session
        Assert.assertEquals(Tracker.Status.WORKING, udpTrackerSession.getTrackerStatus());
        Assert.assertEquals(null, udpTrackerSession.getTrackerMessage());
        Assert.assertEquals(minInterval, udpTrackerSession.getMinInterval());
        Assert.assertEquals(interval, udpTrackerSession.getInterval());

        //Validate correctness of announce request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> normalRequest =
                unitUnderTest.getScheduledRequest(torrentView, udpTrackerSession.getTracker());
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

        EasyMock.replay(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);

        unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);

        EasyMock.verify(peerFoundListenerMock, requestSchedulerMock, udpConnectionManagerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnAnnounceResponseAndSessionStopped() {
        final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.OK,
                trackerMessage, interval, minInterval, trackerId, complete, incomplete, Collections.emptySet());

        //Expect an initial announce and scrape request
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView trackerView = unitUnderTest.addTracker(
                httpTrackerSession.getTracker().getUrl(), torrentView);

        //Validate correctness of the connection request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> initialRequest =
                unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker());
        Assert.assertNotNull(initialRequest);
        final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();

        final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
        Assert.assertNotNull(initialRequestParameters);
        Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());

        //We need to set correct last tracker event before receiving the tracker's response
        httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.STARTED);
        torrent.setStatus(TorrentStatus.STOPPED);
        unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(HTTP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Check that any scheduled requests for the session have been cancelled and removed
        Assert.assertNull(unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker()));

        //Validate correctness of tracker session (should be reset as the session has been stopped)
        Assert.assertEquals(Tracker.Event.STOPPED, httpTrackerSession.getLastAcknowledgedEvent());
        Assert.assertEquals(Tracker.Status.UNKNOWN, httpTrackerSession.getTrackerStatus());
        Assert.assertNull(httpTrackerSession.getTrackerMessage());
        Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, httpTrackerSession.getMinInterval());
        Assert.assertEquals(0, httpTrackerSession.getInterval());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnAnnounceResponseAndIsError() {
        final AnnounceResponse response = new AnnounceResponse(TrackerResponse.Type.READ_WRITE_ERROR,
                trackerMessage, interval, minInterval, trackerId, complete, incomplete, Collections.emptySet());

        //Expect an initial announce and two scrape requests
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(3);

        //Expect an HTTP announcement scheduled after a tracker error has occurred
        EasyMock.expect(requestSchedulerMock.schedule(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR),
                EasyMock.eq(TimeUnit.MILLISECONDS))).andReturn(EasyMock.createMock(ScheduledFuture.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView trackerView = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrentView);

        //Validate correctness of the connection request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> initialRequest =
                unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker());
        Assert.assertNotNull(initialRequest);
        final ScheduledAnnouncement initialAnnounceRequest = initialRequest.getValue();

        final AnnounceParameters initialRequestParameters = initialAnnounceRequest.getAnnounceParameters();
        Assert.assertNotNull(initialRequestParameters);
        Assert.assertEquals(Tracker.Event.STARTED, initialRequestParameters.getTrackerEvent());

        unitUnderTest.onAnnounceResponseReceived(response, httpTrackerSession);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(HTTP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Validate correctness of tracker session
        Assert.assertEquals(Tracker.Status.TRACKER_ERROR, httpTrackerSession.getTrackerStatus());
        Assert.assertEquals(trackerMessage, httpTrackerSession.getTrackerMessage());
        Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, httpTrackerSession.getMinInterval());
        Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, httpTrackerSession.getInterval());

        //Validate correctness of announce request parameters
        final Entry<TrackerSession, ScheduledAnnouncement> regularRequest =
                unitUnderTest.getScheduledRequest(torrentView, httpTrackerSession.getTracker());
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
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView trackerView = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrentView);

        unitUnderTest.onScrapeResponseReceived(httpTrackerSession.getTracker(), scrapeResponse);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(HTTP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

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
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        torrent.setStatus(TorrentStatus.STOPPED);
        final TrackerView trackerView = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrentView);

        final TrackerSession scrapedSession = unitUnderTest.getScheduledUdpScrapeEntry(torrentView, udpTracker);
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

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(UDP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

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
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        torrent.setStatus(TorrentStatus.STOPPED);
        final TrackerView trackerView = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrentView);

        final TrackerSession scrapedSession = unitUnderTest.getScheduledUdpScrapeEntry(torrentView, udpTracker);
        Assert.assertNotNull(scrapedSession);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream daos = new DataOutputStream(baos);
        daos.writeInt(UdpTracker.ACTION_ERROR);
        daos.writeInt(scrapedSession.getTracker().getScrapeTransactionId());
        daos.write(errorMessage.getBytes(ClientProperties.STRING_ENCODING_CHARSET));

        final UdpTrackerResponse scrapeResponse = new UdpTrackerResponse(baos.toByteArray(), UdpTracker.ACTION_ERROR, null);

        unitUnderTest.onUdpTrackerResponseReceived(scrapeResponse);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals(UDP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Validate correctness of tracker session
        Assert.assertEquals(Tracker.Status.TRACKER_ERROR, scrapedSession.getTrackerStatus());
        Assert.assertEquals(errorMessage, scrapedSession.getTrackerMessage());
        Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, scrapedSession.getMinInterval());
        Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, scrapedSession.getInterval());
        Assert.assertNull(unitUnderTest.getScheduledUdpScrapeEntry(torrentView, udpTracker));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testOnUdpTrackerAnnounceError() throws IOException {
        final String errorMessage = "Announce error";

        //Expect an UDP connection request
        final ScheduledFuture futureMock = EasyMock.createMock(ScheduledFuture.class);
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(0L), EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS))).andReturn(futureMock);

        //Expect a cancellation of original announce request when re-scheduling on tracker error
        EasyMock.expect(futureMock.cancel(false)).andReturn(true);

        //Expect three UDP tracker scrape requests
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(3);

        //Expect an UDP announcement scheduled after a tracker error has occurred
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(
                EasyMock.anyObject(Runnable.class), EasyMock.eq(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR),
                EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS))).andReturn(EasyMock.createMock(ScheduledFuture.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock, futureMock);

        final TrackerView trackerView = unitUnderTest.addTracker(udpTrackerSession.getTracker().getUrl(), torrentView);

        final Entry<TrackerSession, ScheduledAnnouncement> announceRequest =
                unitUnderTest.getScheduledRequest(torrentView, udpTrackerSession.getTracker());
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

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock, futureMock);

        Assert.assertEquals(UDP_TRACKER_URL, trackerView.getName());
        Assert.assertEquals(torrentView, trackerView.getTorrentView());

        //Validate correctness of tracker session
        Assert.assertEquals(Tracker.Status.TRACKER_ERROR, trackerSession.getTrackerStatus());
        Assert.assertEquals(errorMessage, trackerSession.getTrackerMessage());
        Assert.assertEquals(Tracker.MIN_INTERVAL_DEFAULT_VALUE, trackerSession.getMinInterval());
        Assert.assertEquals(TrackerManager.REQUEST_DELAY_ON_TRACKER_ERROR, trackerSession.getInterval());

        final Entry<TrackerSession, ScheduledAnnouncement> onErrorRequest =
                unitUnderTest.getScheduledRequest(torrentView, udpTrackerSession.getTracker());
        Assert.assertNotNull(onErrorRequest);
        Assert.assertEquals(originalAnnouncement.getAnnounceParameters(), onErrorRequest.getValue().getAnnounceParameters());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssueAnnounce() {
        //Expect an announce and a scrape request
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);
        final boolean announceAllowed = unitUnderTest.issueAnnounce(httpTrackerSession, Tracker.Event.UPDATE);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertTrue(announceAllowed);
    }

    @Test
    public void testIssueNotAllowedAnnounce() {
        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        httpTrackerSession.setMinInterval(Long.MAX_VALUE);
        httpTrackerSession.setLastAnnounceResponse(System.currentTimeMillis());
        httpTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);
        final boolean announceAllowed = unitUnderTest.issueAnnounce(httpTrackerSession, Tracker.Event.UPDATE);
        Assert.assertFalse(announceAllowed);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @Test
    public void testIssueAnnounceInvalidTracker() {
        final TrackerSession invalidTrackerSession = new TrackerSession(torrentView, new InvalidTracker("invalid_url"));

        invalidTrackerSession.setLastAcknowledgedEvent(Tracker.Event.UPDATE);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);
        final boolean announceAllowed = unitUnderTest.issueAnnounce(invalidTrackerSession, Tracker.Event.UPDATE);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertFalse(announceAllowed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssueTorrentStoppedEvent() {
        //Expect four scrape requests (two when adding trackers and two when stopping torrent) and two HTTP tracker announces
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(6);

        //Expect two connection attempt scheduling for UDP tracker (when adding the torrent and when stopping it)
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(0L), EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS)))
                .andReturn(EasyMock.createMock(ScheduledFuture.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView addedHttpTrackerView = unitUnderTest.addTracker(
                httpTrackerSession.getTracker().getUrl(), torrentView);
        final TrackerView addedUdpTrackerView = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);

        Assert.assertEquals(UDP_TRACKER_URL, addedUdpTrackerView.getName());
        Assert.assertEquals(torrentView, addedUdpTrackerView.getTorrentView());

        Assert.assertEquals(HTTP_TRACKER_URL, addedHttpTrackerView.getName());
        Assert.assertEquals(torrentView, addedHttpTrackerView.getTorrentView());

        unitUnderTest.getTrackerSession(torrentView,
                httpTrackerSession.getTracker()).setLastAcknowledgedEvent(Tracker.Event.STARTED);
        unitUnderTest.getTrackerSession(torrentView, udpTracker).setLastAcknowledgedEvent(Tracker.Event.STARTED);

        unitUnderTest.issueTorrentEvent(torrentView, Tracker.Event.STOPPED);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssueTorrentStartedEvent() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect four scrape requests (two when adding trackers and two when starting torrent) and one HTTP tracker announce
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(5);

        //Expect a connection attempt scheduling for UDP tracker when starting the torrent
        EasyMock.expect(requestSchedulerMock.scheduleAtFixedRate(EasyMock.anyObject(Runnable.class),
                EasyMock.eq(0L), EasyMock.eq(15000L), EasyMock.eq(TimeUnit.MILLISECONDS)))
                .andReturn(EasyMock.createMock(ScheduledFuture.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView addedHttpTrackerView = unitUnderTest.addTracker(
                httpTrackerSession.getTracker().getUrl(), torrentView);
        final TrackerView addedUdpTrackerView = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);

        Assert.assertEquals(UDP_TRACKER_URL, addedUdpTrackerView.getName());
        Assert.assertEquals(torrentView, addedUdpTrackerView.getTorrentView());

        Assert.assertEquals(HTTP_TRACKER_URL, addedHttpTrackerView.getName());
        Assert.assertEquals(torrentView, addedHttpTrackerView.getTorrentView());

        unitUnderTest.issueTorrentEvent(torrentView, Tracker.Event.STARTED);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssueInvalidTorrentEvent() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect two scrape requests when adding the trackers
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView addedHttpTrackerView = unitUnderTest.addTracker(
                httpTrackerSession.getTracker().getUrl(), torrentView);
        final TrackerView addedUdpTrackerView = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);

        Assert.assertEquals(UDP_TRACKER_URL, addedUdpTrackerView.getName());
        Assert.assertEquals(torrentView, addedUdpTrackerView.getTorrentView());

        Assert.assertEquals(HTTP_TRACKER_URL, addedHttpTrackerView.getName());
        Assert.assertEquals(torrentView, addedHttpTrackerView.getTorrentView());

        final TrackerSession storedHttpSession = unitUnderTest.getTrackerSession(
                torrentView, httpTrackerSession.getTracker());
        storedHttpSession.setLastAcknowledgedEvent(Tracker.Event.STOPPED);
        storedHttpSession.setMinInterval(Long.MAX_VALUE);
        storedHttpSession.setLastAnnounceResponse(System.currentTimeMillis());

        final TrackerSession storedUdpSession = unitUnderTest.getTrackerSession(torrentView, udpTracker);
        storedUdpSession.setLastAcknowledgedEvent(Tracker.Event.STOPPED);
        storedUdpSession.setMinInterval(Long.MAX_VALUE);
        storedUdpSession.setLastAnnounceResponse(System.currentTimeMillis());

        unitUnderTest.issueTorrentEvent(torrentView, Tracker.Event.STOPPED);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @Test
    public void testAddInvalidTracker() {
        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final String invalidUrl = "invalid_url";
        final Tracker invalidTracker = new InvalidTracker(invalidUrl);
        final TrackerView invalidTrackerView = unitUnderTest.addTracker(invalidTracker.getUrl(), torrentView);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);

        Assert.assertEquals("invalid_url", invalidTrackerView.getName());
        Assert.assertEquals(torrentView, invalidTrackerView.getTorrentView());

        final TrackerSession session = unitUnderTest.getTrackerSession(torrentView, invalidTracker);
        Assert.assertNotNull(session);
        Assert.assertEquals(Tracker.Status.INVALID_URL, session.getTrackerStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAlreadyAddedTracker() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect a scrape request after first tracker is added
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView firstAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);
        Assert.assertEquals(udpTracker.getUrl(), firstAdded.getName());
        Assert.assertEquals(torrentView, firstAdded.getTorrentView());

        final TrackerView secondAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);
        Assert.assertEquals(udpTracker.getUrl(), secondAdded.getName());
        Assert.assertEquals(torrentView, secondAdded.getTorrentView());

        Assert.assertEquals(firstAdded.getTorrentView(), secondAdded.getTorrentView());

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveTracker() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect a scrape request after first tracker is added
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final Tracker tracker = httpTrackerSession.getTracker();

        final TrackerView added = unitUnderTest.addTracker(tracker.getUrl(), torrentView);
        Assert.assertEquals(HTTP_TRACKER_URL, added.getName());
        Assert.assertEquals(torrentView, added.getTorrentView());

        final boolean removed = unitUnderTest.removeTracker(tracker.getUrl(), torrentView);
        Assert.assertTrue(removed);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveNonExistingTracker() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect a scrape request after first tracker is added
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final Tracker tracker = httpTrackerSession.getTracker();

        final TrackerView added = unitUnderTest.addTracker(tracker.getUrl(), torrentView);
        Assert.assertEquals(HTTP_TRACKER_URL, added.getName());
        Assert.assertEquals(torrentView, added.getTorrentView());

        final boolean removed = unitUnderTest.removeTracker(udpTracker.getUrl(), torrentView);
        Assert.assertFalse(removed);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @Test
    public void testRemoveNonExistingTrackerSession() {
        final QueuedTorrent nonExistingTorrent = buildTorrent("ABCDEFAAAA", TorrentStatus.ACTIVE);

        final boolean removed = unitUnderTest.removeTracker(
                "http://non-existing:1234", new TorrentView(nonExistingTorrent));
        Assert.assertFalse(removed);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveTorrentSession() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect two scrape requests (one for each of UDP and HTTP trackers respectively)
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class)).times(2);

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView httpTrackerAdded = unitUnderTest.addTracker(
                httpTrackerSession.getTracker().getUrl(), torrentView);
        Assert.assertEquals(HTTP_TRACKER_URL, httpTrackerAdded.getName());
        Assert.assertEquals(torrentView, httpTrackerAdded.getTorrentView());

        final TrackerView udpTrackerAdded = unitUnderTest.addTracker(udpTracker.getUrl(), torrentView);
        Assert.assertEquals(UDP_TRACKER_URL, udpTrackerAdded.getName());
        Assert.assertEquals(torrentView, udpTrackerAdded.getTorrentView());

        final int removedCount = unitUnderTest.removeTorrent(torrentView);
        Assert.assertEquals(2, removedCount);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveNonExistingSession() {
        torrent.setStatus(TorrentStatus.STOPPED);

        //Expect a scrape request after first tracker is added
        EasyMock.expect(requestSchedulerMock.submit(EasyMock.anyObject(Runnable.class)))
                .andReturn(EasyMock.createMock(Future.class));

        EasyMock.replay(requestSchedulerMock, peerFoundListenerMock);

        final TrackerView added = unitUnderTest.addTracker(httpTrackerSession.getTracker().getUrl(), torrentView);
        Assert.assertEquals(HTTP_TRACKER_URL, added.getName());
        Assert.assertEquals(torrentView, added.getTorrentView());

        final QueuedTorrent nonExistingTorrent = buildTorrent("BACDEF0123", TorrentStatus.STOPPED);

        final int removedCount = unitUnderTest.removeTorrent(new TorrentView(nonExistingTorrent));
        Assert.assertEquals(0, removedCount);

        EasyMock.verify(requestSchedulerMock, peerFoundListenerMock);
    }

    private QueuedTorrent buildTorrent(final String infoHash, final TorrentStatus initialStatus) {
        final BinaryEncodedDictionary infoDict = new BinaryEncodedDictionary();
        infoDict.put(BinaryEncodingKeys.KEY_LENGTH, new BinaryEncodedInteger(42));
        infoDict.put(BinaryEncodingKeys.KEY_PIECE_LENGTH, new BinaryEncodedInteger(19));

        final BinaryEncodedDictionary metaDataDict = new BinaryEncodedDictionary();
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO, infoDict);
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO_HASH, new BinaryEncodedString(infoHash));


        final QueuedTorrentMetaData metaData = new QueuedTorrentMetaData(metaDataDict);

        final BinaryEncodedDictionary progressDict = new BinaryEncodedDictionary();
        progressDict.put(BinaryEncodingKeys.STATE_KEY_TORRENT_STATUS, new BinaryEncodedString(initialStatus.name()));

        final QueuedTorrentProgress progress = new QueuedTorrentProgress(progressDict);

        return new QueuedTorrent(metaData, progress);
    }
}
