/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.EnumSet;

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

        final QueuedTorrent torrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(torrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        Assert.assertEquals(1, torrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, torrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrent.getStatus());
        Assert.assertFalse(torrent.isForced());
    }

    @Test
    public void testAddInactiveTorrentNoOtherTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent torrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(torrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        Assert.assertEquals(1, torrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, torrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, torrent.getStatus());
        Assert.assertFalse(torrent.isForced());
    }

    @Test
    public void testAddActiveTorrentOneOtherActiveTorrentWithinQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        final QueuedTorrent torrentToAdd = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(torrentToAdd);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, otherTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(2, torrentToAdd.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, torrentToAdd.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testAddActiveTorrentOneOtherActiveTorrentOutsideQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        final QueuedTorrent torrentToAdd = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, otherTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(2, torrentToAdd.getPriority());
        Assert.assertEquals(QueueType.QUEUED, torrentToAdd.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testAddForcedTorrentOneOtherActiveTorrentOutsideQueueLimit() {
        final QueuedTorrent otherTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(otherTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        final QueuedTorrent torrentToAdd = buildTorrent("2", QueueType.ACTIVE);
        torrentToAdd.setForced(true);

        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        Assert.assertEquals(1, otherTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, otherTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, otherTorrent.getStatus());
        Assert.assertFalse(otherTorrent.isForced());

        Assert.assertEquals(2, torrentToAdd.getPriority());
        Assert.assertEquals(QueueType.FORCED, torrentToAdd.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, torrentToAdd.getStatus());
        Assert.assertTrue(torrentToAdd.isForced());
    }

    @Test
    public void testAddActiveTorrentOneActiveOneInactiveTorrentOutsideQueueLimit() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent inactiveTorrent = buildTorrent("2", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        final QueuedTorrent torrentToAdd = buildTorrent("3", QueueType.ACTIVE);

        torrents.add(torrentToAdd);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(3, torrentToAdd.getPriority());
        Assert.assertEquals(QueueType.QUEUED, torrentToAdd.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, torrentToAdd.getStatus());
        Assert.assertFalse(torrentToAdd.isForced());
    }

    @Test
    public void testRemoveActiveTorrentOneQueuedTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        torrents.remove(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());
    }

    @Test
    public void testRemoveOneFromEachQueue() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent inactiveTorrent = buildTorrent("3", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", QueueType.ACTIVE);
        forcedTorrent.setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        Assert.assertEquals(3, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(4, forcedTorrent.getPriority());
        Assert.assertEquals(QueueType.FORCED, forcedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
        Assert.assertTrue(forcedTorrent.isForced());

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        torrents.remove(forcedTorrent);

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, forcedTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, forcedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, forcedTorrent.getStatus());
        Assert.assertFalse(forcedTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        torrents.remove(inactiveTorrent);

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
        Assert.assertFalse(inactiveTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        torrents.remove(queuedTorrent);

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        torrents.remove(activeTorrent);

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
    }

    @Test
    public void testTorrentPriorityChangeBetweenActiveAndQueuedQueues() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        //Test changing a queued torrent's priority to a higher (active) level
        unitUnderTest.onPriorityChangeRequested(queuedTorrent, PriorityChange.HIGHER);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());

        //Test changing an active torrent's priority to a lower (queued) level
        unitUnderTest.onPriorityChangeRequested(queuedTorrent, PriorityChange.LOWER);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());
        Assert.assertFalse(activeTorrent.isForced());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
        Assert.assertFalse(queuedTorrent.isForced());
    }

    @Test
    public void testActiveTorrentPriorityChange() {
        final QueuedTorrent firstActiveTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(firstActiveTorrent);

        final QueuedTorrent secondActiveTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(secondActiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueType.ACTIVE, firstActiveTorrent, secondActiveTorrent, unitUnderTest);
    }

    @Test
    public void testInactiveTorrentPriorityChange() {
        final QueuedTorrent firstInactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(firstInactiveTorrent);

        final QueuedTorrent secondInactiveTorrent = buildTorrent("2", QueueType.INACTIVE);
        torrents.add(secondInactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, maxActiveTorrents, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueType.INACTIVE, firstInactiveTorrent, secondInactiveTorrent, unitUnderTest);
    }

    @Test
    public void testQueuedTorrentPriorityChange() {
        final QueuedTorrent firstQueuedTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(firstQueuedTorrent);

        final QueuedTorrent secondQueuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(secondQueuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueType.QUEUED, firstQueuedTorrent, secondQueuedTorrent, unitUnderTest);
    }

    @Test
    public void testForcedTorrentPriorityChange() {
        final QueuedTorrent firstForcedTorrent = buildTorrent("1", QueueType.ACTIVE);
        firstForcedTorrent.setForced(true);
        torrents.add(firstForcedTorrent);

        final QueuedTorrent secondForcedTorrent = buildTorrent("2", QueueType.ACTIVE);
        secondForcedTorrent.setForced(true);
        torrents.add(secondForcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        validatePriorityChangeWithinQueue(QueueType.FORCED, firstForcedTorrent, secondForcedTorrent, unitUnderTest);
    }

    @Test
    public void testActiveQueueLimitRaisedOneQueuedTorrent() {
        final QueuedTorrent queuedTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 0, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        unitUnderTest.onQueueLimitsChanged(TransferProperties.ACTIVE_TORRENTS_LIMIT, 1);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());
    }

    @Test
    public void testActiveQueueLimitLoweredOneActiveTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        unitUnderTest.onQueueLimitsChanged(TransferProperties.ACTIVE_TORRENTS_LIMIT, 0);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());
    }

    @Test
    public void testActiveQueueLimitRaisedNoQueuedTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("3", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("2", QueueType.ACTIVE);
        forcedTorrent.setForced(true);
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

        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        final QueuedTorrent activeTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        final QueuedTorrent queuedTorrent = buildTorrent("3", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());
    }

    @Test
    public void testRemovalOfHigherPriorityTorrents() {
        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(3, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.QUEUED, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        torrents.remove(activeTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, queuedTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, activeTorrent.getStatus());

        torrents.remove(queuedTorrent);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, queuedTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, queuedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, queuedTorrent.getStatus());

        torrents.remove(inactiveTorrent);

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        Assert.assertEquals(QueuedTorrent.UNKNOWN_PRIORITY, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.NOT_ON_QUEUE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
    }

    @Test
    public void testStartAndStopInactiveTorrentNoOtherTorrentWithinQueueLimits() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, inactiveTorrent.getStatus());

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());
    }

    //TODO: CONTINUE REFACTORING
    @Test
    public void testStopAndRestartActiveTorrentOneQueuedTorrent() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.QUEUED, TorrentStatus.STOPPED);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.ACTIVE, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.QUEUED, TorrentStatus.STOPPED);
    }

    @Test
    public void testStopActiveTorrentAfterQueuedTorrentHasBeenStopped() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.QUEUED, TorrentStatus.STOPPED);

        Assert.assertTrue(unitUnderTest.changeStatus(queuedTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.INACTIVE, TorrentStatus.STOPPED);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(queuedTorrent, 2, QueueType.INACTIVE, TorrentStatus.STOPPED);
    }

    @Test
    public void testStopAndRestartActiveTorrentNoOtherTorrents() {
        final QueuedTorrent activeTorrent = buildTorrent("1", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(activeTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
    }

    @Test
    public void testStartAndStopInactiveTorrentNoOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
    }

    @Test
    public void testStopAndRestartActiveTorrentMultipleOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", QueueType.ACTIVE);
        forcedTorrent.setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 3, QueueType.QUEUED, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(forcedTorrent, 4, QueueType.FORCED, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(activeTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(queuedTorrent, 3, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(forcedTorrent, 4, QueueType.FORCED, TorrentStatus.ACTIVE);
    }

    @Test
    public void testStartAndStopInactiveTorrentMultipleOtherTorrents() {
        final QueuedTorrent inactiveTorrent = buildTorrent("1", QueueType.INACTIVE);
        torrents.add(inactiveTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueuedTorrent queuedTorrent = buildTorrent("3", QueueType.ACTIVE);
        torrents.add(queuedTorrent);

        final QueuedTorrent forcedTorrent = buildTorrent("4", QueueType.ACTIVE);
        forcedTorrent.setForced(true);
        torrents.add(forcedTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 3, QueueType.QUEUED, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(forcedTorrent, 4, QueueType.FORCED, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.ACTIVE));

        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.QUEUED, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(queuedTorrent, 3, QueueType.QUEUED, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(forcedTorrent, 4, QueueType.FORCED, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(inactiveTorrent, TorrentStatus.STOPPED));

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(inactiveTorrent, 1, QueueType.INACTIVE, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.ACTIVE, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(queuedTorrent, 3, QueueType.QUEUED, TorrentStatus.STOPPED);
        validateTorrentsPriorityAndQueue(forcedTorrent, 4, QueueType.FORCED, TorrentStatus.ACTIVE);
    }

    @Test
    public void testStopForcedTorrentAndRestartNormallyWithinQueueLimits() {
        final QueuedTorrent forcedTorrent = buildTorrent("1", QueueType.ACTIVE);
        forcedTorrent.setForced(true);
        torrents.add(forcedTorrent);

        final QueuedTorrent activeTorrent = buildTorrent("2", QueueType.ACTIVE);
        torrents.add(activeTorrent);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        validateTorrentsPriorityAndQueue(forcedTorrent, 1, QueueType.FORCED, TorrentStatus.ACTIVE);
        validateTorrentsPriorityAndQueue(activeTorrent, 2, QueueType.ACTIVE, TorrentStatus.ACTIVE);

        Assert.assertTrue(unitUnderTest.changeStatus(forcedTorrent, TorrentStatus.STOPPED));
    }

    @Test
    public void testChangePriorityNonExistentTorrent() {
        final QueuedTorrent unknownTorrent = buildTorrent("2", QueueType.ACTIVE);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertFalse(unitUnderTest.onPriorityChangeRequested(unknownTorrent, PriorityChange.FORCED));
    }

    @Test
    public void testChangeStatusNonExistentTorrent() {
        final QueuedTorrent unknownTorrent = buildTorrent("2", QueueType.ACTIVE);

        final QueueController unitUnderTest = new QueueController(
                torrents, 1, maxDownloadingTorrents, maxUploadingTorrents);

        Assert.assertFalse(unitUnderTest.changeStatus(unknownTorrent, TorrentStatus.STOPPED));
    }

    @Test
    public void testAddActiveTorrentToTopOfQueueAndOneOtherActiveTorrent() {

    }

    @Test
    public void testAddInactiveTorrentToTopOfQueueAndOneOtherActiveTorrent() {

    }

    @Test
    public void testAddActiveTorrentToTopOfQueueAndOneOtherInactiveTorrent() {

    }

    @Test
    public void testAddInactiveTorrentToTopOfQueueAndOneOtherInactiveTorrent() {

    }

    @Test
    public void testAddActiveTorrentToTopOfQueueAndOneOtherActiveAndQueuedTorrent() {

    }

    private void validateTorrentsPriorityAndQueue(final QueuedTorrent torrent, final int expectedPrio,
                                                  final QueueType expectedQueue, final TorrentStatus expectedStatus) {
        Assert.assertEquals(expectedPrio, torrent.getPriority());
        Assert.assertEquals(expectedQueue, torrent.getQueueType());
        Assert.assertEquals(expectedStatus, torrent.getStatus());
    }

    private void validateQueuesNotChanged(final QueueController unitUnderTest,
                                          final QueuedTorrent inactiveTorrent, final QueuedTorrent activeTorrent,
                                          final QueuedTorrent forcedTorrent) {
        Assert.assertEquals(0, unitUnderTest.getQueueSize(EnumSet.of(QueueType.QUEUED)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.ACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.INACTIVE)));
        Assert.assertEquals(1, unitUnderTest.getQueueSize(EnumSet.of(QueueType.FORCED)));

        Assert.assertEquals(1, inactiveTorrent.getPriority());
        Assert.assertEquals(QueueType.INACTIVE, inactiveTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.STOPPED, inactiveTorrent.getStatus());

        Assert.assertEquals(2, activeTorrent.getPriority());
        Assert.assertEquals(QueueType.ACTIVE, activeTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, activeTorrent.getStatus());

        Assert.assertEquals(3, forcedTorrent.getPriority());
        Assert.assertEquals(QueueType.FORCED, forcedTorrent.getQueueType());
        Assert.assertEquals(TorrentStatus.ACTIVE, forcedTorrent.getStatus());
    }

    private void validatePriorityChangeWithinQueue(final QueueType queue, final QueuedTorrent firstTorrent,
                                                   final QueuedTorrent secondTorrent, final QueueController unitUnderTest) {
        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(queue)));

        Assert.assertEquals(1, firstTorrent.getPriority());
        Assert.assertEquals(queue, firstTorrent.getQueueType());

        Assert.assertEquals(2, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueType());

        //Test changing a torrent's priority within a queue to a higher level within the queue
        unitUnderTest.onPriorityChangeRequested(secondTorrent, PriorityChange.HIGHER);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(queue)));

        Assert.assertEquals(2, firstTorrent.getPriority());
        Assert.assertEquals(queue, firstTorrent.getQueueType());

        Assert.assertEquals(1, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueType());

        //Test changing a torrent's priority within a queue to a lower level within the queue
        unitUnderTest.onPriorityChangeRequested(secondTorrent, PriorityChange.LOWER);

        Assert.assertEquals(2, unitUnderTest.getQueueSize(EnumSet.of(queue)));

        Assert.assertEquals(1, firstTorrent.getPriority());
        Assert.assertEquals(queue, firstTorrent.getQueueType());

        Assert.assertEquals(2, secondTorrent.getPriority());
        Assert.assertEquals(queue, secondTorrent.getQueueType());
    }

    private QueuedTorrent buildTorrent(final String infoHash, final QueueType targetQueue) {
        final BinaryEncodedDictionary metaDataDict = new BinaryEncodedDictionary();
        metaDataDict.put(BinaryEncodingKeys.KEY_INFO_HASH, new BinaryEncodedString(infoHash));
        final QueuedTorrentMetaData metaData = new QueuedTorrentMetaData(metaDataDict);

        final BinaryEncodedDictionary progressDict = new BinaryEncodedDictionary();
        progressDict.put(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME, new BinaryEncodedString(targetQueue.name()));
        final QueuedTorrentProgress progress = new QueuedTorrentProgress(progressDict);

        return new QueuedTorrent(metaData, progress);
    }
}