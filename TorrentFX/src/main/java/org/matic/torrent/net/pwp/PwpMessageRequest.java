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
package org.matic.torrent.net.pwp;

import java.util.Collection;

import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.net.pwp.PwpMessage.MessageType;

/**
 * A request for a message to be sent on one of the peers' channels
 *
 * @author Vedran Matic
 *
 */
public final class PwpMessageRequest {

    private final MessageType messageType;
    private final Collection<PeerView> peers;
    private final byte[] data;

    /**
     * Create a new request with data to be sent on the specified connection
     *
     * @param data The message data to be sent
     * @param peers Peers to which to send the message, or null if the message should be sent to all handshaken peers
     */
    public PwpMessageRequest(final MessageType messageType, final byte[] data, final Collection<PeerView> peers) {
        this.messageType = messageType;
        this.data = data;
        this.peers = peers;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Collection<PeerView> getPeers() {
        return peers;
    }

    public byte[] getMessageData() {
        return data;
    }
}