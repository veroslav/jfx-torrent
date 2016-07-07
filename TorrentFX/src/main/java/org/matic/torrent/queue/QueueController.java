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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

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
        synchronized(torrents) {
            switch(torrent.getQueueStatus()) {
                case ACTIVE:
                    return handleActiveTorrentPriorityChange(torrent, priorityChange);
                case INACTIVE:
                    return handleInactiveTorrentPriorityChange(torrent, priorityChange);
                case QUEUED:
                    return handleQueuedTorrentPriorityChange(torrent, priorityChange);
                case FORCED:
                    return handleForcedTorrentPriorityChange(torrent, priorityChange);
                default:
                    return false;
            }
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
        final TorrentStatus currentStatus = torrent.getStatus();

        synchronized(torrents) {
            if(currentStatus == TorrentStatus.ACTIVE && newStatus == TorrentStatus.STOPPED) {
                return onStopActiveTorrent(torrent);
            } else if(currentStatus == TorrentStatus.STOPPED && newStatus == TorrentStatus.ACTIVE) {
                return onStartStoppedTorrent(torrent);
            } else if(currentStatus == TorrentStatus.STOPPED && newStatus == TorrentStatus.STOPPED &&
                    torrent.getQueueStatus() == QueueStatus.QUEUED) {
                return onStopQueuedTorrent(torrent);
            }
        }
        return false;
    }

    private boolean onStopQueuedTorrent(final QueuedTorrent torrent) {
        if(queueStatus.get(QueueStatus.QUEUED).remove(torrent)) {
            torrent.setQueueStatus(QueueStatus.INACTIVE);
            torrent.setStatus(TorrentStatus.STOPPED);

            final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
            final Optional<QueuedTorrent> lowerPrioInactiveTorrent = inactiveTorrents.stream().filter(
                    t -> t.getPriority() > torrent.getPriority()).findFirst();
            if(lowerPrioInactiveTorrent.isPresent()) {
                inactiveTorrents.add(inactiveTorrents.indexOf(lowerPrioInactiveTorrent.get()), torrent);
            }
            else {
                inactiveTorrents.add(torrent);
            }
            return true;
        }
        return false;
    }

    private boolean onStartStoppedTorrent(final QueuedTorrent torrent) {
        if(!queueStatus.get(QueueStatus.INACTIVE).remove(torrent)) {
            return false;
        }

        final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
        final Optional<QueuedTorrent> lowerPrioActiveTorrent = activeTorrents.stream().filter(
                t -> t.getPriority() > torrent.getPriority()).findFirst();

        if(activeTorrents.isEmpty() || (activeTorrents.size() < maxActiveTorrents)) {
            if(lowerPrioActiveTorrent.isPresent()) {
                activeTorrents.add(activeTorrents.indexOf(lowerPrioActiveTorrent.get()), torrent);
            }
            else {
                activeTorrents.add(torrent);
            }
            torrent.setQueueStatus(QueueStatus.ACTIVE);
            torrent.setStatus(TorrentStatus.ACTIVE);
        } else {
            final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
            if(lowerPrioActiveTorrent.isPresent()) {
                final QueuedTorrent lowestPrioActiveTorrent = activeTorrents.remove(activeTorrents.size() - 1);
                lowestPrioActiveTorrent.setQueueStatus(QueueStatus.QUEUED);
                lowestPrioActiveTorrent.setStatus(TorrentStatus.STOPPED);
                queuedTorrents.add(0, lowestPrioActiveTorrent);

                if(activeTorrents.isEmpty()) {
                    activeTorrents.add(torrent);
                }
                else {
                    final int insertionIndex = lowerPrioActiveTorrent.get().equals(lowestPrioActiveTorrent)?
                            activeTorrents.size() - 1 : activeTorrents.indexOf(lowerPrioActiveTorrent.get());
                    activeTorrents.add(insertionIndex, torrent);
                }

                torrent.setQueueStatus(QueueStatus.ACTIVE);
                torrent.setStatus(TorrentStatus.ACTIVE);
            } else {
                final Optional<QueuedTorrent> lowerPrioQueuedTorrent = queuedTorrents.stream().filter(
                        t -> t.getPriority() > torrent.getPriority()).findFirst();
                if(lowerPrioQueuedTorrent.isPresent()) {
                    queuedTorrents.add(queuedTorrents.indexOf(lowerPrioQueuedTorrent.get()), torrent);
                } else {
                    queuedTorrents.add(torrent);
                }
                torrent.setQueueStatus(QueueStatus.QUEUED);
            }
        }
        return true;
    }

    private boolean onStopActiveTorrent(final QueuedTorrent torrent) {
        final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
        final int torrentIndex = activeTorrents.indexOf(torrent);
        if(torrentIndex == -1) {
            return false;
        }

        final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
        final Optional<QueuedTorrent> lowerPrioInactiveTorrent = inactiveTorrents.stream().filter(
                t -> t.getPriority() > torrent.getPriority()).findFirst();
        if(lowerPrioInactiveTorrent.isPresent()) {
            inactiveTorrents.add(inactiveTorrents.indexOf(lowerPrioInactiveTorrent.get()), torrent);
        }
        else {
            inactiveTorrents.add(torrent);
        }

        activeTorrents.remove(torrentIndex);
        torrent.setQueueStatus(QueueStatus.INACTIVE);
        torrent.setStatus(TorrentStatus.STOPPED);

        final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
        if(!queuedTorrents.isEmpty()) {
            final QueuedTorrent queuedTorrent = queuedTorrents.remove(0);
            insertIntoQueue(QueueStatus.ACTIVE, queuedTorrent);
            queuedTorrent.setStatus(TorrentStatus.ACTIVE);
        }

        return true;
    }

    protected int getQueueSize(final QueueStatus queue) {
        synchronized(torrents) {
            return queueStatus.get(queue).size();
        }
    }

    private boolean handleForcedTorrentPriorityChange(final QueuedTorrent torrent,
                                                      final PriorityChange priorityChange) {
        final List<QueuedTorrent> forcedTorrents = queueStatus.get(QueueStatus.FORCED);
        final int torrentIndex = forcedTorrents.indexOf(torrent);
        if(torrentIndex == -1) {
            return false;
        }
        if(priorityChange == PriorityChange.HIGHER) {
            if(torrentIndex > 0) {
                Collections.swap(forcedTorrents, torrentIndex - 1, torrentIndex);
                return true;
            }
        } else if(priorityChange == PriorityChange.LOWER) {
            if(torrentIndex < forcedTorrents.size() - 1) {
                Collections.swap(forcedTorrents, torrentIndex + 1, torrentIndex);
                return true;
            }
        }
        return false;
    }

    private boolean handleInactiveTorrentPriorityChange(final QueuedTorrent torrent,
                                                        final PriorityChange priorityChange) {
        final List<QueuedTorrent> inactiveTorrents = queueStatus.get(QueueStatus.INACTIVE);
        final int torrentIndex = inactiveTorrents.indexOf(torrent);
        if(torrentIndex == -1) {
            return false;
        }
        if(priorityChange == PriorityChange.HIGHER) {
            if(torrentIndex > 0) {
                final QueuedTorrent higherPrioTorrent = inactiveTorrents.get(torrentIndex - 1);
                higherPrioTorrent.setPriority(higherPrioTorrent.getPriority() + 1);
                torrent.setPriority(torrent.getPriority() - 1);
                Collections.swap(inactiveTorrents, torrentIndex - 1, torrentIndex);
                return true;
            }
        } else if(priorityChange == PriorityChange.LOWER) {
            if(torrentIndex < inactiveTorrents.size() - 1) {
                final QueuedTorrent lowerPrioTorrent = inactiveTorrents.get(torrentIndex + 1);
                lowerPrioTorrent.setPriority(lowerPrioTorrent.getPriority() - 1);
                torrent.setPriority(torrent.getPriority() + 1);
                Collections.swap(inactiveTorrents, torrentIndex + 1, torrentIndex);
                return true;
            }
        } else {
            //Forced
            torrent.setPriority(QueuedTorrent.FORCED_PRIORITY);
            inactiveTorrents.stream().filter(t -> t.getPriority() > torrent.getPriority())
                    .forEach(t -> t.setPriority(t.getPriority() - 1));
            inactiveTorrents.remove(torrentIndex);
            insertIntoQueue(QueueStatus.FORCED, torrent);
            torrent.setStatus(TorrentStatus.ACTIVE);
            return true;
        }
        return false;
    }

    private boolean handleQueuedTorrentPriorityChange(final QueuedTorrent torrent,
                                                      final PriorityChange priorityChange) {
        final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
        final int torrentIndex = queuedTorrents.indexOf(torrent);
        if(torrentIndex == -1) {
            return false;
        }
        if(priorityChange == PriorityChange.HIGHER) {
            if(torrentIndex > 0) {
                final QueuedTorrent higherPrioTorrent = queuedTorrents.get(torrentIndex - 1);
                higherPrioTorrent.setPriority(higherPrioTorrent.getPriority() + 1);
                torrent.setPriority(torrent.getPriority() - 1);
                Collections.swap(queuedTorrents, torrentIndex - 1, torrentIndex);
                return true;
            } else {
                final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
                final QueuedTorrent lowestActiveTorrent = activeTorrents.remove(activeTorrents.size() - 1);
                queuedTorrents.remove(torrentIndex);
                insertIntoQueue(QueueStatus.QUEUED, lowestActiveTorrent);
                insertIntoQueue(QueueStatus.ACTIVE, torrent);
                torrent.setPriority(torrent.getPriority() - 1);
                torrent.setStatus(TorrentStatus.ACTIVE);
                lowestActiveTorrent.setPriority(lowestActiveTorrent.getPriority() + 1);
                lowestActiveTorrent.setStatus(TorrentStatus.STOPPED);
                return true;
            }
        } else if(priorityChange == PriorityChange.LOWER) {
            if(!queuedTorrents.isEmpty() && torrentIndex < queuedTorrents.size() -1) {
                final QueuedTorrent lowerPrioTorrent = queuedTorrents.get(torrentIndex + 1);
                lowerPrioTorrent.setPriority(lowerPrioTorrent.getPriority() - 1);
                torrent.setPriority(torrent.getPriority() + 1);
                Collections.swap(queuedTorrents, torrentIndex + 1, torrentIndex);
                return true;
            }
        } else {
            //Forced
            torrent.setPriority(QueuedTorrent.FORCED_PRIORITY);
            queuedTorrents.stream().filter(t -> t.getPriority() > torrent.getPriority())
                    .forEach(t -> t.setPriority(t.getPriority() - 1));
            queuedTorrents.remove(torrentIndex);
            insertIntoQueue(QueueStatus.FORCED, torrent);
            torrent.setStatus(TorrentStatus.ACTIVE);
            return true;
        }
        return false;
    }

    private boolean handleActiveTorrentPriorityChange(final QueuedTorrent torrent,
                                                      final PriorityChange priorityChange) {
        final List<QueuedTorrent> activeTorrents = queueStatus.get(QueueStatus.ACTIVE);
        final int torrentIndex = activeTorrents.indexOf(torrent);
        if(torrentIndex == -1) {
            return false;
        }
        if(priorityChange == PriorityChange.HIGHER) {
            if(torrentIndex > 0) {
                final QueuedTorrent higherPrioTorrent = activeTorrents.get(torrentIndex - 1);
                higherPrioTorrent.setPriority(higherPrioTorrent.getPriority() + 1);
                torrent.setPriority(torrent.getPriority() - 1);
                Collections.swap(activeTorrents, torrentIndex - 1, torrentIndex);
                return true;
            }
        } else if(priorityChange == PriorityChange.LOWER) {
            if(torrentIndex < activeTorrents.size() - 1) {
                final QueuedTorrent lowerPrioTorrent = activeTorrents.get(torrentIndex + 1);
                lowerPrioTorrent.setPriority(lowerPrioTorrent.getPriority() - 1);
                torrent.setPriority(torrent.getPriority() + 1);
                Collections.swap(activeTorrents, torrentIndex + 1, torrentIndex);
                return true;
            } else {
                final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                if(!queuedTorrents.isEmpty()) {
                    final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                    activeTorrents.remove(torrentIndex);
                    insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                    insertIntoQueue(QueueStatus.QUEUED, torrent);
                    torrent.setPriority(torrent.getPriority() + 1);
                    torrent.setStatus(TorrentStatus.STOPPED);
                    highestQueuedTorrent.setPriority(highestQueuedTorrent.getPriority() - 1);
                    highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
                }
            }
        } else {
            //Forced
            for(int i = torrentIndex + 1; i < activeTorrents.size(); ++i) {
                final QueuedTorrent lowerPrioTorrent = activeTorrents.get(i);
                lowerPrioTorrent.setPriority(lowerPrioTorrent.getPriority() - 1);
            }
            activeTorrents.stream().filter(t -> t.getPriority() > torrent.getPriority())
                    .forEach(t -> t.setPriority(t.getPriority() - 1));
            activeTorrents.remove(torrentIndex);
            insertIntoQueue(QueueStatus.FORCED, torrent);
            torrent.setPriority(QueuedTorrent.FORCED_PRIORITY);

            final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
            if(!queuedTorrents.isEmpty()) {
                final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                highestQueuedTorrent.setPriority(highestQueuedTorrent.getPriority() - 1);
                highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
            }

            queuedTorrents.forEach(t -> t.setPriority(t.getPriority() - 1));
            queueStatus.get(QueueStatus.INACTIVE).forEach(t -> t.setPriority(t.getPriority() - 1));
        }
        return false;
    }

    private void handleTorrentsAdded(final List<?extends QueuedTorrent> addedTorrents) {
        synchronized(torrents) {
            addedTorrents.forEach(t -> {
                t.priorityProperty().addListener((obs, oldV, newV) ->
                        t.getProgress().setTorrentPriority(newV.intValue()));

                final int forcedTorrents = queueStatus.get(QueueStatus.FORCED).size();
                final int torrentsInQueue = queueStatus.get(QueueStatus.ACTIVE).size() +
                        queueStatus.get(QueueStatus.INACTIVE).size() +
                        queueStatus.get(QueueStatus.QUEUED).size() + forcedTorrents;

                final int priority = t.getPriority() != QueuedTorrent.UKNOWN_PRIORITY?
                        t.getPriority() : torrentsInQueue - forcedTorrents + 1;

                if(t.getStatus() != TorrentStatus.ACTIVE) {
                    t.setPriority(priority);
                    insertIntoQueue(t.getQueueStatus() != QueueStatus.NOT_ON_QUEUE? t.getQueueStatus() : QueueStatus.INACTIVE, t);
                } else {
                    if(t.isForced()) {
                        t.setPriority(QueuedTorrent.FORCED_PRIORITY);
                        insertIntoQueue(QueueStatus.FORCED, t);
                    } else if(queueStatus.get(QueueStatus.ACTIVE).size() < maxActiveTorrents) {
                        t.setPriority(priority);
                        insertIntoQueue(QueueStatus.ACTIVE, t);
                    } else {
                        t.setPriority(priority);
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
                if(queueStatus.get(t.getQueueStatus()).indexOf(t) == -1) {
                    return;
                }
                updatePrioritiesOnTorrentRemoval(t);

                final List<QueuedTorrent> queuedTorrents = queueStatus.get(QueueStatus.QUEUED);
                if(!queuedTorrents.isEmpty()) {
                    final QueuedTorrent highestQueuedTorrent = queuedTorrents.remove(0);
                    insertIntoQueue(QueueStatus.ACTIVE, highestQueuedTorrent);
                    highestQueuedTorrent.setStatus(TorrentStatus.ACTIVE);
                }
            });
        }
    }

    private void updatePrioritiesOnTorrentRemoval(final QueuedTorrent removedTorrent) {
        final QueueStatus queue = removedTorrent.getQueueStatus();
        if(queue != QueueStatus.FORCED) {
            final Consumer<QueuedTorrent> priorityUpdater = t -> {
                final int priority = t.getPriority();
                if(priority > removedTorrent.getPriority()) {
                    t.setPriority(priority - 1);
                }
            };
            queueStatus.get(QueueStatus.ACTIVE).forEach(priorityUpdater::accept);
            queueStatus.get(QueueStatus.QUEUED).forEach(priorityUpdater::accept);
            queueStatus.get(QueueStatus.INACTIVE).forEach(priorityUpdater::accept);
        }

        removeFromQueue(removedTorrent);
        queueStatus.get(queue).remove(removedTorrent);
    }

    private void removeFromQueue(final QueuedTorrent torrent) {
        torrent.setStatus(TorrentStatus.STOPPED);
        torrent.setPriority(QueuedTorrent.UKNOWN_PRIORITY);
        torrent.setQueueStatus(QueueStatus.NOT_ON_QUEUE);
    }

    private void insertIntoQueue(final QueueStatus queue, final QueuedTorrent torrent) {
        queueStatus.get(queue).add(torrent);
        torrent.setQueueStatus(queue);
    }
}