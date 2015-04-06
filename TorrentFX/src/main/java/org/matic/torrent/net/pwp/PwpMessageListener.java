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
 * An interface for notifying implementing classes when a peer-wire-protocol message
 * is available for processing.
 * 
 * @author vedran
 *
 */
public interface PwpMessageListener {
	
	/* Available message interest masks */
	final int NOT_INTERESTED_MASK = 1;
	final int HANDSHAKE_MASK = 2;
	final int BITFIELD_MASK = 4;
	final int UNCHOKE_MASK = 8;
	final int REQUEST_MASK = 16;
	final int CANCEL_MASK = 32;
	final int CHOKE_MASK = 64;
	final int KEEP_ALIVE = 128;
	final int PIECE_MASK = 256;	
	final int HAVE_MASK = 512;		
	final int PORT_MASK = 1024;

	/**
	 * Notify implementing classes when a message they are interested in is available
	 * 
	 * @param message Received message
	 * @param connection Originating connection
	 */
	void messageReceived(final PwpMessage message, final SocketChannel connection);
	
	/**
	 * Return a mask of all messages the implementing class is interested in receiving
	 * 
	 * @return Message mask
	 */
	int getMessageInterestMask();
}