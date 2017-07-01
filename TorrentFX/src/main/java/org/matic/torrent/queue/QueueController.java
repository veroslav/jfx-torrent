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

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class QueueController {

    private final Map<QueueType, List<QueuedTorrent>> queueStatus = new HashMap<>();
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

        Arrays.stream(QueueType.values()).forEach(qs -> queueStatus.put(qs, new ArrayList<>()));

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
                    final List<QueuedTorrent> activeQueue = queueStatus.get(QueueType.ACTIVE);
                    if(newQueueLimit < activeQueue.size()) {
                        while(activeQueue.size() > newQueueLimit) {
                            final QueuedTorrent torrentToQueue = activeQueue.remove(activeQueue.size() - 1);
                            insertIntoQueue(QueueType.QUEUED, torrentToQueue);
                        }
                    } else {
                        final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueType.QUEUED);
                        while(!queuedTorrents.isEmpty() && activeQueue.size() < newQueueLimit) {
                            final QueuedTorrent torrentToActivate = queuedTorrents.remove(0);
                            insertIntoQueue(QueueType.ACTIVE, torrentToActivate);
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
        final QueueType queue = torrent.getQueueType();

        if(newStatus == TorrentStatus.ACTIVE && queue == QueueType.INACTIVE) {
            return onStartTorrent(torrent);
        }
        else if(newStatus == TorrentStatus.STOPPED && queue != QueueType.INACTIVE) {
            return onStopTorrent(torrent);
        }

        return false;
    }

    private boolean onStartTorrent(final QueuedTorrent torrent) {
        synchronized(torrents) {
            if(queueStatus.get(QueueType.INACTIVE).remove(torrent)) {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueType.ACTIVE);

                if(activeTorrents.size() < maxActiveTorrents) {
                    insertIntoQueue(QueueType.ACTIVE, torrent);
                    return true;
                }

                final Optional<QueuedTorrent> lowerPrioActiveTorrent = activeTorrents.stream().filter(
                        t -> t.getPriority() > torrent.getPriority()).findFirst();

                if(lowerPrioActiveTorrent.isPresent()) {
                    //We found a currently active torrent with a lower priority, replace it
                    final QueuedTorrent activeTorrent = lowerPrioActiveTorrent.get();
                    activeTorrents.remove(activeTorrent);

                    insertIntoQueue(QueueType.QUEUED, activeTorrent);
                    insertIntoQueue(QueueType.ACTIVE, torrent);
                }
                else {
                    //All active torrents have higher priority, queue the torrent instead
                    insertIntoQueue(QueueType.QUEUED, torrent);
                }
                return true;
            }
            return false;
        }
    }

    private boolean onStopTorrent(final QueuedTorrent torrent) {
        synchronized(torrents) {
            switch(torrent.getQueueType()) {
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
        if(queueStatus.get(QueueType.FORCED).remove(torrent)) {
            torrent.setForced(false);
            insertIntoQueue(QueueType.INACTIVE, torrent);
            return true;
        }
        return false;
    }

    private boolean onStopQueuedTorrent(final QueuedTorrent torrent) {
        if(queueStatus.get(QueueType.QUEUED).remove(torrent)) {
            insertIntoQueue(QueueType.INACTIVE, torrent);
            return true;
        }
        return false;
    }

    private boolean onStopActiveTorrent(final QueuedTorrent torrent) {
        final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueType.ACTIVE);
        if(activeTorrents.remove(torrent)) {
            insertIntoQueue(QueueType.INACTIVE, torrent);

            final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueType.QUEUED);
            if(!queuedTorrents.isEmpty()) {
                final QueuedTorrent queuedTorrent = queuedTorrents.remove(0);
                insertIntoQueue(QueueType.ACTIVE, queuedTorrent);
            }
            return true;
        }
        return false;
    }

    /**
     * Get the size of one or more torrent queues.
     *
     * @param queues A set of queues to calculate sizes for
     * @return The sum of all queues' sizes
     */
    protected int getQueueSize(final Set<QueueType> queues) {
        synchronized(torrents) {
            return queues.stream().mapToInt(q -> queueStatus.get(q).size()).sum();
        }
    }

    private boolean handlePriorityRaised(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueType queue = torrent.getQueueType();
            final List<QueuedTorrent> matchingQueueTorrents = queueStatus.get(queue);
            final int torrentIndex = matchingQueueTorrents.indexOf(torrent);
            if(torrentIndex == -1 || torrent.getPriority() == 1) {
                return false;
            }
            torrent.setPriority(torrent.getPriority() - 1);
            final Optional<QueuedTorrent> matchingPrioTorrent = queueStatus.get(queue).stream().filter(
                    t -> !t.equals(torrent) && t.getPriority() == torrent.getPriority()).findAny();
            if(matchingPrioTorrent.isPresent()) {
                final QueuedTorrent torrentMatch = matchingPrioTorrent.get();
                torrentMatch.setPriority(torrent.getPriority() + 1);
                Collections.swap(matchingQueueTorrents, matchingQueueTorrents.indexOf(torrentMatch), torrentIndex);
                return true;
            }

            //Search all queues except torrent.getQueueType() for matching torrent prio and lower it
            queueStatus.entrySet().stream().filter(entry -> entry.getKey() != torrent.getQueueType()).flatMap(
                    entry -> entry.getValue().stream()).filter(
                    t -> t.getPriority() == torrent.getPriority()).findFirst().ifPresent(
                    t -> t.setPriority(t.getPriority() + 1));

            if(queue == QueueType.QUEUED) {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueType.ACTIVE);
                final QueuedTorrent lowestPrioActiveTorrent = activeTorrents.get(activeTorrents.size() - 1);

                if(lowestPrioActiveTorrent.getPriority() > torrent.getPriority()) {
                    //Remove both torrents from their respective queues
                    matchingQueueTorrents.remove(torrentIndex);
                    activeTorrents.remove(activeTorrents.size() - 1);

                    //Move lowest prio active torrent to queued torrents' queue
                    insertIntoQueue(QueueType.QUEUED, lowestPrioActiveTorrent);

                    //Move torrent with raised prio to active torrent's queue
                    insertIntoQueue(QueueType.ACTIVE, torrent);
                }
            }
            return true;
        }
    }

    private boolean handlePriorityLowered(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueType queue = torrent.getQueueType();
            final List<QueuedTorrent> matchingQueueTorrents = queueStatus.get(queue);
            final int torrentIndex = matchingQueueTorrents.indexOf(torrent);
            if(torrentIndex == -1 || torrent.getPriority() == queueStatus.values().size()) {
                return false;
            }
            torrent.setPriority(torrent.getPriority() + 1);
            final Optional<QueuedTorrent> matchingPrioTorrent = queueStatus.get(queue).stream().filter(
                    t -> !t.equals(torrent) && t.getPriority() == torrent.getPriority()).findAny();
            if(matchingPrioTorrent.isPresent()) {
                final QueuedTorrent torrentMatch = matchingPrioTorrent.get();
                torrentMatch.setPriority(torrent.getPriority() - 1);
                Collections.swap(matchingQueueTorrents, matchingQueueTorrents.indexOf(torrentMatch), torrentIndex);
                return true;
            }

            //Search all queues except torrent.getQueueType() for matching torrent prio and raise it
            queueStatus.entrySet().stream().filter(entry -> entry.getKey() != torrent.getQueueType()).flatMap(
                    entry -> entry.getValue().stream()).filter(
                    t -> t.getPriority() == torrent.getPriority()).findFirst().ifPresent(
                    t -> t.setPriority(t.getPriority() - 1));

            if(queue == QueueType.ACTIVE) {
                final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueType.QUEUED);
                if(!queuedTorrents.isEmpty()) {
                    final QueuedTorrent highestQueuedTorrent = queuedTorrents.get(0);

                    if(highestQueuedTorrent.getPriority() < torrent.getPriority()) {
                        //Remove both torrents from their respective queues
                        matchingQueueTorrents.remove(torrentIndex);
                        queuedTorrents.remove(0);

                        //Move highest prio queued torrent to active torrents' queue
                        insertIntoQueue(QueueType.ACTIVE, highestQueuedTorrent);

                        //Move torrent with lowered prio to queued torrent's queue
                        insertIntoQueue(QueueType.QUEUED, torrent);
                    }
                }
            }
            return true;
        }
    }

    private boolean handlePriorityForced(final QueuedTorrent torrent) {
        synchronized(torrents) {
            final QueueType queue = torrent.getQueueType();
            if(queue == QueueType.FORCED || queueStatus.get(queue).indexOf(torrent) == -1) {
                return false;
            }
            switch(queue) {
                case ACTIVE:
                    final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueType.ACTIVE);
                    activeTorrents.remove(torrent);
                    torrent.setForced(true);
                    insertIntoQueue(QueueType.FORCED, torrent);

                    final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueType.QUEUED);
                    if(!queuedTorrents.isEmpty()) {
                        final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                        insertIntoQueue(QueueType.ACTIVE, highestQueuedTorrent);
                    }
                    break;
                case INACTIVE:
                case QUEUED:
                    queueStatus.get(queue).remove(torrent);
                    torrent.setForced(true);
                    insertIntoQueue(QueueType.FORCED, torrent);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    private void handleInsertOnTopOfQueue(final QueuedTorrent torrent) {
        synchronized(torrents) {
            //Lower priorities for all other torrents
            queueStatus.entrySet().stream().flatMap(entry -> entry.getValue().stream()).forEach(
                    t -> t.setPriority(t.getPriority() + 1));

            final TorrentStatus torrentStatus = torrent.getStatus();
            torrent.setPriority(1);

            if(torrentStatus == TorrentStatus.ACTIVE) {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueType.ACTIVE);
                if(activeTorrents.size() == maxActiveTorrents) {
                    //Move lowest prio active torrent to queued torrents
                    final QueuedTorrent lowestPrioActiveTorrent = activeTorrents.remove(activeTorrents.size() - 1);
                    insertIntoQueue(QueueType.QUEUED, lowestPrioActiveTorrent);
                }

                //Move top prio torrent to active torrent's queue
                insertIntoQueue(QueueType.ACTIVE, torrent);
            }
            else {
                insertIntoQueue(QueueType.INACTIVE, torrent);
            }
        }
    }

    private void handleTorrentsAdded(final List<?extends QueuedTorrent> addedTorrents) {
        synchronized(torrents) {
            if(addedTorrents.size() == 1) {
                final QueuedTorrent torrent = addedTorrents.get(0);
                if(torrent.getProgress().getTorrentPriority() == QueuedTorrent.TOP_PRIORITY) {
                    handleInsertOnTopOfQueue(torrent);
                    return;
                }
            }
            addedTorrents.forEach(this::addTorrent);
        }
    }

    private void addTorrent(final QueuedTorrent torrent) {
        torrent.priorityProperty().addListener((obs, oldV, newV) ->
                torrent.getProgress().setTorrentPriority(newV.intValue()));

        final int torrentsInQueue = queueStatus.entrySet().stream().mapToInt(
                entry -> entry.getValue().size()).sum();
        torrent.setPriority(torrentsInQueue + 1);

        final QueueType queue = torrent.getQueueType();

        if(torrent.isForced()) {
            insertIntoQueue(QueueType.FORCED, torrent);
        } else if((queue == QueueType.NOT_ON_QUEUE && torrent.getStatus() == TorrentStatus.STOPPED)
                || queue == QueueType.INACTIVE) {
            //Inactive torrent
            insertIntoQueue(QueueType.INACTIVE, torrent);
        } else if(((queue == QueueType.NOT_ON_QUEUE && torrent.getStatus() == TorrentStatus.ACTIVE)
                || queue == QueueType.ACTIVE || queue == QueueType.QUEUED)
                && queueStatus.get(QueueType.ACTIVE).size() < maxActiveTorrents) {
            //Active torrent
            insertIntoQueue(QueueType.ACTIVE, torrent);
        } else {
            //Queue torrent
            insertIntoQueue(QueueType.QUEUED, torrent);
        }
    }

    private void handleTorrentsRemoved(final List<?extends QueuedTorrent> removedTorrents) {
        synchronized(torrents) {
            removedTorrents.forEach(this::removeTorrent);
        }
    }

    private void removeTorrent(final QueuedTorrent torrent) {
        if(!queueStatus.get(torrent.getQueueType()).remove(torrent)) {
            return;
        }
        torrent.setStatus(TorrentStatus.STOPPED);

        torrents.forEach(other -> {
            if(other.getPriority() > torrent.getPriority()) {
                other.setPriority(other.getPriority() - 1);
            }
        });

        if(torrent.getQueueType() == QueueType.ACTIVE) {
            final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueType.QUEUED);
            if (!queuedTorrents.isEmpty()) {
                final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                insertIntoQueue(QueueType.ACTIVE, highestQueuedTorrent);
            }
        }

        torrent.setPriority(QueuedTorrent.UNKNOWN_PRIORITY);
        torrent.setQueueType(QueueType.NOT_ON_QUEUE);
        torrent.setForced(false);
    }

    private void insertIntoQueue(final QueueType queue, final QueuedTorrent torrent) {
        final int insertionIndex = Collections.binarySearch(queueStatus.get(queue), torrent,
                Comparator.comparingInt(QueuedTorrent::getPriority));
        queueStatus.get(queue).add(insertionIndex < 0? -(insertionIndex) - 1 : insertionIndex, torrent);
        torrent.setQueueType(queue);
        switch(queue) {
            case ACTIVE:
            case FORCED:
                if(torrent.getStatus() != TorrentStatus.ACTIVE) {
                    torrent.setStatus(TorrentStatus.ACTIVE);
                }
                break;
            case QUEUED:
            case INACTIVE:
                if(torrent.getStatus() != TorrentStatus.STOPPED) {
                    torrent.setStatus(TorrentStatus.STOPPED);
                }
                break;
        }
    }
}