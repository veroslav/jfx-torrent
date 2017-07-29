/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
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
package org.matic.torrent.preferences;

public final class BitTorrentProperties {

	//Basic BitTorrent properties
	public static final String LOCAL_PEER_DISCOVERY_ENABLED = "bt.local.peer.discovery.enabled";
	public static final boolean DEFAULT_LOCAL_PEER_DISCOVERY_ENABLED = true;
	
	public static final String DHT_NETWORK_ENABLED = "bt.dht.enabled";
	public static final boolean DEFAULT_DHT_NETWORK_ENABLED = true;
	
	public static final String DHT_NETWORK_NEW_TORRENTS_ENABLED = "bt.dht.new.torrents.enabled";
	public static final boolean DEFAULT_DHT_NETWORK_NEW_TORRENTS_ENABLED = true;
	
	public static final String PEER_EXCHANGE_ENABLED = "bt.pex.enabled";
	public static final boolean DEFAULT_PEER_EXCHANGE_ENABLED = true;
	
	public static final String WEB_SEEDS_ENABLED = "bt.web.seeds.enabled";
	public static final boolean DEFAULT_WEB_SEEDS_ENABLED = false;
	
	//Tracker properties
	public static final String REPORT_TRACKER_IP_ENABLED = "bt.tracker.ip.report.enabled";
	public static final boolean DEFAULT_REPORT_TRACKER_IP_ENABLED = false;
	public static final String TRACKER_IP = "bt.tracker.ip";
	
	public static final String TRACKER_SCRAPE_ENABLED = "bt.tracker.scrape.enabled";
	public static final boolean DEFAULT_TRACKER_SCRAPE_ENABLED = true;
	
	public static final String UDP_TRACKER_ENABLED = "bt.tracker.udp.enabled";
	public static final boolean DEFAULT_UDP_TRACKER_ENABLED = true;
	
	//Encryption properties
	public static final String ALLOW_LEGACY_CONNECTIONS = "bt.connection.allow.legacy";
	public static final boolean DEFAULT_ALLOW_LEGACY_CONNECTIONS = true;
	
	public static final String ANONYMOUS_MODE_ENABLED = "bt.connection.anonimous.enabled";
	public static final boolean DEFAULT_ANONYMOUS_MODE_ENABLED = false;
	
	public static final String ENCRYPTION_MODE = "bt.encryption.mode";
}