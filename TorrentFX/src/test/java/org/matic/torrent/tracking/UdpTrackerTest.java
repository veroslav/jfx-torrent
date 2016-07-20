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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpRequest;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.enums.QueueStatus;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class UdpTrackerTest {

    private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("ABCDEF0123"));
    private final QueuedTorrent torrent = buildTorrent(infoHash.getBytes(), QueueStatus.ACTIVE);
    private final TorrentView torrentView = new TorrentView(torrent);

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

        trackerSession = new TrackerSession(torrentView, unitUnderTest);
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

        Assert.assertEquals(ClientProperties.TCP_PORT, inputStream.readShort() & 0xffff);
    }

    private QueuedTorrent buildTorrent(final byte[] infoHash, final QueueStatus targetQueue) {
        final BinaryEncodedDictionary infoDict = new BinaryEncodedDictionary();
        infoDict.put(BinaryEncodingKeys.KEY_LENGTH, new BinaryEncodedInteger(42));
        infoDict.put(BinaryEncodingKeys.KEY_PIECE_LENGTH, new BinaryEncodedInteger(19));

        final BinaryEncodedDictionary metaDataDict = new BinaryEncodedDictionary();
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO, infoDict);
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO_HASH, new BinaryEncodedString(infoHash));

        final QueuedTorrentMetaData metaData = new QueuedTorrentMetaData(metaDataDict);

        final BinaryEncodedDictionary progressDict = new BinaryEncodedDictionary();
        progressDict.put(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME, new BinaryEncodedString(targetQueue.name()));

        final QueuedTorrentProgress progress = new QueuedTorrentProgress(progressDict);

        return new QueuedTorrent(metaData, progress);
    }
}