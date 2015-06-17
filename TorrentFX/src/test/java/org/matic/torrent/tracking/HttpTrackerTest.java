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
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeyNames;
import org.matic.torrent.codec.InfoHash;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.tracking.AnnounceRequest;
import org.matic.torrent.tracking.HttpTracker;
import org.matic.torrent.tracking.TrackableTorrent;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerResponse;
import org.matic.torrent.tracking.TrackerResponse.Type;
import org.matic.torrent.utils.UnitConverter;

public final class HttpTrackerTest {
	
	private final TrackableTorrent torrent = new TrackableTorrent(
			new InfoHash("12345678901234567890"));	
	
	//Mandatory response fields
	private final long incomplete = 77;
	private final long interval = 30L; //30 seconds
	private final long complete = 12;
	
	//Optional response fields
	private final String warningMessage = "A warning message";
	private final String trackerId = "trackerId";
	private final Long minInterval = 678L;
	
	private final HttpTracker unitUnderTest = new HttpTracker("http://example.com/announce", null);

	@Test
	public final void testScrapeSupported() {
		HttpTracker unitUnderTest = new HttpTracker("http://example.com/announce", null);
		Assert.assertTrue(unitUnderTest.isScrapeSupported());
		Assert.assertEquals("http://example.com/scrape", unitUnderTest.getScrapeUrl());
		
		unitUnderTest = new HttpTracker("http://example.com/x/announce", null);
		Assert.assertTrue(unitUnderTest.isScrapeSupported());
		Assert.assertEquals("http://example.com/x/scrape", unitUnderTest.getScrapeUrl());
		
		unitUnderTest = new HttpTracker("http://example.com/announce.php", null);
		Assert.assertTrue(unitUnderTest.isScrapeSupported());
		Assert.assertEquals("http://example.com/scrape.php", unitUnderTest.getScrapeUrl());
		
		unitUnderTest = new HttpTracker("http://example.com/announce?x2%0644", null);
		Assert.assertTrue(unitUnderTest.isScrapeSupported());
		Assert.assertEquals("http://example.com/scrape?x2%0644", unitUnderTest.getScrapeUrl());
	}
	
	@Test
	public final void testScrapeUnsupported() {
		HttpTracker unitUnderTest = new HttpTracker("http://example.com/a", null);
		Assert.assertFalse(unitUnderTest.isScrapeSupported());
		
		unitUnderTest = new HttpTracker("http://example.com/announce?x=2/4", null);
		Assert.assertFalse(unitUnderTest.isScrapeSupported());
		
		unitUnderTest = new HttpTracker("http://example.com/x%064announce", null);
		Assert.assertFalse(unitUnderTest.isScrapeSupported());
	}
	
	@Test
	public final void testBuildRequestUrl() throws Exception {
		final AnnounceRequest request = new AnnounceRequest(
				torrent, Tracker.Event.STARTED, 123, 321, 444);
		
		final StringBuilder urlBuilder = new StringBuilder("http://example.com/announce?info_hash=");
		urlBuilder.append("12345678901234567890&peer_id=");
		urlBuilder.append(ClientProperties.PEER_ID);
		urlBuilder.append("&uploaded=123&downloaded=321&left=444&port=");
		urlBuilder.append(ClientProperties.TCP_PORT);
		urlBuilder.append("&compact=1?event=started");
		
		final String expectedUrl = urlBuilder.toString(); 
		final String actualUrl = unitUnderTest.buildRequestUrl(request);
		Assert.assertEquals(expectedUrl, actualUrl);
	}
	
	@Test
	public final void testExceptionThrownOnResponse() throws IOException {
		final String errorMessage = "Tracker threw an exception";
		final InputStream errorStream = new ByteArrayInputStream(
				errorMessage.getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		
		final TrackerResponse actualResponse = unitUnderTest.buildErrorResponse(errorStream);
		
		Assert.assertEquals(TrackerResponse.Type.TRACKER_ERROR, actualResponse.getType());
		Assert.assertEquals(errorMessage, actualResponse.getMessage());
	}
	
	@Test
	public final void testErrorTrackerResponse() {
		final String failureReason = "Tracker failed to respond";
		final BinaryEncodedDictionary responseMap = new BinaryEncodedDictionary();
		responseMap.put(BinaryEncodingKeyNames.KEY_FAILURE_REASON, new BinaryEncodedString(
				failureReason.getBytes(ClientProperties.STRING_ENCODING_CHARSET)));
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());
		
		Assert.assertEquals(TrackerResponse.Type.TRACKER_ERROR, actualResponse.getType());
		Assert.assertEquals(failureReason, actualResponse.getMessage());
	}
	
	@Test
	public final void testWarningTrackerResponse() {	
		final Set<PwpPeer> peers = createPeers(0);
		final TrackerResponse expectedResponse = new TrackerResponse(TrackerResponse.Type.WARNING, 
				warningMessage, interval, minInterval, trackerId,
				complete, incomplete, peers);
		
		final BinaryEncodedDictionary responseMap = buildNormalTrackerResponse(TrackerResponse.Type.WARNING,
				warningMessage, interval, minInterval, trackerId, complete, incomplete, peers, true);
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());
		
		Assert.assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public final void testNormalTrackerResponseAllOptionalFieldsSet() {
		final Set<PwpPeer> peers = createPeers(0);
		final TrackerResponse expectedResponse = new TrackerResponse(TrackerResponse.Type.NORMAL, 
				null, interval, minInterval, trackerId,
				complete, incomplete, peers);
		
		final BinaryEncodedDictionary responseMap = buildNormalTrackerResponse(TrackerResponse.Type.NORMAL,
				null, interval, minInterval, trackerId, complete, incomplete, peers, true);
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());
		
		Assert.assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public final void testNormalTrackerResponseNoOptionalFieldsSet() {
		final Set<PwpPeer> peers = createPeers(0);
		final TrackerResponse expectedResponse = new TrackerResponse(TrackerResponse.Type.NORMAL, 
				null, interval, null, null, complete, incomplete, peers);
		
		final BinaryEncodedDictionary responseMap = buildNormalTrackerResponse(TrackerResponse.Type.NORMAL,
				null, interval, null, null, complete, incomplete, peers, true);
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());
		
		Assert.assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public final void testMissingMandatoryResponseValue() {
		final TrackerResponse expectedResponse = new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, 
				"Missing mandatory response value");
		final Set<PwpPeer> peers = createPeers(1);
		final BinaryEncodedDictionary responseMap = buildNormalTrackerResponse(TrackerResponse.Type.NORMAL,
				null, null, null, null, complete, incomplete, peers, true);
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());
		
		Assert.assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public final void testParsePeersFromList() {
		testParsePeers(true, 3);
	}
	
	@Test
	public final void testParsePeersFromString() {
		testParsePeers(false, 3);
	}
	
	private void testParsePeers(final boolean peersInAList, final int peerCount) {
		final Set<PwpPeer> expectedPeers = createPeers(peerCount);
		final TrackerResponse expectedResponse = new TrackerResponse(TrackerResponse.Type.NORMAL, 
				null, interval, minInterval, trackerId,
				complete, incomplete, expectedPeers);
		
		final BinaryEncodedDictionary responseMap = buildNormalTrackerResponse(TrackerResponse.Type.NORMAL,
				null, interval, minInterval, trackerId, complete, incomplete, expectedPeers, peersInAList);
		
		final TrackerResponse actualResponse = unitUnderTest.buildResponse(responseMap, torrent.getInfoHash());		
		Assert.assertEquals(expectedResponse, actualResponse);
		
		final Set<PwpPeer> actualPeers = actualResponse.getPeers();
		Assert.assertEquals(peerCount, actualPeers.size());
		Assert.assertEquals(expectedPeers, actualPeers);
	}
	
	private Set<PwpPeer> createPeers(final int peerCount) {
		final Set<PwpPeer> peers = new HashSet<>();
		for(int i = 0; i < peerCount; ++i) {			
			peers.add(new PwpPeer("127.0.0." + i, i, torrent.getInfoHash()));
		}
		
		return peers;
	}
	
	private BinaryEncodedString createPeerString(final Set<PwpPeer> peers) {								
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(6 * peers.size());		
		peers.stream().forEach(p -> {
			try {
				final byte[] ipBytes = InetAddress.getByName(p.getPeerIp()).getAddress();
				baos.write(ipBytes);
				
				final byte[] peerPort = UnitConverter.getBytes((short)p.getPeerPort());
				baos.write(peerPort);
			}
			catch(final Exception e) {
				//Can't do anything here, unit test will eventually fail
				return;
			}
		});
		
		return new BinaryEncodedString(baos.toByteArray());
	}
	
	private BinaryEncodedList createPeerList(final Set<PwpPeer> peers) {
		final BinaryEncodedList peerList = new BinaryEncodedList();
		peers.stream().forEach(p -> {
			final BinaryEncodedDictionary peerMap = new BinaryEncodedDictionary();
			peerMap.put(BinaryEncodingKeyNames.KEY_IP, new BinaryEncodedString(
					p.getPeerIp().getBytes(ClientProperties.STRING_ENCODING_CHARSET)));
			peerMap.put(BinaryEncodingKeyNames.KEY_PORT, new BinaryEncodedInteger(p.getPeerPort()));
			peerList.add(peerMap);
		});
		return peerList;
	}
	
	private BinaryEncodedDictionary buildNormalTrackerResponse(final Type type, 
			final String warningMessage, final Long interval,
			final Long minInterval, final String trackerId, 
			final Long complete, final Long incomplete, final Set<PwpPeer> peers, final boolean peersInAList) {		
		final BinaryEncodedDictionary response = new BinaryEncodedDictionary();
		response.put(BinaryEncodingKeyNames.KEY_INTERVAL, interval != null? new BinaryEncodedInteger(interval) : null);
		response.put(BinaryEncodingKeyNames.KEY_COMPLETE, complete != null? new BinaryEncodedInteger(complete) : null);
		response.put(BinaryEncodingKeyNames.KEY_INCOMPLETE, incomplete != null? new BinaryEncodedInteger(incomplete) : null);
		response.put(BinaryEncodingKeyNames.KEY_TRACKER_ID, trackerId != null? new BinaryEncodedString(
				trackerId.getBytes(ClientProperties.STRING_ENCODING_CHARSET)) : null);
		response.put(BinaryEncodingKeyNames.KEY_WARNING_MESSAGE, warningMessage != null? new BinaryEncodedString(
				warningMessage.getBytes(ClientProperties.STRING_ENCODING_CHARSET)) : null);
		response.put(BinaryEncodingKeyNames.KEY_MIN_INTERVAL, minInterval != null? 
				new BinaryEncodedInteger(minInterval) : null);
		response.put(BinaryEncodingKeyNames.KEY_PEERS, peersInAList? createPeerList(peers) : createPeerString(peers));
		
		return response;
	}
}