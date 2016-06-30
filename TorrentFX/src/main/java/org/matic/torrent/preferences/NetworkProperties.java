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
package org.matic.torrent.preferences;

public final class NetworkProperties {

	public static final String NETWORK_INTERFACE_NAME = "network.interface.name";

    //Port properties
    public static final String ENABLE_UPNP_PORT_MAPPING = "network.upnp.port.mapping.enabled";
    public static final String RANDOMIZE_CONNECTION_PORT = "network.randomize.port";
    public static final String INCOMING_CONNECTION_PORT = "network.incoming.port";
	public static final String UDP_TRACKER_PORT = "network.udp.tracker.port";
	public static final String UDP_DHT_PORT = "network.udp.dht.port";

    public static final int DEFAULT_INCOMING_CONNECTION_PORT = 44893;
}