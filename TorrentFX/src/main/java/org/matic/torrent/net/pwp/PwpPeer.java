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
 * A remote peer on a handshaken connection serving a unique torrent
 * 
 * @author vedran
 *
 */
public final class PwpPeer {
	
	private final String infoHash;
	private final String peerIp;	
	private final int peerPort;

	public PwpPeer(final String peerIp, final int peerPort, final String infoHash) {
		this.peerIp = peerIp;
		this.peerPort = peerPort;
		this.infoHash = infoHash;
	}
	
	public final String getInfoHash() {
		return infoHash;
	}

	public final String getPeerIp() {
		return peerIp;
	}

	public final int getPeerPort() {
		return peerPort;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((infoHash == null) ? 0 : infoHash.hashCode());
		result = prime * result + ((peerIp == null) ? 0 : peerIp.hashCode());
		result = prime * result + peerPort;
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwpPeer other = (PwpPeer) obj;
		if (infoHash == null) {
			if (other.infoHash != null)
				return false;
		} else if (!infoHash.equals(other.infoHash))
			return false;
		if (peerIp == null) {
			if (other.peerIp != null)
				return false;
		} else if (!peerIp.equals(other.peerIp))
			return false;
		if (peerPort != other.peerPort)
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "PwpPeer [infoHash=" + infoHash + ", peerIp=" + peerIp
				+ ", peerPort=" + peerPort + "]";
	}
}