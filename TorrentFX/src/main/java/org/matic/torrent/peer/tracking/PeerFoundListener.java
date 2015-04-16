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

package org.matic.torrent.peer.tracking;

/**
 * An interface for notifying implementing classes when a new peer is
 * obtained by peer discovery strategies 
 * 
 * @author vedran
 *
 */
public interface PeerFoundListener {

	/**
	 * Notify implementing classes when a new peer is found
	 * 
	 * @param ip The remote IP address of the peer
	 * @param port The remote port of the peer
	 * @param torrent The torrent the found peer is serving
	 */
	void onPeerFound(final String ip, final long port, final TrackableTorrent torrent);
}
