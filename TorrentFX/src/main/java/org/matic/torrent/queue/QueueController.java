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

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.TransferProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QueueController {
    private enum QueueStatus {ACTIVE, INACTIVE, QUEUED, FORCED}

    private final Map<QueueStatus, List<QueuedTorrent>> queueStatus = new HashMap<>();
    private final ObservableList<QueuedTorrent> torrents;

    private int maxActiveTorrents = (int) ApplicationPreferences.getProperty(
            TransferProperties.ACTIVE_TORRENTS_LIMIT, 5);
    private int maxDownloadingTorrents = (int)ApplicationPreferences.getProperty(
            TransferProperties.DOWNLOADING_TORRENTS_LIMIT, 3);
    private int maxUploadingTorrents = (int)ApplicationPreferences.getProperty(
            TransferProperties.UPLOADING_TORRENTS_LIMIT, 3);

    public QueueController(final ObservableList<QueuedTorrent> torrents) {
        this.torrents = torrents;

        handleTorrentsAdded(torrents);

        this.torrents.addListener((ListChangeListener<QueuedTorrent>) l ->  {
            if(l.next()) {
                return;
            }
            if(l.wasAdded()) {
                handleTorrentsAdded(l.getAddedSubList());
            }
            else if(l.wasRemoved()) {
                handleTorrentsRemoved(l.getRemoved());
            }
        });
    }

    public void onQueueLimitsChanged(final String limitName, final String newValue) {
        switch(limitName) {
            case TransferProperties.ACTIVE_TORRENTS_LIMIT:
                break;
            case TransferProperties.DOWNLOADING_TORRENTS_LIMIT:
                break;
            case TransferProperties.UPLOADING_TORRENTS_LIMIT:
                break;
        }
    }

    public boolean changeStatus(final QueuedTorrent torrent, final TorrentStatus newStatus) {
        final TorrentStatus currentStatus = torrent.getStatus();

        if(currentStatus == newStatus || (newStatus != TorrentStatus.ACTIVE &&
                newStatus != TorrentStatus.STOPPED)) {
            return false;
        }

        synchronized(torrents) {
            torrent.setStatus(newStatus);
        }
        return true;
    }

    private void handleTorrentsAdded(final List<?extends QueuedTorrent> addedTorrents) {
        synchronized(torrents) {
            addedTorrents.forEach(t -> {
                t.priorityProperty().addListener((obs, oldV, newV) ->
                        t.getProgress().setTorrentPriority(newV.intValue()));
                if(t.getStatus() != TorrentStatus.ACTIVE) {

                }
            });
        }
    }

    private void handleTorrentsRemoved(final List<?extends QueuedTorrent> removedTorrents) {
        synchronized(torrents) {

        }
    }
}