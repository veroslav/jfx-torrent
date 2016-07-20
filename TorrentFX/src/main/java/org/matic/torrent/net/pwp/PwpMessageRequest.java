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
package org.matic.torrent.net.pwp;

/**
 * A request for a message to be sent on one of the peers' channels
 * 
 * @author vedran
 * 
 */
public final class PwpMessageRequest {
	
	private final PwpPeer peer;
	private final byte[] data;

	/**
	 * Create a new request with data to be sent on the specified connection
	 * 
	 * @param data The message data to be sent
	 * @param peer Peer to which to send the message
	 */
	public PwpMessageRequest(final byte[] data, final PwpPeer peer) {
		this.data = data;
		this.peer = peer;
	}

	public final PwpPeer getPeer() {
        return peer;
    }

	public final byte[] getMessageData() {
		return data;
	}	
}