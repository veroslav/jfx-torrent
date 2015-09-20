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


public final class UdpTrackerTest {
	
	/*private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEF0123"));
	private final QueuedTorrent torrent = new QueuedTorrent(infoHash, 1, QueuedTorrent.State.ACTIVE);	
	private final Tracker.Event trackerEvent = Tracker.Event.STARTED;
	private final long downloaded = 321;
	private final long uploaded = 123;	
	private final long left = 444;
	
	private final int sessionId = 6543543; 
	private final long lastResponse = 12214325L;
	private final long connectionId = 68566L;
	
	private UdpConnectionManager connectionManagerMock;
	private TrackerSession trackerSession;
	private UdpTracker unitUnderTest;	
	private URI trackerUrl;	
	
	@Before
	public final void setup() throws IOException, URISyntaxException {
		connectionManagerMock = EasyMock.createMock(UdpConnectionManager.class);		
		trackerUrl = new URI("udp://localhost:44893");
		
		unitUnderTest = new UdpTracker(trackerUrl, connectionManagerMock);		
		unitUnderTest.setLastResponse(lastResponse);
		unitUnderTest.setId(connectionId);
		
		trackerSession = new TrackerSession(torrent, unitUnderTest);
	}
	
	@Test
	public final void testBuildUdpRequest() throws IOException {
		final UdpRequest udpRequest = unitUnderTest.buildAnnounceRequest(
				new AnnounceParameters(trackerEvent, uploaded, downloaded, left), infoHash, 42, sessionId);
		
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
		
		unitUnderTest.announce(new AnnounceParameters(trackerEvent, uploaded, downloaded, left), trackerSession);
		
		EasyMock.verify(connectionManagerMock);
	}
	
	private void verifyRequestData(final DataInputStream inputStream) throws IOException {
		Assert.assertEquals(connectionId, inputStream.readLong());
		Assert.assertEquals(1, inputStream.readInt());
		Assert.assertEquals(sessionId, inputStream.readInt());
		
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
		Assert.assertEquals(42, inputStream.readInt());
		Assert.assertEquals(200, inputStream.readInt());
		
		Assert.assertEquals(UdpConnectionManager.UDP_TRACKER_PORT, inputStream.readShort() & 0xffff);
	}*/	
}