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

/**
 * A notification that is sent to listeners when a new message has been received from a remote peer.
 *
 * @author Vedran Matic
 */
public final class PwpMessageEvent {

    private final PwpMessage message;
    private final PeerView peerView;

    /**
     * Create a new event instance.
     *
     * @param message The received message
     * @param peerView The remote peer that sent the message
     */
    public PwpMessageEvent(final PwpMessage message, final PeerView peerView) {
        this.message = message;
        this.peerView = peerView;
    }

    public PwpMessage getMessage() {
        return message;
    }

    public PeerView getPeerView() {
        return peerView;
    }
}