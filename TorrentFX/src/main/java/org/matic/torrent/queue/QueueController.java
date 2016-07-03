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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QueueController {

    private final Map<QueueStatus, List<QueuedTorrent>> queueStatus = new HashMap<>();
    private final ObservableList<QueuedTorrent> torrents;

    private int maxActiveTorrents = (int) ApplicationPreferences.getProperty(
            TransferProperties.ACTIVE_TORRENTS_LIMIT, 5);
    private int maxDownloadingTorrents = (int)ApplicationPreferences.getProperty(
            TransferProperties.DOWNLOADING_TORRENTS_LIMIT, 3);
    private int maxUploadingTorrents = (int)ApplicationPreferences.getProperty(
            TransferProperties.UPLOADING_TORRENTS_LIMIT, 3);

    protected QueueController(final ObservableList<QueuedTorrent> torrents) {
        this.torrents = torrents;

        Arrays.stream(QueueStatus.values()).forEach(qs -> queueStatus.put(qs, new ArrayList<>()));

        handleTorrentsAdded(torrents);

        this.torrents.addListener((ListChangeListener<QueuedTorrent>) l ->  {
            if(!l.next()) {
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

    protected void onQueueLimitsChanged(final String limitName, final int newQueueLimit) {
        switch(limitName) {
            case TransferProperties.ACTIVE_TORRENTS_LIMIT:
                maxActiveTorrents = newQueueLimit;
                synchronized(torrents) {
                    final List<QueuedTorrent> activeQueue = queueStatus.get(QueueStatus.ACTIVE);
                    if(newQueueLimit < activeQueue.size()) {
                        while(activeQueue.size() > newQueueLimit) {
                            final QueuedTorrent torrentToInactivate = activeQueue.remove(activeQueue.size() - 1);
                            queueStatus.get(QueueStatus.INACTIVE).add(0, torrentToInactivate);
                            torrentToInactivate.setQueueStatus(QueueStatus.INACTIVE);
                            torrentToInactivate.setStatus(TorrentStatus.STOPPED);
                        }
                    }
                    else {
                        final List<QueuedTorrent> inactiveQueue = queueStatus.get(QueueStatus.INACTIVE);
                        while(!inactiveQueue.isEmpty() && activeQueue.size() < newQueueLimit) {
                            final QueuedTorrent torrentToActivate = inactiveQueue.remove(0);
                            queueStatus.get(QueueStatus.ACTIVE).add(torrentToActivate);
                            torrentToActivate.setQueueStatus(QueueStatus.ACTIVE);
                            torrentToActivate.setStatus(TorrentStatus.ACTIVE);
                        }
                    }
                }
                break;
            case TransferProperties.DOWNLOADING_TORRENTS_LIMIT:
                maxDownloadingTorrents = newQueueLimit;
                break;
            case TransferProperties.UPLOADING_TORRENTS_LIMIT:
                maxUploadingTorrents = newQueueLimit;
                break;
        }
    }

    protected boolean changeStatus(final QueuedTorrent torrent, final TorrentStatus newStatus) {
        final TorrentStatus currentStatus = torrent.getStatus();

        if(currentStatus == newStatus || (newStatus != TorrentStatus.ACTIVE &&
                newStatus != TorrentStatus.STOPPED)) {
            return false;
        }

        synchronized(torrents) {
            if(currentStatus == TorrentStatus.ACTIVE && newStatus == TorrentStatus.STOPPED) {
                if(!queueStatus.get(torrent.getQueueStatus()).remove(torrent)) {
                    return false;
                }
                queueStatus.get(QueueStatus.INACTIVE).add(0, torrent);
                torrent.setQueueStatus(QueueStatus.INACTIVE);
                torrent.setStatus(TorrentStatus.STOPPED);
            }
            else if(currentStatus == TorrentStatus.STOPPED && newStatus == TorrentStatus.ACTIVE) {
                if(!queueStatus.get(torrent.getQueueStatus()).remove(torrent)) {
                    return false;
                }
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
                if(activeTorrents.size() < maxActiveTorrents) {
                    activeTorrents.add(torrent);
                    torrent.setQueueStatus(QueueStatus.ACTIVE);
                    torrent.setStatus(TorrentStatus.ACTIVE);
                }
                else {
                    queueStatus.get(QueueStatus.QUEUED).add(torrent);
                    torrent.setQueueStatus(QueueStatus.QUEUED);
                }
            }

        }
        return true;
    }

    private void handleTorrentsAdded(final List<?extends QueuedTorrent> addedTorrents) {
        synchronized(torrents) {
            addedTorrents.forEach(t -> {

                //TODO: Remove listener when torrent is removed
                t.priorityProperty().addListener((obs, oldV, newV) ->
                        t.getProgress().setTorrentPriority(newV.intValue()));

                final int activeTorrents = queueStatus.get(QueueStatus.ACTIVE).size();
                final int inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE).size();
                final int queuedTorrents = queueStatus.get(QueueStatus.QUEUED).size();

                if(t.getStatus() != TorrentStatus.ACTIVE) {
                    t.setPriority(activeTorrents + inactiveTorrents + queuedTorrents + 1);
                    insertIntoQueue(QueueStatus.INACTIVE, t);
                }
                else {
                    if(queueStatus.containsKey(QueueStatus.ACTIVE) &&
                            queueStatus.get(QueueStatus.ACTIVE).size() < maxActiveTorrents) {
                        t.setPriority(activeTorrents + 1);
                        insertIntoQueue(QueueStatus.ACTIVE, t);
                    }
                    else if(t.isForciblyQueued()) {
                        t.setPriority(-1);
                        insertIntoQueue(QueueStatus.FORCED, t);
                    }
                    else {
                        t.setPriority(activeTorrents + queuedTorrents + 1);
                        insertIntoQueue(QueueStatus.QUEUED, t);
                        t.setStatus(TorrentStatus.STOPPED);
                    }
                }
            });
        }
    }

    private void handleTorrentsRemoved(final List<?extends QueuedTorrent> removedTorrents) {
        synchronized(torrents) {
            removedTorrents.forEach(t -> {
                final boolean torrentRemoved = queueStatus.get(t.getQueueStatus()).remove(t);
                if(torrentRemoved) {
                    t.setStatus(TorrentStatus.STOPPED);
                }
            });
        }
    }

    private void insertIntoQueue(final QueueStatus queue, final QueuedTorrent torrent) {
        queueStatus.get(queue).add(torrent);
        torrent.setQueueStatus(queue);
    }
}