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

import java.util.Arrays;

/**
 * A peer to peer message that contains a payload
 * 
 * @author vedran
 *
 */
public final class PwpRegularMessage extends PwpMessage {
	
	private final byte[] payload;

	public PwpRegularMessage(final MessageType messageType, final byte[] payload) {
		super(messageType);
		//TODO: System.arrayCopy on payload
		this.payload = payload;
	}

	public final byte[] getPayload() {
		//TODO: System.arrayCopy on payload
		return payload;
	}

	@Override
	public String toString() {
		return "PwpRegularMessage [payload=" + Arrays.toString(payload)
				+ ", messageType=" + messageType + "]";
	}		
}