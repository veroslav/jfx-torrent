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

package org.matic.torrent.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.codec.InfoHash;
import org.matic.torrent.tracker.HttpTracker;
import org.matic.torrent.tracker.TrackableTorrent;
import org.matic.torrent.tracker.Tracker;
import org.matic.torrent.tracker.UdpTracker;

public final class TrackerTest {	
	
	private final InfoHash torrentInfoHash = new InfoHash("12345678901234567890");	
	private final String httpTrackerUrl = "http://localhost:44893/announce";

	@Test
	public final void testTrackerComparison() throws Exception {
		final String udpTrackerUrl = "udp://localhost:44893";
		final String anotherUdpTrackerUrl = "udp://localhost:44894";
		final String anotherHttpTrackerUrl = "http://localhost:44894/announce";
		
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		httpTracker.setNextAnnounce(100002L);
		
		final Tracker udpTracker = new UdpTracker(udpTrackerUrl, null);
		udpTracker.setNextAnnounce(100000L);
		
		final Tracker anotherUdpTracker = new UdpTracker(anotherUdpTrackerUrl, null);
		anotherUdpTracker.setNextAnnounce(100001L);
		
		final Tracker anotherHttpTracker = new HttpTracker(anotherHttpTrackerUrl, null);
		anotherHttpTracker.setNextAnnounce(100003L);
		
		final Set<Tracker> trackers = new TreeSet<>(
				Arrays.asList(httpTracker, udpTracker, anotherHttpTracker, anotherUdpTracker));
		
		final List<Tracker> expectedTrackerOrder = Arrays.asList(udpTracker, anotherUdpTracker,
				httpTracker, anotherHttpTracker);
		final List<Tracker> actualTrackerOrder = new ArrayList<>(trackers);		
		
		Assert.assertEquals(expectedTrackerOrder, actualTrackerOrder);
	}
	
	@Test
	public final void testAddTorrent() throws Exception {
		final String udpTrackerUrl = "udp://localhost:44893";
		
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		final Tracker udpTracker = new UdpTracker(udpTrackerUrl, null);
		
		Assert.assertTrue(httpTracker.addTorrent(torrentInfoHash));
		Assert.assertTrue(udpTracker.addTorrent(torrentInfoHash));
		
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		Assert.assertEquals(1, udpTracker.trackedTorrents.size());
		
		final TrackableTorrent httpTorrent = (TrackableTorrent)httpTracker.getTorrents().toArray()[0];
		Assert.assertEquals(new InfoHash("12345678901234567890"), 
				httpTorrent.getInfoHash());
		final TrackableTorrent udpTorrent = (TrackableTorrent)udpTracker.getTorrents().toArray()[0];
		Assert.assertEquals(new InfoHash("12345678901234567890"), 
				udpTorrent.getInfoHash());
	}
	
	@Test
	public final void testAddExistingTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		
		Assert.assertTrue(httpTracker.addTorrent(torrentInfoHash));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		
		Assert.assertFalse(httpTracker.addTorrent(torrentInfoHash));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
	}
	
	@Test
	public final void testRemoveTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		
		Assert.assertTrue(httpTracker.addTorrent(torrentInfoHash));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		
		Assert.assertTrue(httpTracker.removeTorrent(torrentInfoHash));
		Assert.assertFalse(httpTracker.removeTorrent(torrentInfoHash));
	}
	
	@Test
	public final void testRemoveNonExistingTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);		
		Assert.assertFalse(httpTracker.removeTorrent(new InfoHash("ABABABABABABABABABAB")));
	}
	
	@Test
	public final void testTorrentsTracked() {				
		final InfoHash notTrackedTorrentInfoHash = new InfoHash("ACACACACACACACACACAC");
		final InfoHash anotherTorrentInfoHash = new InfoHash("ABABABABABABABABABAB");
		
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		httpTracker.addTorrent(torrentInfoHash);
		httpTracker.addTorrent(anotherTorrentInfoHash);
		
		Assert.assertEquals(2, httpTracker.getTorrents().size());
		Assert.assertTrue(httpTracker.isTracking(torrentInfoHash));
		Assert.assertTrue(httpTracker.isTracking(anotherTorrentInfoHash));
		Assert.assertFalse(httpTracker.isTracking(notTrackedTorrentInfoHash));
	}	
}
