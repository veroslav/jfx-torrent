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

import java.util.function.Predicate;

/**
 * An interface for notifying implementing classes when a remote peer's state changes.
 *
 * @author Vedran Matic
 *
 */
public interface PwpConnectionStateListener {

    /**
     * Notify implementing classes when s peer's connection state has changed.
     *
     * @param event Event containing the information about the connection state change
     */
    void peerConnectionStateChanged(PeerConnectionStateChangeEvent event);

    /**
     * Allow the listener to only be notified when a certain event occurs.
     *
     * @return A filter that determines which events to notify the listeners about
     */
    Predicate<PeerConnectionStateChangeEvent> getPeerStateChangeAcceptanceFilter();
}