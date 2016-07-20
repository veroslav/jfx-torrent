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
import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class QueueController {

    private final Map<QueueStatus, List<QueuedTorrent>> queueStatus = new HashMap<>();
    private final ObservableList<QueuedTorrent> torrents;

    private int maxActiveTorrents;
    private int maxDownloadingTorrents;
    private int maxUploadingTorrents;

    protected QueueController(final ObservableList<QueuedTorrent> torrents,
                              final int maxActiveTorrents, final int maxDownloadingTorrents,
                              final int maxUploadingTorrents) {
        this.torrents = torrents;
        this.maxActiveTorrents = maxActiveTorrents;
        this.maxDownloadingTorrents = maxDownloadingTorrents;
        this.maxUploadingTorrents = maxUploadingTorrents;

        Arrays.stream(QueueStatus.values()).forEach(qs -> queueStatus.put(qs, new ArrayList<>()));

        handleTorrentsAdded(torrents);

        this.torrents.addListener((ListChangeListener<QueuedTorrent>) l ->  {
            if(!l.next()) {
                return;
            }
            if(l.wasAdded()) {
                handleTorrentsAdded(l.getAddedSubList());
            } else if(l.wasRemoved()) {
                handleTorrentsRemoved(l.getRemoved());
            }
        });
    }

    protected boolean onPriorityChangeRequested(final QueuedTorrent torrent, final PriorityChange priorityChange) {
        switch(priorityChange) {
            case HIGHER:
                return handlePriorityRaised(torrent);
            case LOWER:
                return handlePriorityLowered(torrent);
            case FORCED:
                return handlePriorityForced(torrent);
            default:
                return false;
        }
    }

    protected void onQueueLimitsChanged(final String limitName, final int newQueueLimit) {
        switch(limitName) {
            case TransferProperties.ACTIVE_TORRENTS_LIMIT:
                maxActiveTorrents = newQueueLimit;
                synchronized(torrents) {
                    final List<QueuedTorrent> activeQueue = queueStatus.get(QueueStatus.ACTIVE);
                    if(newQueueLimit < activeQueue.size()) {
                        while(activeQueue.size() > newQueueLimit) {
                            final QueuedTorrent torrentToQueue = activeQueue.remove(activeQueue.size() - 1);
                            queueStatus.get(QueueStatus.QUEUED).add(0, torrentToQueue);
                            torrentToQueue.setQueueStatus(QueueStatus.QUEUED);
                            torrentToQueue.setStatus(TorrentStatus.STOPPED);
                        }
                    } else {
                        final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                        while(!queuedTorrents.isEmpty() && activeQueue.size() < newQueueLimit) {
                            final QueuedTorrent torrentToActivate = queuedTorrents.remove(0);
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
        final QueueStatus queue = torrent.getQueueStatus();

        if(newStatus == TorrentStatus.ACTIVE && queue == QueueStatus.INACTIVE) {
            return onStartTorrent(torrent);
        }
        else if(newStatus == TorrentStatus.STOPPED && queue != QueueStatus.INACTIVE) {
            return onStopTorrent(torrent);
        }

        return false;
    }

    private boolean onStartTorrent(final QueuedTorrent torrent) {
        synchronized(torrents) {
            if(queueStatus.get(QueueStatus.INACTIVE).remove(torrent)) {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);

                if(activeTorrents.size() < maxActiveTorrents) {
                    insertIntoQueue(QueueStatus.ACTIVE, torrent);
                    torrent.setStatus(TorrentStatus.ACTIVE);
                    return true;
                }

                final Optional<QueuedTorrent> lowerPrioActiveTorrent = activeTorrents.stream().filter(
                        t -> t.getPriority() > torrent.getPriority()).findFirst();

                if(lowerPrioActiveTorrent.isPresent()) {
                    //We found a currently active torrent with a lower priority, replace it
                    final QueuedTorrent activeTorrent = lowerPrioActiveTorrent.get();
                    final int insertionIndex = activeTorrents.indexOf(activeTorrent);

                    activeTorrents.remove(insertionIndex);
                    activeTorrent.setQueueStatus(QueueStatus.QUEUED);
                    activeTorrent.setStatus(TorrentStatus.STOPPED);

                    final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                    final Optional<QueuedTorrent> lowerPrioQueuedTorrent = queuedTorrents.stream().filter(
                            t -> t.getPriority() > activeTorrent.getPriority()).findFirst();

                    if(lowerPrioQueuedTorrent.isPresent()) {
                        queuedTorrents.add(queuedTorrents.indexOf(lowerPrioQueuedTorrent.get()), activeTorrent);
                    } else {
                        queuedTorrents.add(activeTorrent);
                    }

                    activeTorrents.add(insertionIndex, torrent);
                    torrent.setQueueStatus(QueueStatus.ACTIVE);
                    torrent.setStatus(TorrentStatus.ACTIVE);
                }
                else {
                    //All active torrents have higher priority, queue the torrent instead
                    final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                    final Optional<QueuedTorrent> lowerPrioQueuedTorrent = queuedTorrents.stream().filter(
                            t -> t.getPriority() > torrent.getPriority()).findFirst();

                    if(lowerPrioActiveTorrent.isPresent()) {
                        queuedTorrents.add(queuedTorrents.indexOf(lowerPrioQueuedTorrent.get()), torrent);
                    } else {
                        queuedTorrents.add(torrent);
                    }

                    torrent.setQueueStatus(QueueStatus.QUEUED);
                    torrent.setStatus(TorrentStatus.STOPPED);
                }
                return true;
            }
            return false;
        }
    }

    private boolean onStopTorrent(final QueuedTorrent torrent) {
        synchronized(torrents) {
            switch(torrent.getQueueStatus()) {
                case ACTIVE:
                    return onStopActiveTorrent(torrent);
                case QUEUED:
                    return onStopQueuedTorrent(torrent);
                case FORCED:
                    return onStopForcedTorrent(torrent);
                default:
                    return false;
            }
        }
    }

    private boolean onStopForcedTorrent(final QueuedTorrent torrent) {
        if(queueStatus.get(QueueStatus.FORCED).remove(torrent)) {
            torrent.setForced(false);
            torrent.setStatus(TorrentStatus.STOPPED);
            torrent.setQueueStatus(QueueStatus.INACTIVE);

            final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
            final Optional<QueuedTorrent> lowerPrioInactiveTorrent = inactiveTorrents.stream().filter(
                    t -> t.getPriority() > torrent.getPriority()).findFirst();

            if(lowerPrioInactiveTorrent.isPresent()) {
                inactiveTorrents.add(inactiveTorrents.indexOf(lowerPrioInactiveTorrent.get()), torrent);
            } else {
                inactiveTorrents.add(torrent);
            }

            return true;
        }
        return false;
    }

    private boolean onStopQueuedTorrent(final QueuedTorrent torrent) {
        if(queueStatus.get(QueueStatus.QUEUED).remove(torrent)) {
            torrent.setQueueStatus(QueueStatus.INACTIVE);
            final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
            final Optional<QueuedTorrent> lowerPrioInactiveTorrent = inactiveTorrents.stream().filter(
                    t -> t.getPriority() > torrent.getPriority()).findFirst();

            if(lowerPrioInactiveTorrent.isPresent()) {
                inactiveTorrents.add(inactiveTorrents.indexOf(lowerPrioInactiveTorrent.get()), torrent);
            } else {
                inactiveTorrents.add(torrent);
            }

            return true;
        }
        return false;
    }

    private boolean onStopActiveTorrent(final QueuedTorrent torrent) {
        final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
        if(activeTorrents.remove(torrent)) {
            torrent.setStatus(TorrentStatus.STOPPED);
            torrent.setQueueStatus(QueueStatus.INACTIVE);

            final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
            final Optional<QueuedTorrent> lowerPrioInactiveTorrent = inactiveTorrents.stream().filter(
                    t -> t.getPriority() > torrent.getPriority()).findFirst();

            if(lowerPrioInactiveTorrent.isPresent()) {
                inactiveTorrents.add(inactiveTorrents.indexOf(lowerPrioInactiveTorrent.get()), torrent);
            } else {
                inactiveTorrents.add(torrent);
            }

            final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
            if(!queuedTorrents.isEmpty()) {
                final QueuedTorrent queuedTorrent = queuedTorrents.remove(0);
                queuedTorrent.setQueueStatus(QueueStatus.ACTIVE);
                final Optional<QueuedTorrent> lowerPrioActiveTorrent = activeTorrents.stream().filter(
                        t -> t.getPriority() > torrent.getPriority()).findFirst();

                if(lowerPrioActiveTorrent.isPresent()) {
                    activeTorrents.add(activeTorrents.indexOf(lowerPrioActiveTorrent.get()), queuedTorrent);
                } else {
                    activeTorrents.add(queuedTorrent);
                }

                queuedTorrent.setStatus(TorrentStatus.ACTIVE);
            }
            return true;
        }
        return false;
    }

    protected int getQueueSize(final QueueStatus queue) {
        synchronized(torrents) {
            return queueStatus.get(queue).size();
        }
    }

    private boolean handlePriorityRaised(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueStatus queue = torrent.getQueueStatus();
            final List<QueuedTorrent> matchingQueueTorrents = queueStatus.get(queue);
            final int torrentIndex = matchingQueueTorrents.indexOf(torrent);
            if(torrentIndex == -1 || torrent.getPriority() == 1) {
                return false;
            }
            torrent.setPriority(torrent.getPriority() - 1);
            final Optional<QueuedTorrent> matchingPrioTorrent = torrents.stream().filter(
                    t -> !t.equals(torrent) && t.getPriority() == torrent.getPriority()).findAny();
            if(!matchingPrioTorrent.isPresent()) {
                return false;
            }
            matchingPrioTorrent.get().setPriority(torrent.getPriority() + 1);

            if(torrentIndex > 0) {
                final QueuedTorrent higherPrioTorrent = matchingQueueTorrents.get(torrentIndex - 1);
                if(higherPrioTorrent.getPriority() > torrent.getPriority()) {
                    Collections.swap(matchingQueueTorrents, torrentIndex - 1, torrentIndex);
                }
            } else if(queue == QueueStatus.QUEUED) {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
                final QueuedTorrent lowestActiveTorrent = activeTorrents.get(activeTorrents.size() - 1);

                if(lowestActiveTorrent.getPriority() > torrent.getPriority()) {
                    matchingQueueTorrents.remove(torrentIndex);
                    activeTorrents.remove(activeTorrents.size() - 1);
                    insertIntoQueue(QueueStatus.QUEUED, lowestActiveTorrent);
                    insertIntoQueue(QueueStatus.ACTIVE, torrent);
                    torrent.setStatus(TorrentStatus.ACTIVE);
                    lowestActiveTorrent.setStatus(TorrentStatus.STOPPED);
                }
            }
            return true;
        }
    }

    private boolean handlePriorityLowered(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueStatus queue = torrent.getQueueStatus();
            final List<QueuedTorrent> matchingQueueTorrents = queueStatus.get(queue);
            final int torrentIndex = matchingQueueTorrents.indexOf(torrent);
            final int torrentCount = queueStatus.values().size();
            if(torrentIndex == -1 || torrent.getPriority() == torrentCount) {
                return false;
            }
            torrent.setPriority(torrent.getPriority() + 1);
            final Optional<QueuedTorrent> matchingPrioTorrent = torrents.stream().filter(
                    t -> !t.equals(torrent) && t.getPriority() == torrent.getPriority()).findAny();
            if(!matchingPrioTorrent.isPresent()) {
                return false;
            }
            matchingPrioTorrent.get().setPriority(torrent.getPriority() - 1);

            if(torrentIndex < matchingQueueTorrents.size() - 1) {
                final QueuedTorrent lowerPrioTorrent = matchingQueueTorrents.get(torrentIndex + 1);
                if(lowerPrioTorrent.getPriority() < torrent.getPriority()) {
                    Collections.swap(matchingQueueTorrents, torrentIndex + 1, torrentIndex);
                }
            } else if(queue == QueueStatus.ACTIVE) {
                final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                if(!queuedTorrents.isEmpty()) {
                    final QueuedTorrent highestQueuedTorrent = queuedTorrents.get(0);

                    if(highestQueuedTorrent.getPriority() - 1 < torrent.getPriority()) {
                        matchingQueueTorrents.remove(torrentIndex);
                        queuedTorrents.remove(0);
                        insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                        insertIntoQueue(QueueStatus.QUEUED, torrent);
                        torrent.setStatus(TorrentStatus.STOPPED);
                        highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
                    }
                }
            }
            return true;
        }
    }

    private boolean handlePriorityForced(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueStatus queue = torrent.getQueueStatus();
            if(queue == QueueStatus.FORCED || queueStatus.get(queue).indexOf(torrent) == -1) {
                return false;
            }
            switch(queue) {
                case ACTIVE:
                    final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
                    activeTorrents.remove(torrent);
                    torrent.setForced(true);
                    insertIntoQueue(QueueStatus.FORCED, torrent);

                    final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                    if(!queuedTorrents.isEmpty()) {
                        final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                        insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                        highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
                    }
                    break;
                case INACTIVE:
                case QUEUED:
                    queueStatus.get(queue).remove(torrent);
                    torrent.setForced(true);
                    insertIntoQueue(QueueStatus.FORCED, torrent);
                    torrent.setStatus(TorrentStatus.ACTIVE);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    private void handleTorrentsAdded(final List<?extends QueuedTorrent> addedTorrents) {
        synchronized(torrents) {
            addedTorrents.forEach(t -> {
                t.priorityProperty().addListener((obs, oldV, newV) ->
                        t.getProgress().setTorrentPriority(newV.intValue()));

                final int activeTorrents = queueStatus.get(QueueStatus.ACTIVE).size();
                final int torrentsInQueue = activeTorrents +
                        queueStatus.get(QueueStatus.INACTIVE).size() +
                        queueStatus.get(QueueStatus.QUEUED).size() +
                        queueStatus.get(QueueStatus.FORCED).size();

                t.setPriority(torrentsInQueue + 1);

                final QueueStatus queue = t.getQueueStatus();

                if(t.isForced()) {
                    insertIntoQueue(QueueStatus.FORCED, t);
                    t.setStatus(TorrentStatus.ACTIVE);
                } else if((queue == QueueStatus.NOT_ON_QUEUE && t.getStatus() == TorrentStatus.STOPPED)
                        || queue == QueueStatus.INACTIVE) {
                    //Inactive torrent
                    insertIntoQueue(QueueStatus.INACTIVE, t);
                    t.setStatus(TorrentStatus.STOPPED);
                } else if(((queue == QueueStatus.NOT_ON_QUEUE && t.getStatus() == TorrentStatus.ACTIVE)
                        || queue == QueueStatus.ACTIVE || queue == QueueStatus.QUEUED)
                        && activeTorrents < maxActiveTorrents) {
                    //Active torrent
                    insertIntoQueue(QueueStatus.ACTIVE, t);
                    t.setStatus(TorrentStatus.ACTIVE);
                } else {
                    //Queue torrent
                    insertIntoQueue(QueueStatus.QUEUED, t);
                    t.setStatus(TorrentStatus.STOPPED);
                }
            });
        }
    }

    private void handleTorrentsRemoved(final List<?extends QueuedTorrent> removedTorrents) {
        synchronized(torrents) {
            removedTorrents.forEach(t -> {
                if(!queueStatus.get(t.getQueueStatus()).remove(t)) {
                    return;
                }
                t.setStatus(TorrentStatus.STOPPED);

                torrents.forEach(other -> {
                    if(other.getPriority() > t.getPriority()) {
                        other.setPriority(other.getPriority() - 1);
                    }
                });

                if(t.getQueueStatus() == QueueStatus.ACTIVE) {
                    final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                    if (!queuedTorrents.isEmpty()) {
                        final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                        insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                        highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
                    }
                }

                t.setPriority(QueuedTorrent.UNKNOWN_PRIORITY);
                t.setQueueStatus(QueueStatus.NOT_ON_QUEUE);
                t.setForced(false);
            });
        }
    }

    private void insertIntoQueue(final QueueStatus queue, final QueuedTorrent torrent) {
        queueStatus.get(queue).add(torrent);
        torrent.setQueueStatus(queue);
    }
}