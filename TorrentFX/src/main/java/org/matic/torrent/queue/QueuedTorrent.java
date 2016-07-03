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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class QueuedTorrent {

    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;
    private final InfoHash infoHash;

    private final ObjectProperty<TorrentStatus> status = new SimpleObjectProperty<>();
    private final IntegerProperty priority;

    private QueueStatus queueStatus = QueueStatus.INACTIVE;

    public QueuedTorrent(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        this.metaData = metaData;
        this.progress = progress;
        this.infoHash = metaData.getInfoHash();

        this.status.set(progress.getStatus());
        priority = new SimpleIntegerProperty(progress.getTorrentPriority());
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public QueuedTorrentMetaData getMetaData() {
        return metaData;
    }

    public QueuedTorrentProgress getProgress() {
        return progress;
    }

    protected void setQueueStatus(final QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

    protected ObjectProperty<TorrentStatus> statusProperty() {
        return status;
    }

    protected IntegerProperty priorityProperty() {
        return priority;
    }

    protected int getPriority() {
        return priority.get();
    }

    protected void setPriority(final int priority) {
        this.priority.set(priority);
    }

    public TorrentStatus getStatus() {
        return status.get();
    }

    protected void setStatus(final TorrentStatus status) {
        this.status.set(status);
    }

    protected boolean isForciblyQueued() {
        return progress.isForciblyQueued();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueuedTorrent that = (QueuedTorrent) o;
        return Objects.equals(infoHash, that.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(infoHash);
    }
}