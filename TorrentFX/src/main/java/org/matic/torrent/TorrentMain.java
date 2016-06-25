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

package org.matic.torrent;

import javafx.application.Application;
import javafx.stage.Stage;
import org.matic.torrent.gui.window.ApplicationWindow;
import org.matic.torrent.io.DataPersistenceSupport;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.PathProperties;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.tracking.TrackerManager;

import java.io.File;

public final class TorrentMain extends Application {

    //The UDP server/client for communication with trackers that support UDP protocol
    private static final UdpConnectionManager UDP_TRACKER_CONNECTION_MANAGER = new UdpConnectionManager();

    //The manager for trackers supporting HTTP(S) and UDP protocols
    private static final TrackerManager TRACKER_MANAGER =
            new TrackerManager(UDP_TRACKER_CONNECTION_MANAGER, TrackerManager.DEFAULT_REQUEST_SCHEDULER_POOL_SIZE);

    private static final String WORK_PATH = ApplicationPreferences.getProperty(PathProperties.NEW_TORRENTS,
            PathProperties.DEFAULT_STORE_TORRENTS_PATH);

    private static final DataPersistenceSupport PERSISTENCE_SUPPORT = new DataPersistenceSupport(
            WORK_PATH.endsWith(File.separator)? WORK_PATH : WORK_PATH + File.separator);

    //The manager for handling queued torrents' states
    private static final QueuedTorrentManager TORRENT_MANAGER = new QueuedTorrentManager(PERSISTENCE_SUPPORT, TRACKER_MANAGER);

    //The UDP server/client for communication through DHT
    private static final UdpConnectionManager DHT_CONNECTION_MANAGER = UdpConnectionManager.UDP_TRACKER_PORT ==
            UdpConnectionManager.DHT_PORT? UDP_TRACKER_CONNECTION_MANAGER : new UdpConnectionManager();

	/**
	 * Main application execution entry point. Used when the application packaging
	 * is performed by other means than by JavaFX
	 * 
	 * @param args Application parameters
	 */
	public static void main(final String[] args) throws Exception {				
		//Improve font rendering on Linux
		System.setProperty("prism.lcdtext", "true");

		//Initialize threaded resources
		TorrentMain.startup();
		
		launch(args);
		
		//Perform resource cleanup before shutdown
		TorrentMain.cleanup();
	}

	@Override
	public void start(final Stage stage) {
        new ApplicationWindow(stage, TRACKER_MANAGER, TORRENT_MANAGER);
	}
	
	private static void startup() {
        UDP_TRACKER_CONNECTION_MANAGER.addTrackerListener(TRACKER_MANAGER);
        UDP_TRACKER_CONNECTION_MANAGER.manage("", UdpConnectionManager.UDP_TRACKER_PORT);
	}
	
	private static void cleanup() {
        UDP_TRACKER_CONNECTION_MANAGER.removeTrackerListener(TRACKER_MANAGER);

        if(DHT_CONNECTION_MANAGER != UDP_TRACKER_CONNECTION_MANAGER) {
            DHT_CONNECTION_MANAGER.unmanage();
        }

        //Stop all threaded resources and perform cleanup
        TRACKER_MANAGER.stop();

        UDP_TRACKER_CONNECTION_MANAGER.unmanage();
        TORRENT_MANAGER.storeState();
	}
}
