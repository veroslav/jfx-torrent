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

package org.matic.torrent.queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.tracking.TrackerManager;

public final class QueuedTorrentManagerTest {

	private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("12345678901234567890"));
	private final QueuedTorrent torrent = new QueuedTorrent(infoHash, 1, QueuedTorrent.State.ACTIVE);
	
	private TrackerManager trackerManagerMock;
	private QueuedTorrentManager unitUnderTest;
	
	@Before
	public void setup() {
		 trackerManagerMock = EasyMock.createMock(TrackerManager.class);
		 unitUnderTest = new QueuedTorrentManager(trackerManagerMock);
	}
	
	@Test
	public void testAddSingleTorrent() {
		final String simpleUrl = "a.simple.url";
		final Set<String> urls = new HashSet<>(Arrays.asList(simpleUrl));
		
		EasyMock.expect(trackerManagerMock.addTracker(simpleUrl, torrent)).andReturn(true);
		EasyMock.replay(trackerManagerMock);
		
		final boolean added = unitUnderTest.add(torrent, urls);
		
		EasyMock.verify(trackerManagerMock);
		
		Assert.assertTrue(added);
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
	}
	
	@Test
	public void testAddMultipleTorrents() {
		final QueuedTorrent otherTorrent = new QueuedTorrent(new InfoHash(
				DatatypeConverter.parseHexBinary("12345678901234567891")), 1, QueuedTorrent.State.ACTIVE);
		final boolean addedFirst = unitUnderTest.add(torrent, Collections.emptySet());
		final boolean addedOther = unitUnderTest.add(otherTorrent, Collections.emptySet());
		
		Assert.assertTrue(addedFirst && addedOther);
		Assert.assertEquals(2, unitUnderTest.getQueueSize());
	}
	
	@Test
	public void testAddExistingTorrent() {
		final boolean firstAddition = unitUnderTest.add(torrent, Collections.emptySet());
		final boolean secondAddition = unitUnderTest.add(torrent, Collections.emptySet());
		
		Assert.assertTrue(firstAddition);
		Assert.assertFalse(secondAddition);
		
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
	}
	
	@Test
	public void testRemoveNonexistingTorrent() {
		final boolean removed = unitUnderTest.remove(torrent);
		Assert.assertFalse(removed);
		Assert.assertEquals(0, unitUnderTest.getQueueSize());		
	}
	
	@Test
	public void testRemoveOnlyTorrent() {
		unitUnderTest.add(torrent, Collections.emptySet());
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
		
		EasyMock.expect(trackerManagerMock.removeTorrent(torrent)).andReturn(1);
		EasyMock.replay(trackerManagerMock);
		
		final boolean removed = unitUnderTest.remove(torrent);
		
		EasyMock.verify(trackerManagerMock);
		
		Assert.assertTrue(removed);
		Assert.assertEquals(0, unitUnderTest.getQueueSize());
	}
	
	@Test
	public void testRemoveOneFromMultipleTorrents() {
		final QueuedTorrent otherTorrent = new QueuedTorrent(new InfoHash(
				DatatypeConverter.parseHexBinary("12345678901234567891")), 1, QueuedTorrent.State.ACTIVE);
		unitUnderTest.add(torrent, Collections.emptySet());
		unitUnderTest.add(otherTorrent, Collections.emptySet());
		
		EasyMock.expect(trackerManagerMock.removeTorrent(otherTorrent)).andReturn(1);
		EasyMock.replay(trackerManagerMock);
		
		final boolean removed = unitUnderTest.remove(otherTorrent);
		
		EasyMock.verify(trackerManagerMock);
		
		Assert.assertTrue(removed);
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
		Assert.assertTrue(unitUnderTest.find(torrent.getInfoHash()).isPresent());
	}
}