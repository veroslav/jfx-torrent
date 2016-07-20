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
package org.matic.torrent.tracking.listeners;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.pwp.PwpPeer;

import java.util.Set;

/**
 * An interface for notifying implementing classes when a new peer is
 * obtained by peer discovery strategies 
 * 
 * @author vedran
 *
 */
public interface PeerFoundListener {

    /**
     * Notify implementing classes when new peers are found
     *
     * @param peers Newly obtained set of peers
     * @param infoHash Target torrent's info hash
     * @param source Name of the source that found the peers
     */
    void onPeersFound(Set<PwpPeer> peers, InfoHash infoHash, String source);
}
