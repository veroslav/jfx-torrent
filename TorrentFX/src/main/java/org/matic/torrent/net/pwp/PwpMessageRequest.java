/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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

import org.matic.torrent.gui.model.PeerView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A request for a message to be sent on one of a peers' channels.
 *
 * @author Vedran Matic
 *
 */
public final class PwpMessageRequest {

    private final Collection<PeerView> peers;
    private final PwpMessage message;

    /**
     * Create a new request with data to be sent to specified peers.
     *
     * @param message The message to be sent
     * @param peers Peers to which to send the message
     */
    public PwpMessageRequest(final PwpMessage message, final Collection<PeerView> peers) {
        this.message = message;
        this.peers = peers;
    }

    /**
     * Create a new request with data to be sent to a peer.
     *
     * @param message The message to be sent
     * @param peer Peer to which to send the message
     */
    public PwpMessageRequest(final PwpMessage message, final PeerView peer) {
        this(message, Arrays.asList(peer));
    }

    /**
     * Create a new request with data to be sent to all handshaken peers.
     *
     * @param message The message to be sent
     */
    public PwpMessageRequest(final PwpMessage message) {
        this(message, Collections.emptyList());
    }

    public Collection<PeerView> getPeers() {
        return peers;
    }

    public PwpMessage getMessage() {
        return message;
    }
}