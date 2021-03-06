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

package org.matic.torrent.tracking.listeners;

import org.matic.torrent.net.udp.UdpRequest;
import org.matic.torrent.tracking.methods.dht.DhtResponse;

/**
 * An interface for notifying implementing classes
 * when a response is received from a DHT network
 * 
 * @author vedran
 *
 */
public interface DhtResponseListener {

	/**
	 * Notify implementing classes when a DHT UDP packet is received
	 * 
	 * @param response Response containing DHT packet data
	 */
	void onDhtResponseReceived(final DhtResponse response);
	
	/**
	 * Notify implementing classes when an error occurs on sending an UDP request
	 * 
	 * @param request The originating UDP request
	 * @param message The error message
	 */
	void onDhtRequestError(final UdpRequest request, final String message);
}