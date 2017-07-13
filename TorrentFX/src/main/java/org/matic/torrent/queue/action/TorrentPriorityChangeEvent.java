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
package org.matic.torrent.queue.action;

import org.matic.torrent.hash.InfoHash;

/**
 * An event that is triggered when a torrent's priority has changed.
 *
 * @author Vedran Matic
 */
public final class TorrentPriorityChangeEvent {

    private final InfoHash infoHash;
    private final int newPriority;
    private final int oldPriority;

    public TorrentPriorityChangeEvent(final InfoHash infoHash,
                                    final int oldPriority, final int newPriority) {
        this.infoHash = infoHash;
        this.newPriority = newPriority;
        this.oldPriority = oldPriority;
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public int getNewPriority() {
        return newPriority;
    }

    public int getOldPriority() {
        return oldPriority;
    }
}