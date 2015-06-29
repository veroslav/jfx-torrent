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

import java.net.SocketAddress;
import java.util.Arrays;

/**
 * A response received from a remote peer or a tracker supporting UDP protocol
 * 
 * @author vedran
 *
 */
public final class UdpResponse {
	
	public enum Type {
		CONNECTION_ACCEPTED, ANNOUNCE, UNKNOWN
	}
	
	private final SocketAddress senderAddress;
	private final byte[] data;
	private final Type type;

	public UdpResponse(final byte[] data, final Type type, final SocketAddress senderAddress) {		
		this.data = Arrays.copyOf(data, data.length);
		this.senderAddress = senderAddress;
		this.type = type;
	}
	
	public final SocketAddress getSenderAddress() {
		return senderAddress;
	}
	
	public final Type getType() {
		return type;
	}
	
	public final byte[] getData() {
		return data;
	}
}