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

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class QueueControllerTest {

    private final int maxActiveTorrents = 2;
    private final int maxDownloadingTorrents = 1;
    private final int maxUploadingTorrents = 1;

    private final ObservableList<QueuedTorrent> torrents = FXCollections.observableArrayList();

    @After
    public void cleanup() {
        torrents.clear();
    }

    @Test
    public void testAddActiveTorrentNoOtherTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent torrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(torrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, torrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, torrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrent.getStatus());
        Assert.assertFalse(torrent.isForced());
    }

    @Test
    public void testAddInactiveTorrentNoOtherTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent torrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(torrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, torrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, torrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, torrent.getStatus());
        Assert.assertFalse(torrent.isForced());
    }

    @Test
    public void testAddActiveTorrentOneOtherActiveTorrentWithinQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        final QueuedTorrent torrentToAdd = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(torrentToAdd);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, otherTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(2, torrentToAdd.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, torrentToAdd.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testAddActiveTorrentOneOtherActiveTorrentOutsideQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        final QueuedTorrent torrentToAdd = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, otherTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(2, torrentToAdd.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, torrentToAdd.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testAddForcedTorrentOneOtherActiveTorrentOutsideQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        final QueuedTorrent torrentToAdd = buildTorrent("2", TorrentStatus.ACTIVE);
        torrentToAdd.getProgress().setForced(true);

        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, otherTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, torrentToAdd.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, torrentToAdd.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrentToAdd.getStatus());
        Assert.assertTrue(torrentToAdd.isForced());
    }

    @Test
    public void testAddActiveTorrentOneActiveOneInactiveTorrentOutsideQueueLimit() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent inactiveTorrent = buildTorrent("2", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        final QueuedTorrent torrentToAdd = buildTorrent("3", TorrentStatus.ACTIVE);

        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(3, torrentToAdd.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, torrentToAdd.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testRemoveActiveTorrentOneQueuedTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        torrents.remove(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());
    }

    @Test
    public void testRemoveOneFromEachQueue() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent inactiveTorrent = buildTorrent("3", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", TorrentStatus.ACTIVE);
        forcedTorrent.getProgress().setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        Assert.assertEquals(3, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
        Assert.assertTrue(forcedTorrent.isForced());

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        torrents.remove(forcedTorrent);

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, forcedTorrent.getStatus());
        Assert.assertFalse(forcedTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        torrents.remove(inactiveTorrent);

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        torrents.remove(queuedTorrent);

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        torrents.remove(activeTorrent);

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
    }

    @Test
    public void testTorrentPriorityChangeBetweenActiveAndQueuedQueues() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        //Test changing a queued torrent's priority to a higher (active) level
        unitUnderTest.onPriorityChangeRequested(queuedTorrent, PriorityChange.HIGHER);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        //Test changing an active torrent's priority to a lower (queued) level
        unitUnderTest.onPriorityChangeRequested(queuedTorrent, PriorityChange.LOWER);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());
    }

    @Test
    public void testActiveTorrentPriorityChange() {
        final QueuedTorrent firstActiveTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(firstActiveTorrent);

        final QueuedTorrent secondActiveTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(secondActiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueStatus.ACTIVE, firstActiveTorrent, secondActiveTorrent, unitUnderTest);
    }

    @Test
    public void testInactiveTorrentPriorityChange() {
        final QueuedTorrent firstInactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(firstInactiveTorrent);

        final QueuedTorrent secondInactiveTorrent = buildTorrent("2", TorrentStatus.STOPPED);
        torrents.add(secondInactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueStatus.INACTIVE, firstInactiveTorrent, secondInactiveTorrent, unitUnderTest);
    }

    @Test
    public void testQueuedTorrentPriorityChange() {
        final QueuedTorrent firstQueuedTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(firstQueuedTorrent);

        final QueuedTorrent secondQueuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(secondQueuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueStatus.QUEUED, firstQueuedTorrent, secondQueuedTorrent, unitUnderTest);
    }

    @Test
    public void testForcedTorrentPriorityChange() {
        final QueuedTorrent firstForcedTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        firstForcedTorrent.getProgress().setForced(true);
        torrents.add(firstForcedTorrent);

        final QueuedTorrent secondForcedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        secondForcedTorrent.getProgress().setForced(true);
        torrents.add(secondForcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueStatus.FORCED, firstForcedTorrent, secondForcedTorrent, unitUnderTest);
    }

    @Test
    public void testActiveQueueLimitRaisedOneQueuedTorrent() {
        final QueuedTorrent queuedTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        unitUnderTest.onQueueLimitsChanged(TransferProperties.ACTIVE_TORRENTS_LIMIT, 1);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
    }

    @Test
    public void testActiveQueueLimitLoweredOneActiveTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        unitUnderTest.onQueueLimitsChanged(TransferProperties.ACTIVE_TORRENTS_LIMIT, 0);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
    }

    @Test
    public void testActiveQueueLimitRaisedNoQueuedTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("3", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        forcedTorrent.getProgress().setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        validateQueuesNotChanged(unitUnderTest, inactiveTorrent, activeTorrent, forcedTorrent);

        unitUnderTest.onQueueLimitsChanged(TransferProperties.ACTIVE_TORRENTS_LIMIT, 2);

        validateQueuesNotChanged(unitUnderTest, inactiveTorrent, activeTorrent, forcedTorrent);
    }

    @Test
    public void testAdditionOfHigherPriorityTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        final QueuedTorrent activeTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        final QueuedTorrent queuedTorrent = buildTorrent("3", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
    }

    @Test
    public void testRemovalOfHigherPriorityTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        torrents.remove(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        torrents.remove(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        torrents.remove(inactiveTorrent);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(QueuedTorrent.UKNOWN_PRIORITY, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.NOT_ON_QUEUE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
    }

    @Test
    public void testStartAndStopInactiveTorrentNoOtherTorrentWithinQueueLimits() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
    }

    @Test
    public void testStopAndRestartActiveTorrentOneQueuedTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
    }

    @Test
    public void testStopActiveTorrentAfterQueuedTorrentHasBeenStopped() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(queuedTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(2, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
    }

    @Test
    public void testStopAndRestartActiveTorrentNoOtherTorrents() {
        final QueuedTorrent activeTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
    }

    @Test
    public void testStartAndStopInactiveTorrentNoOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
    }

    @Test
    public void testStopAndRestartActiveTorrentMultipleOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", TorrentStatus.ACTIVE);
        forcedTorrent.getProgress().setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(2, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
    }

    @Test
    public void testStartAndStopInactiveTorrentMultipleOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", TorrentStatus.STOPPED);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", TorrentStatus.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", TorrentStatus.ACTIVE);
        forcedTorrent.getProgress().setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(2, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.QUEUED, queuedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
    }

    @Ignore
    @Test
    public void testStopForcedTorrentAndRestartNormallyWithinQueueLimits() {
        final QueuedTorrent forcedTorrent = buildTorrent("1", TorrentStatus.ACTIVE);
        forcedTorrent.getProgress().setForced(true);
        torrents.add(forcedTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", TorrentStatus.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(forcedTorrent, TorrentStatus.STOPPED));
    }

    @Test
    public void testChangePriorityNonExistentTorrent() {
        final QueuedTorrent unknownTorrent = buildTorrent("2", TorrentStatus.ACTIVE);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertFalse(unitUnderTest.onPriorityChangeRequested(unknownTorrent, PriorityChange.FORCED));
    }

    @Test
    public void testChangeStatusNonExistentTorrent() {
        final QueuedTorrent unknownTorrent = buildTorrent("2", TorrentStatus.ACTIVE);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertFalse(unitUnderTest.changeStatus(unknownTorrent, TorrentStatus.STOPPED));
    }

    private void validateQueuesNotChanged(final QueueController unitUnderTest,
                                          final QueuedTorrent inactiveTorrent, final QueuedTorrent activeTorrent,
                                          final QueuedTorrent forcedTorrent) {
        Assert.assertEquals(0, unitUnderTest.getQueueSize(QueueStatus.QUEUED));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.ACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.INACTIVE));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(QueueStatus.FORCED));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueStatus.INACTIVE, inactiveTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueStatus.ACTIVE, activeTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.FORCED_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueStatus.FORCED, forcedTorrent.getQueueStatus());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
    }

    private void validatePriorityChangeWithinQueue(final QueueStatus queue, final QueuedTorrent firstTorrent,
                                                   final QueuedTorrent secondTorrent, final QueueController unitUnderTest) {
        Assert.assertEquals(2, unitUnderTest.getQueueSize(queue));

        Assert.assertEquals(firstTorrent.isForced()? -1 : 1, firstTorrent.getPriority());

        Assert.assertEquals(queue, firstTorrent.getQueueStatus());

        Assert.assertEquals(secondTorrent.isForced()? -1 : 2, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueStatus());

        //Test changing a torrent's priority within a queue to a higher level within the queue
        unitUnderTest.onPriorityChangeRequested(secondTorrent, PriorityChange.HIGHER);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(queue));

        Assert.assertEquals(firstTorrent.isForced()? -1 : 2, firstTorrent.getPriority());
        Assert.assertEquals(queue, firstTorrent.getQueueStatus());

        Assert.assertEquals(secondTorrent.isForced()? -1 : 1, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueStatus());

        //Test changing a torrent's priority within a queue to a lower level within the queue
        unitUnderTest.onPriorityChangeRequested(secondTorrent, PriorityChange.LOWER);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(queue));

        Assert.assertEquals(firstTorrent.isForced()? -1 : 1, firstTorrent.getPriority());
        Assert.assertEquals(queue, firstTorrent.getQueueStatus());

        Assert.assertEquals(secondTorrent.isForced()? -1 : 2, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueStatus());
    }

    private QueuedTorrent buildTorrent(final String infoHash, final TorrentStatus initialStatus) {
        final BinaryEncodedDictionary metaDataDict = new BinaryEncodedDictionary();
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO_HASH, new BinaryEncodedString(infoHash));
        final QueuedTorrentMetaData metaData = new QueuedTorrentMetaData(metaDataDict);

        final BinaryEncodedDictionary progressDict = new BinaryEncodedDictionary();
        progressDict.put(BinaryEncodingKeys.STATE_KEY_TORRENT_STATUS, new BinaryEncodedString(initialStatus.name()));
        //progressDict.get(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME
        final QueuedTorrentProgress progress = new QueuedTorrentProgress(progressDict);

        return new QueuedTorrent(metaData, progress);
    }
}