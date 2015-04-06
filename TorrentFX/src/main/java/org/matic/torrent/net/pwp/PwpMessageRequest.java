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

package org.matic.torrent.net.pwp;

import java.nio.channels.SocketChannel;

/**
 * A request for a message to be sent on one of the peers' channels
 * 
 * @author vedran
 * 
 */
public final class PwpMessageRequest {
	
	private final SocketChannel connection;
	private final byte[] messageData;

	/**
	 * Create a new request with data to be sent on the specified connection
	 * 
	 * @param messageData Bytes containing the message to be sent
	 * @param connection Connection to use for sending the message
	 */
	public PwpMessageRequest(final byte[] messageData, final SocketChannel connection) {
		this.messageData = messageData;
		this.connection = connection;
	}

	public final SocketChannel getConnection() {
		return connection;
	}

	public final byte[] getMessageData() {
		return messageData;
	}	
}