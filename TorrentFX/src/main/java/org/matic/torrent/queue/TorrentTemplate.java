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
package org.matic.torrent.queue;

/**
 * A bean containing the data from which to queue a torrent.
 *
 * @author Vedran Matic
 */
public final class TorrentTemplate implements Comparable<TorrentTemplate> {

    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;

    public TorrentTemplate(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        this.metaData = metaData;
        this.progress = progress;
    }

    public QueuedTorrentMetaData getMetaData() {
        return metaData;
    }

    public QueuedTorrentProgress getProgress() {
        return progress;
    }

    @Override
    public int compareTo(final TorrentTemplate other) {
        final int thisPriority = progress.getTorrentPriority();
        final int otherPriority = other.getProgress().getTorrentPriority();

        if(thisPriority < otherPriority) {
            return -1;
        }
        else if(otherPriority < thisPriority) {
            return 1;
        }
        else {
            return 0;
        }
    }
}