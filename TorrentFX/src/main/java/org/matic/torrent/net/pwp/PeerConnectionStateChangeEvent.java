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

import java.util.Objects;

/**
 * @author Vedran Matic
 */
public final class PeerConnectionStateChangeEvent {

    public enum PeerLifeCycleChangeType {
        CONNECTED, DISCONNECTED
    }

    private final PeerLifeCycleChangeType eventType;
    private final PeerView peerView;

    public PeerConnectionStateChangeEvent(final PeerView peerView, final PeerLifeCycleChangeType eventType) {
        this.peerView = peerView;
        this.eventType = eventType;
    }

    public PeerLifeCycleChangeType getEventType() {
        return eventType;
    }

    public PeerView getPeerView() {
        return peerView;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerConnectionStateChangeEvent that = (PeerConnectionStateChangeEvent) o;
        return eventType == that.eventType &&
                Objects.equals(peerView, that.peerView);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, peerView);
    }

    @Override
    public String toString() {
        return "PeerConnectionStateChangeEvent{" +
                "eventType=" + eventType +
                ", peerView=" + peerView +
                '}';
    }
}