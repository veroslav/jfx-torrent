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


/**
 * A request to be sent to a remote peer or a tracker supporting UDP protocol 
 * 
 * @author vedran
 *
 */
public final class UdpRequest {
	
	public enum Type {
		DHT, TRACKER
	}
	
	private final String receiverHost;
	private final byte[] requestData;
	private final int receiverPort;
	private final int id;
	
	private final Type type;

	public UdpRequest(final Type type, final int id, final byte[] requestData, final String receiverHost,
			final int receiverPort) {
		this.requestData = new byte[requestData.length];
		System.arraycopy(requestData, 0, this.requestData, 0, requestData.length);
		
		this.receiverPort = receiverPort;
		this.receiverHost = receiverHost;		
		this.type = type;
		this.id = id;
	}
	
	public final int getId() {
		return id;
	}
	
	public final Type getType() {
		return type;
	}

	public final String getReceiverHost() {
		return receiverHost;
	}

	public final byte[] getRequestData() {
		return requestData;
	}

	public final int getReceiverPort() {
		return receiverPort;
	}	
}
