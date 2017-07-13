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
package org.matic.torrent.queue;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.action.TorrentPriorityChangeEvent;
import org.matic.torrent.queue.action.TorrentPriorityChangeListener;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.Objects;

public class QueuedTorrent {

    public static final int UNKNOWN_PRIORITY = 0;
    public static final int TOP_PRIORITY = -1;

    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;
    private final InfoHash infoHash;

    private final ObjectProperty<TorrentStatus> status = new SimpleObjectProperty<>();
    private final ObjectProperty<QueueType> queueType = new SimpleObjectProperty<>();
    private final IntegerProperty priority;

    private boolean isForced = false;

    public QueuedTorrent(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        this.metaData = metaData;
        this.progress = progress;
        this.infoHash = metaData.getInfoHash();

        this.status.set(progress.getQueueStatus() != QueueType.INACTIVE?
                TorrentStatus.ACTIVE : TorrentStatus.STOPPED);
        this.queueType.set(progress.getQueueStatus());
        priority = new SimpleIntegerProperty(progress.getTorrentPriority());
    }

    public void addPriorityChangeListener(final TorrentPriorityChangeListener handler) {
        priority.addListener((obs, oldV, newV) -> handler.onTorrentPriorityChanged(
                new TorrentPriorityChangeEvent(infoHash, oldV.intValue(), newV.intValue())));
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

    protected final void setQueueType(final QueueType queueType) {
        this.queueType.set(queueType);
    }

    public final QueueType getQueueType() {
        return queueType.get();
    }

    public final ObjectProperty<QueueType> queueTypeProperty() {
        return queueType;
    }

    public ObjectProperty<TorrentStatus> statusProperty() {
        return status;
    }

    protected final IntegerProperty priorityProperty() {
        return priority;
    }

    protected final int getPriority() {
        return priority.get();
    }

    protected final void setPriority(final int priority) {
        this.priority.set(priority);
    }

    public final TorrentStatus getStatus() {
        return status.get();
    }

    public final void setStatus(final TorrentStatus status) {
        this.status.set(status);
    }

    protected final boolean isForced() {
        return isForced;
    }

    protected final void setForced(final boolean forced) {
        isForced = forced;
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
                ", queueType=" + queueType +
                ", priority=" + priority +
                '}';
    }
}