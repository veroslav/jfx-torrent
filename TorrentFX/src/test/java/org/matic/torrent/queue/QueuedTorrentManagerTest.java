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

import java.util.Collections;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.hash.InfoHash;

public final class QueuedTorrentManagerTest {

	private final InfoHash infoHash = new InfoHash(DatatypeConverter.parseHexBinary("12345678901234567890"));
	private final QueuedTorrent torrent = new QueuedTorrent(infoHash, 1, QueuedTorrent.Status.ACTIVE);
	private final QueuedTorrentManager unitUnderTest = new QueuedTorrentManager();	
	
	@Test
	public void testAddSingleTorrent() {
		final boolean added = unitUnderTest.add(torrent, Collections.emptySet());
		Assert.assertTrue(added);
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
	}
	
	/*@Test
	public void testAddMultipleTorrents() {
		final QueuedTorrent otherTorrent = new QueuedTorrent(new InfoHash(
				DatatypeConverter.parseHexBinary("12345678901234567890")), Collections.emptySet(), 1);
		final boolean addedFirst = unitUnderTest.add(torrent);
		final boolean addedOther = unitUnderTest.add(otherTorrent);
		
		Assert.assertTrue(addedFirst && addedOther);
		Assert.assertEquals(2, unitUnderTest.getQueueSize());
	}*/
	
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
		
		final boolean removed = unitUnderTest.remove(torrent);
		
		Assert.assertTrue(removed);
		Assert.assertEquals(0, unitUnderTest.getQueueSize());
	}
	
	/*@Test
	public void testRemoveOneFromMultipleTorrents() {
		final QueuedTorrent otherTorrent = new QueuedTorrent(new InfoHash(
				DatatypeConverter.parseHexBinary("12345678901234567890")), Collections.emptySet(), 1);
		unitUnderTest.add(torrent);
		unitUnderTest.add(otherTorrent);
		
		final boolean removed = unitUnderTest.remove(otherTorrent.getInfoHash());
		Assert.assertTrue(removed);
		Assert.assertEquals(1, unitUnderTest.getQueueSize());
		Assert.assertTrue(unitUnderTest.contains(torrent));
	}*/
}