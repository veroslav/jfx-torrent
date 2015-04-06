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

/**
 * An initial (handshake) message sent between two peers
 * 
 * @author vedran
 *
 */
public final class PwpHandshakeMessage extends PwpMessage {
	
	private final byte[] reservedBytes;
	private final byte[] infoHash;
	private final byte[] peerId;
	
	public PwpHandshakeMessage(final byte[] reservedBytes, final byte[] infoHash, final byte[] peerId) {
		super(MessageType.HANDSHAKE);
		
		//TODO: System.arrayCopy on input byte[]
		this.reservedBytes = reservedBytes;
		this.infoHash = infoHash;
		this.peerId = peerId;
	}

	public final byte[] getReservedBytes() {
		return reservedBytes;
	}

	public final byte[] getInfoHash() {
		return infoHash;
	}

	public final byte[] getPeerId() {
		return peerId;
	}
}