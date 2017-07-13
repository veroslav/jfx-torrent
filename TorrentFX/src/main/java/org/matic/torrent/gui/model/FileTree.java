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
package org.matic.torrent.gui.model;

import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.action.FilePriorityChangeEvent;
import org.matic.torrent.queue.action.FilePriorityChangeListener;
import org.matic.torrent.queue.enums.FilePriority;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FileTree {

    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;

    private final List<FilePriorityChangeListener> filePriorityChangeListeners = new CopyOnWriteArrayList<>();

    public FileTree(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        this.metaData = metaData;
        this.progress = progress;
    }

    public void addFilePriorityChangeListener(final FilePriorityChangeListener listener) {
        filePriorityChangeListeners.add(listener);
    }

    public void removeFilePriorityChangeListener(final FilePriorityChangeListener listener) {
        filePriorityChangeListeners.remove(listener);
    }

    public boolean isSingleFile() {
        return metaData.isSingleFile();
    }

    public long getLength() {
        return metaData.getTotalLength();
    }

    public long getPieceLength() {
        return metaData.getPieceLength();
    }

    public String getName() {
        return metaData.getName();
    }

    public List<QueuedFileMetaData> getFiles() {
        return metaData.getFiles();
    }

    public FilePriority getFilePriority(final Path filePath) {
        final FilePriority filePriority = progress.getFilePriority(filePath.toString());
        return filePriority != null? filePriority : FilePriority.NORMAL;
    }

    public void setFilePriority(final Path filePath, final FilePriority priority, final boolean isPriorityChange) {
        progress.setFilePriority(filePath.toString(), priority);

        if(isPriorityChange) {
            final FilePriorityChangeEvent changeEvent = new FilePriorityChangeEvent(filePath, priority);
            filePriorityChangeListeners.forEach(l -> l.filePriorityChanged(changeEvent));
        }
    }
}