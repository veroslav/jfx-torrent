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

package org.matic.torrent.utils;

import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.NetworkProperties;
import org.matic.torrent.tracking.TrackerManager;

public final class ResourceManager {
	
	//The UDP server/client for communication with trackers that support UDP protocol
	private final UdpConnectionManager udpTrackerConnectionManager;
	
	//The UDP server/client for communication through DHT
	private final UdpConnectionManager dhtConnectionManager;
	
	//The manager for trackers supporting HTTP and UDP protocols
	private final TrackerManager trackerManager = new TrackerManager();
	
	private final int udpTrackerPort;
	private final int dhtPort;
	
	public static final ResourceManager INSTANCE = new ResourceManager();
	
	static {
		INSTANCE.init();
	}
	
	private ResourceManager() {
		udpTrackerPort = Integer.parseInt(ApplicationPreferences.getProperty(
				NetworkProperties.UDP_TRACKER_PORT, String.valueOf(UdpConnectionManager.DEFAULT_UDP_PORT)));
		dhtPort = Integer.parseInt(ApplicationPreferences.getProperty(
				NetworkProperties.UDP_DHT_PORT, String.valueOf(UdpConnectionManager.DEFAULT_UDP_PORT)));		
		
		udpTrackerConnectionManager = new UdpConnectionManager();
		udpTrackerConnectionManager.addTrackerListener(trackerManager);		
		
		dhtConnectionManager = udpTrackerPort == dhtPort? 
				udpTrackerConnectionManager : new UdpConnectionManager();
	}
	
	private void init() {		
		udpTrackerConnectionManager.manage("", udpTrackerPort);
		dhtConnectionManager.manage("", dhtPort);
	}
	
	public void cleanup() {
		//Remove all listeners before shutting down
		udpTrackerConnectionManager.removeTrackerListener(trackerManager);
		
		if(dhtConnectionManager != udpTrackerConnectionManager) {
			dhtConnectionManager.unmanage();
		}
		
		//Stop all threaded resources and perform cleanup
		trackerManager.stop();
		udpTrackerConnectionManager.unmanage();
		dhtConnectionManager.unmanage();
	}
	
	public final int getDhtPort() {
		return dhtPort;
	}
	
	public final int getUdpTrackerPort() {
		return udpTrackerPort;
	}

	public final UdpConnectionManager getUdpTrackerConnectionManager() {
		return udpTrackerConnectionManager;
	}
	
	public final UdpConnectionManager getDhtConnectionManager() {
		return dhtConnectionManager;
	}

	public final TrackerManager getTrackerManager() {
		return trackerManager;
	}	
}