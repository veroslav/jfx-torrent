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

package org.matic.torrent.peer.tracking.tracker;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class TrackerTest {	
	
	private final TrackableTorrent torrent = new TrackableTorrent(
			"12345678901234567890".getBytes(Charset.forName("UTF-8")));	
	
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
		
		Assert.assertTrue(httpTracker.addTorrent(torrent));
		Assert.assertTrue(udpTracker.addTorrent(torrent));
		
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		Assert.assertEquals(1, udpTracker.trackedTorrents.size());
		
		final TrackableTorrent httpTorrent = (TrackableTorrent)httpTracker.getTorrents().toArray()[0];
		Assert.assertEquals("3132333435363738393031323334353637383930", 
				httpTorrent.getInfoHashHexValue());
		final TrackableTorrent udpTorrent = (TrackableTorrent)udpTracker.getTorrents().toArray()[0];
		Assert.assertEquals("3132333435363738393031323334353637383930", 
				udpTorrent.getInfoHashHexValue());
	}
	
	@Test
	public final void testAddExistingTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		
		Assert.assertTrue(httpTracker.addTorrent(torrent));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		
		Assert.assertFalse(httpTracker.addTorrent(torrent));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
	}
	
	@Test
	public final void testRemoveTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		
		Assert.assertTrue(httpTracker.addTorrent(torrent));
		Assert.assertEquals(1, httpTracker.trackedTorrents.size());
		
		Assert.assertTrue(httpTracker.removeTorrent(torrent));
		Assert.assertFalse(httpTracker.removeTorrent(torrent));
	}
	
	@Test
	public final void testRemoveNonExistingTorrent() {
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		
		final TrackableTorrent nonExistingTorrent = new TrackableTorrent(
				"ABABABABABABABABABAB".getBytes(Charset.forName("UTF-8")));
		
		Assert.assertFalse(httpTracker.removeTorrent(nonExistingTorrent));
	}
	
	@Test
	public final void testTorrentsTracked() {				
		final TrackableTorrent anotherTorrent = new TrackableTorrent(
				"ABABABABABABABABABAB".getBytes(Charset.forName("UTF-8")));
		final TrackableTorrent notTrackedTorrent = new TrackableTorrent(
				"ACACACACACACACACACAC".getBytes(Charset.forName("UTF-8")));
		
		final Tracker httpTracker = new HttpTracker(httpTrackerUrl, null);
		httpTracker.addTorrent(torrent);
		httpTracker.addTorrent(anotherTorrent);
		
		final TrackableTorrent expectedTorrent = 
				new TrackableTorrent(torrent.getInfoHashBytes());
		final TrackableTorrent expectedAnotherTorrent = 
				new TrackableTorrent(anotherTorrent.getInfoHashBytes());
		
		Assert.assertEquals(2, httpTracker.getTorrents().size());
		Assert.assertTrue(httpTracker.isTracking(expectedTorrent));
		Assert.assertTrue(httpTracker.isTracking(expectedAnotherTorrent));
		Assert.assertFalse(httpTracker.isTracking(notTrackedTorrent));
	}	
}
