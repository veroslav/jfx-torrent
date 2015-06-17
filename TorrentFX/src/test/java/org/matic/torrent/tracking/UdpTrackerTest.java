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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.codec.InfoHash;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpRequest;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.tracking.AnnounceRequest;
import org.matic.torrent.tracking.TrackableTorrent;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.UdpTracker;

public final class UdpTrackerTest {
	
	private final TrackableTorrent torrent = new TrackableTorrent(
			new InfoHash("12345678901234567890"));
	private final Tracker.Event trackerEvent = Tracker.Event.STARTED;
	private final long downloaded = 321;
	private final long uploaded = 123;	
	private final long left = 444;
	
	private final int transactionId = 6543543; 
	private final long lastResponse = 12214325L;
	private final long connectionId = 68566L;
	
	private final UdpConnectionManager connectionManagerMock = 
			EasyMock.createMock(UdpConnectionManager.class);
	private final String trackerUrl = "udp://localhost:44893";
	private UdpTracker unitUnderTest;
	
	@Before
	public final void setup() throws IOException, URISyntaxException {
		unitUnderTest = new UdpTracker(trackerUrl, connectionManagerMock);
		unitUnderTest.setConnectionId(connectionId);
		unitUnderTest.setLastResponse(lastResponse);
		
		torrent.setTransactionId(transactionId);
	}
	
	@Test
	public final void testBuildUdpRequest() throws IOException {
		final UdpRequest udpRequest = unitUnderTest.buildUdpRequest(buildAnnounceRequest());
		
		Assert.assertNotNull(udpRequest);
		
		final InetAddress actualAddress = InetAddress.getByName(udpRequest.getReceiverHost());
		final byte[] actualRequestData = udpRequest.getRequestData();
		final int actualPort = udpRequest.getReceiverPort();
		
		Assert.assertEquals(44893, actualPort);
		Assert.assertEquals("localhost", actualAddress.getHostName());
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(actualRequestData);
		final DataInputStream dis = new DataInputStream(bais);
		
		verifyRequestData(dis);
	}
	
	@Test
	public final void testAnnounceRequest() {
		EasyMock.expect(connectionManagerMock.send(EasyMock.anyObject(UdpRequest.class))).andReturn(true);
		EasyMock.replay(connectionManagerMock);
		
		unitUnderTest.announce(buildAnnounceRequest());
		
		EasyMock.verify(connectionManagerMock);
	}
	
	private void verifyRequestData(final DataInputStream inputStream) throws IOException {
		Assert.assertEquals(connectionId, inputStream.readLong());
		Assert.assertEquals(1, inputStream.readInt());
		Assert.assertEquals(transactionId, inputStream.readInt());
		
		final byte[] expectedInfoHash = torrent.getInfoHash().getBytes();
		final byte[] actualInfoHash = new byte[expectedInfoHash.length];
		inputStream.read(actualInfoHash);
		
		Assert.assertTrue(Arrays.equals(expectedInfoHash, actualInfoHash));
		
		final byte[] expectedPeerId = ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8.name());
		final byte[] actualPeerId = new byte[expectedPeerId.length];
		inputStream.read(actualPeerId);
		
		Assert.assertTrue(Arrays.equals(expectedPeerId, actualPeerId));
		Assert.assertEquals(downloaded, inputStream.readLong());
		Assert.assertEquals(left, inputStream.readLong());
		Assert.assertEquals(uploaded, inputStream.readLong());		
		
		Assert.assertEquals(2, inputStream.readInt());
		Assert.assertEquals(0, inputStream.readInt());
		Assert.assertEquals(0, inputStream.readInt());
		Assert.assertEquals(-1, inputStream.readInt());
		
		Assert.assertEquals(43893, inputStream.readShort() & 0xffff);
	}
	
	private AnnounceRequest buildAnnounceRequest() {				 	
		return new AnnounceRequest(torrent, trackerEvent, uploaded, downloaded, left);
	}
}