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

package org.matic.torrent.net.udp;

import java.util.Set;

/**
 * An interface for notifying implementing classes when a UDP packet is
 * received from a remote peer or an UDP tracker
 * 
 * @author vedran
 *
 */
public interface UdpConnectionListener {

	/**
	 * Notify implementing class when a UDP packet is received
	 * 
	 * @param response Response containing UDP packet data
	 */
	void onUdpResponseReceived(final UdpResponse response);
	
	/**
	 * Target message types for which to notify listeners
	 * 
	 * @return Message types for which to notify listeners 
	 */
	Set<UdpResponse.Type> messageNotificationMask();
}