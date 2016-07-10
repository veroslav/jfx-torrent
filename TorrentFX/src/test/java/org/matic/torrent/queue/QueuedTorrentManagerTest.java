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
package org.matic.torrent.queue;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.model.TrackableView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataPersistenceSupport;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.TrackerSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QueuedTorrentManagerTest {

    private final DataPersistenceSupport dataPersistenceSupportMock = EasyMock.createMock(DataPersistenceSupport.class);
    private final TrackerManager trackerManagerMock = EasyMock.createMock(TrackerManager.class);

    @After
    public void cleanup() {
        EasyMock.reset(dataPersistenceSupportMock, trackerManagerMock);
    }

    @Test
    public void testAddSingleTorrent() {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(infoHash)).andReturn(true);
        EasyMock.replay(dataPersistenceSupportMock, trackerManagerMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template));

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);

        Assert.assertEquals(1, torrentViews.size());

        final TorrentView view = torrentViews.get(0);
        Assert.assertEquals(infoHash, view.getInfoHash());
        Assert.assertEquals(TorrentStatus.ACTIVE, view.getStatus());
    }

    @Test
    public void testAddMultipleTorrents() {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash1 = new InfoHash("1".getBytes());
        final TorrentTemplate template1 = buildTorrentTemplate(infoHash1, QueueStatus.ACTIVE);

        final InfoHash infoHash2 = new InfoHash("2".getBytes());
        final TorrentTemplate template2 = buildTorrentTemplate(infoHash2, QueueStatus.INACTIVE);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(EasyMock.anyObject(InfoHash.class))).andReturn(true).times(2);
        EasyMock.replay(dataPersistenceSupportMock, trackerManagerMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template1, template2));

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);

        Assert.assertEquals(2, torrentViews.size());

        final TorrentView view1 = torrentViews.get(0);
        Assert.assertEquals(infoHash1, view1.getInfoHash());
        Assert.assertEquals(TorrentStatus.ACTIVE, view1.getStatus());

        final TorrentView view2 = torrentViews.get(1);
        Assert.assertEquals(infoHash2, view2.getInfoHash());
        Assert.assertEquals(TorrentStatus.STOPPED, view2.getStatus());
    }

    @Test
    public void testAddExistingTorrent() {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template1 = buildTorrentTemplate(infoHash, QueueStatus.INACTIVE);
        final TorrentTemplate template2 = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(infoHash)).andReturn(true);
        EasyMock.replay(dataPersistenceSupportMock, trackerManagerMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template1, template2));

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);

        Assert.assertEquals(1, torrentViews.size());

        final TorrentView view = torrentViews.get(0);
        Assert.assertEquals(infoHash, view.getInfoHash());
        Assert.assertEquals(TorrentStatus.STOPPED, view.getStatus());
    }

    @Test
    public void testRemoveNonexistingTorrent() throws IOException {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);
        final QueuedTorrent torrent = new QueuedTorrent(template.getMetaData(), template.getProgress());
        final TorrentView view = new TorrentView(torrent);

        Assert.assertFalse(unitUnderTest.remove(view));
    }

    @Test
    public void testRemoveOnlyTorrent() throws IOException {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(infoHash)).andReturn(true);
        dataPersistenceSupportMock.delete(infoHash);
        EasyMock.expectLastCall();
        EasyMock.replay(dataPersistenceSupportMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template));

        Assert.assertEquals(1, torrentViews.size());

        final TorrentView view = torrentViews.get(0);
        Assert.assertEquals(infoHash, view.getInfoHash());
        Assert.assertEquals(TorrentStatus.ACTIVE, view.getStatus());

        EasyMock.expect(trackerManagerMock.removeTorrent(view)).andReturn(0);
        trackerManagerMock.issueTorrentEvent(view, Tracker.Event.STOPPED);
        EasyMock.expectLastCall();

        EasyMock.replay(trackerManagerMock);

        Assert.assertTrue(unitUnderTest.remove(view));

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);
    }

    @Test
    public void testAddTrackers() {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(infoHash)).andReturn(true);
        EasyMock.replay(dataPersistenceSupportMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template));

        Assert.assertEquals(1, torrentViews.size());

        final TorrentView view = torrentViews.get(0);
        Assert.assertEquals(infoHash, view.getInfoHash());
        Assert.assertEquals(TorrentStatus.ACTIVE, view.getStatus());

        final TrackerSession session1 = EasyMock.createMock(TrackerSession.class);
        final TrackerView view1 = new TrackerView(session1);
        final String tracker1 = "mytracker1";
        EasyMock.expect(trackerManagerMock.addTracker(tracker1, view)).andReturn(view1);

        final TrackerSession session2 = EasyMock.createMock(TrackerSession.class);
        final TrackerView view2 = new TrackerView(session2);
        final String tracker2 = "mytracker2";
        EasyMock.expect(trackerManagerMock.addTracker(tracker2, view)).andReturn(view2);

        EasyMock.replay(trackerManagerMock);

        final List<TrackerView> trackerViews = new ArrayList<>(unitUnderTest.addTrackers(
                view, Arrays.asList(tracker1, tracker2)));

        Assert.assertEquals(2, trackerViews.size());
        Assert.assertTrue(trackerViews.contains(view1));
        Assert.assertTrue(trackerViews.contains(view2));

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);
    }

    @Test
    public void testRemoveTrackers() {
        final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager(dataPersistenceSupportMock, trackerManagerMock);

        final InfoHash infoHash = new InfoHash("1".getBytes());
        final TorrentTemplate template = buildTorrentTemplate(infoHash, QueueStatus.ACTIVE);

        final String tracker1 = "mytracker1";
        final String tracker2 = "mytracker2";

        final Set<String> trackerUrls = new LinkedHashSet<>(Arrays.asList(tracker1, tracker2));
        template.getProgress().addTrackerUrls(trackerUrls);

        EasyMock.expect(dataPersistenceSupportMock.isPersisted(infoHash)).andReturn(true);
        final TrackerSession session1 = EasyMock.createMock(TrackerSession.class);
        final TrackerView view1 = new TrackerView(session1);
        EasyMock.expect(trackerManagerMock.addTracker(EasyMock.eq(tracker1),
                EasyMock.anyObject(TorrentView.class))).andReturn(view1);

        final TrackerSession session2 = EasyMock.createMock(TrackerSession.class);
        final TrackerView view2 = new TrackerView(session2);
        EasyMock.expect(trackerManagerMock.addTracker(EasyMock.eq(tracker2),
                EasyMock.anyObject(TorrentView.class))).andReturn(view2);

        EasyMock.replay(dataPersistenceSupportMock, trackerManagerMock);

        final List<TorrentView> torrentViews = unitUnderTest.addTorrents(Arrays.asList(template));

        Assert.assertEquals(1, torrentViews.size());

        final TorrentView view = torrentViews.get(0);
        Assert.assertEquals(infoHash, view.getInfoHash());
        Assert.assertEquals(TorrentStatus.ACTIVE, view.getStatus());

        final List<TrackableView> trackerViews = new ArrayList<>(view.getTrackableViews());
        Assert.assertEquals(5, trackerViews.size());

        EasyMock.verify(dataPersistenceSupportMock, trackerManagerMock);
    }

    private TorrentTemplate buildTorrentTemplate(final InfoHash infoHash, final QueueStatus targetQueue) {
        final BinaryEncodedDictionary metaDataDict = new BinaryEncodedDictionary();
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO_HASH, new BinaryEncodedString(infoHash.getBytes()));

        final BinaryEncodedDictionary infoDict = new BinaryEncodedDictionary();
        infoDict.put(BinaryEncodingKeys.KEY_LENGTH, new BinaryEncodedInteger(42));
        infoDict.put(BinaryEncodingKeys.KEY_PIECE_LENGTH, new BinaryEncodedInteger(42));

        metaDataDict.put(BinaryEncodingKeys.KEY_INFO, infoDict);
        final QueuedTorrentMetaData metaData = new QueuedTorrentMetaData(metaDataDict);

        final BinaryEncodedDictionary progressDict = new BinaryEncodedDictionary();
        progressDict.put(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME, new BinaryEncodedString(targetQueue.name()));
        final QueuedTorrentProgress progress = new QueuedTorrentProgress(progressDict);

        return new TorrentTemplate(metaData, progress);
    }
}