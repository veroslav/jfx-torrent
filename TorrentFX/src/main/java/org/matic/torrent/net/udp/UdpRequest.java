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

import java.net.InetAddress;

/**
 * A request to be sent to a remote peer or a tracker supporting UDP protocol 
 * 
 * @author vedran
 *
 */
public final class UdpRequest {
	
	private final InetAddress receiverAddress;
	private final byte[] requestData;
	private final int receiverPort;

	public UdpRequest(final byte[] requestData, final InetAddress receiverAddress,
			final int receiverPort) {
		this.requestData = new byte[requestData.length];
		System.arraycopy(requestData, 0, this.requestData, 0, requestData.length);
		
		this.receiverAddress = receiverAddress;
		this.receiverPort = receiverPort;
	}

	public final InetAddress getReceiverAddress() {
		return receiverAddress;
	}

	public final byte[] getRequestData() {
		return requestData;
	}

	public final int getReceiverPort() {
		return receiverPort;
	}	
}
