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

import java.util.Objects;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class QueuedTorrent {

    public static final int FORCED_PRIORITY = -1;
    public static final int UKNOWN_PRIORITY = 0;

    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;
    private final InfoHash infoHash;

    private final ObjectProperty<TorrentStatus> status = new SimpleObjectProperty<>();
    private final IntegerProperty priority;

    private QueueStatus queueStatus;

    public QueuedTorrent(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        this.metaData = metaData;
        this.progress = progress;
        this.infoHash = metaData.getInfoHash();

        this.status.set(progress.getStatus());
        this.queueStatus = progress.getQueueStatus();
        priority = new SimpleIntegerProperty(progress.getTorrentPriority());
    }

    public final InfoHash getInfoHash() {
        return infoHash;
    }

    public final QueuedTorrentMetaData getMetaData() {
        return metaData;
    }

    public final QueuedTorrentProgress getProgress() {
        return progress;
    }

    protected final void setQueueStatus(final QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    public final QueueStatus getQueueStatus() {
        return queueStatus;
    }

    protected final ObjectProperty<TorrentStatus> statusProperty() {
        return status;
    }

    protected final IntegerProperty priorityProperty() {
        return priority;
    }

    protected final int getPriority() {
        return priority.get();
    }

    protected final void setPriority(final int priority) {
        progress.setForced(priority == FORCED_PRIORITY);
        this.priority.set(priority);
    }

    public final TorrentStatus getStatus() {
        return status.get();
    }

    public final void setStatus(final TorrentStatus status) {
        this.status.set(status);
    }

    protected final boolean isForced() {
        return progress.isForced();
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueuedTorrent that = (QueuedTorrent) o;
        return Objects.equals(infoHash, that.infoHash);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(infoHash);
    }

    @Override
    public String toString() {
        return "QueuedTorrent{" +
                "infoHash=" + infoHash +
                ", queueStatus=" + queueStatus +
                ", priority=" + priority +
                '}';
    }
}