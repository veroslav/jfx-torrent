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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.gui.model.DhtView;
import org.matic.torrent.gui.model.LocalPeerDiscoveryView;
import org.matic.torrent.gui.model.PeerExchangeView;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.model.TrackableView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataPersistenceSupport;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.net.pwp.PeerConnectionController;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PwpConnectionStateListener;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.TransferProperties;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.methods.dht.DhtSession;
import org.matic.torrent.tracking.methods.peerdiscovery.LocalPeerDiscoverySession;
import org.matic.torrent.tracking.methods.pex.PeerExchangeSession;
import org.matic.torrent.transfer.TransferController;
import org.matic.torrent.transfer.TransferStatusChangeEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.stream.Collectors;

/**
 * This class manages the life cycles of all torrents, such as loading, storing, removing and adding.
 *
 * @author Vedran Matic
 */
public final class QueuedTorrentController implements PreferenceChangeListener, PwpConnectionStateListener {

    private static final int MAX_TRANSFER_EXECUTOR_POOL_SIZE = 30;

    private final ObservableList<QueuedTorrent> queuedTorrents = FXCollections.observableArrayList();
    private final Map<InfoHash, QueuedTorrentJob> queuedTorrentJobs = new HashMap<>();

    private final int maxActiveTorrents = (int) ApplicationPreferences.getProperty(
            TransferProperties.ACTIVE_TORRENTS_LIMIT, 5);
    private final int maxDownloadingTorrentsLimit = (int)ApplicationPreferences.getProperty(
            TransferProperties.DOWNLOADING_TORRENTS_LIMIT, 3);
    private int maxUploadingTorrents = (int)ApplicationPreferences.getProperty(
            TransferProperties.UPLOADING_TORRENTS_LIMIT, 3);

    private final ExecutorService transferExecutor = new ThreadPoolExecutor(maxActiveTorrents,
            MAX_TRANSFER_EXECUTOR_POOL_SIZE, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
            new ArrayBlockingQueue<>(MAX_TRANSFER_EXECUTOR_POOL_SIZE));
    private final Map<InfoHash, Future<?>> transferTasks = new HashMap<>();

    private final QueueController queueController = new QueueController(queuedTorrents, maxActiveTorrents,
            maxDownloadingTorrentsLimit, maxUploadingTorrents);
    private final TrackerManager trackerManager;
    private final DataPersistenceSupport persistenceSupport;
    private final PeerConnectionController connectionManager;

    //TODO: Read cache size from a property. Also, add a cache timeout property
    private final DataPieceCache pieceCache = new DataPieceCache(128 * 1048576);    //128 MB

    public QueuedTorrentController(final DataPersistenceSupport persistenceSupport,
                                   final TrackerManager trackerManager,
                                   final PeerConnectionController connectionManager) {
        this.persistenceSupport = persistenceSupport;
        this.trackerManager = trackerManager;
        this.connectionManager = connectionManager;
    }

    public int getTorrentsOnQueue() {
        synchronized(queuedTorrents) {
            return queueController.getQueueSize(EnumSet.of(QueueType.ACTIVE, QueueType.INACTIVE, QueueType.QUEUED));
        }
    }

    /**
     * @see {@link PwpConnectionStateListener#peerConnectionStateChanged(PeerConnectionStateChangeEvent)}
     */
    @Override
    public void peerConnectionStateChanged(final PeerConnectionStateChangeEvent event) {
        synchronized(queuedTorrents) {
            final PeerView peerView = event.getPeerView();
            final QueuedTorrentJob torrentJob = queuedTorrentJobs.get(peerView.getInfoHash());
            if(torrentJob == null) {
                return;
            }
            final TorrentView torrentView = torrentJob.getTorrentView();
            if(torrentView == null) {
                return;
            }
            final PeerConnectionStateChangeEvent.PeerLifeCycleChangeType eventType = event.getEventType();
            if(eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.CONNECTED) {
                torrentView.addPeerViews(Arrays.asList(peerView));
            }
            else if(eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED) {
                torrentView.getPeerViews().remove(peerView);
            }
        }
    }

    /**
     * @see {@link PwpConnectionStateListener#getPeerStateChangeAcceptanceFilter()}
     */
    @Override
    public Predicate<PeerConnectionStateChangeEvent> getPeerStateChangeAcceptanceFilter() {
        return event -> {
            final PeerConnectionStateChangeEvent.PeerLifeCycleChangeType eventType = event.getEventType();
            final InfoHash infoHash = event.getPeerView().getInfoHash();
            return infoHash != null && (eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.CONNECTED ||
                    eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED);
        };
    }

    /**
     * @see PreferenceChangeListener#preferenceChange(PreferenceChangeEvent)
     */
    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        final String eventKey = event.getKey();
        if(eventKey.startsWith("transfer.max.torrents")) {
            queueController.onQueueLimitsChanged(eventKey, Integer.parseInt(event.getNewValue()));
        }
    }

    /**
     * Allow the user to schedule a torrent's status change from the GUI.
     *
     * @param torrentView A view of the target torrent.
     * @param requestedStatus Target status to change to.
     */
    public void requestTorrentStatusChange(final TorrentView torrentView, final TorrentStatus requestedStatus) {
        synchronized(queuedTorrents) {
            match(torrentView.getInfoHash()).ifPresent(torrent ->
                    queueController.changeStatus(torrent, requestedStatus));
        }
    }

    /**
     * Allow the user to change priority for a torrent (either higher, lower or to forcibly activate a torrent).
     *
     * @param torrentView A view of the target torrent.
     * @param priorityChange Target priority to be applied.
     */
    public void requestTorrentPriorityChange(final TorrentView torrentView, final PriorityChange priorityChange) {
        synchronized(queuedTorrents) {
            match(torrentView.getInfoHash()).ifPresent(torrent ->
                    queueController.onPriorityChangeRequested(torrent, priorityChange));
        }
    }

    /**
     * Load and start managing previously stored torrents on the disk.
     *
     * @return Loaded torrents.
     */
    public List<TorrentView> loadPersisted() {
        final List<TorrentTemplate> loadedTorrentTemplates = persistenceSupport.loadAll();
        Collections.sort(loadedTorrentTemplates);

        synchronized(queuedTorrents) {
            return addTorrents(loadedTorrentTemplates);
        }
    }

    /**
     * Store the torrents' progress and properties when shutting down the client.
     */
    public void storeState() {
        transferExecutor.shutdownNow();
        try {
            transferExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ie) {
            System.err.println("Timeout while waiting for transfer controller to complete");
        }
        synchronized(queuedTorrents) {
            queuedTorrents.forEach(t -> {

                final QueuedTorrentJob torrentJob = queuedTorrentJobs.remove(t.getInfoHash());
                torrentJob.getTransferController().shutdown();

                final QueuedTorrentProgress progress = t.getProgress();
                final QueueType queueType = t.getQueueType();
                progress.setQueueType(queueType != QueueType.INACTIVE? QueueType.ACTIVE : QueueType.INACTIVE);
                progress.setTorrentPriority(t.getPriority());

                persistenceSupport.store(t.getMetaData(), progress);
            });
        }
    }

    /**
     * Add torrents to be managed.
     *
     * @param torrentTemplates Templates from which to add torrents.
     * @return A view collection to the newly created, unique torrents.
     */
    public List<TorrentView> addTorrents(final Collection<TorrentTemplate> torrentTemplates) {
        return torrentTemplates.stream().filter(template -> {
            //We are only interested in new torrents, filter out existing ones
            final Optional<QueuedTorrent> torrent = match(template.getMetaData().getInfoHash());
            return !torrent.isPresent();
        }).map(template -> {
            final QueuedTorrent newTorrent = new QueuedTorrent(template.getMetaData(), template.getProgress());
            persistTorrent(newTorrent);

            synchronized(queuedTorrents) {
                final TorrentView torrentView = buildTorrentView(newTorrent);

                newTorrent.statusProperty().addListener((obs, oldV, newV) ->
                        onTorrentStatusChanged(torrentView, oldV, newV));

                queuedTorrents.add(newTorrent);

                updateTransferControllerState(torrentView, TorrentStatus.STOPPED, newTorrent.getStatus());

                return torrentView;
            }
        }).collect(Collectors.toList());
    }

    private TorrentView buildTorrentView(final QueuedTorrent torrent) {
        final QueuedTorrentMetaData metaData = torrent.getMetaData();
        final QueuedTorrentProgress progress = torrent.getProgress();

        final TorrentView torrentView = new TorrentView(torrent);
        initTransferController(torrent, torrentView);
        connectionManager.accept(torrentView);

        final List<PwpPeer> storedPeers = progress.getPeers(true).stream().map(p -> {
            p.setInfoHash(metaData.getInfoHash());
            return p;
        }).collect(Collectors.toList());
        if (!storedPeers.isEmpty()) {
            connectionManager.onPeersFound(storedPeers, "queued_torrent_progress_data");
        }

        final Set<String> trackerUrls = progress.getTrackerUrls();
        final Set<TrackableView> trackableViews = new LinkedHashSet<>();

        final TrackableView[] internalTrackables = {new DhtView(new DhtSession(torrentView)),
                new LocalPeerDiscoveryView(new LocalPeerDiscoverySession(torrentView)),
                new PeerExchangeView(new PeerExchangeSession(torrentView))};

        trackableViews.addAll(Arrays.asList(internalTrackables));
        trackableViews.addAll(trackerUrls.stream().map(
                t -> trackerManager.addTracker(t, torrentView)).collect(Collectors.toSet()));

        torrentView.priorityProperty().bind(torrent.priorityProperty());
        torrentView.addTrackerViews(trackableViews);

        return torrentView;
    }

    private void initTransferController(final QueuedTorrent torrent, final TorrentView torrentView) {
        final TransferController transferController = new TransferController(torrentView, connectionManager, pieceCache);
        torrentView.getFileTree().addFilePriorityChangeListener(transferController);

        transferController.addStatusChangeListener(event -> {
            if(event.getEventType() == TransferStatusChangeEvent.EventType.ERROR) {
                queueController.changeStatus(torrent, TorrentStatus.ERROR);
            }
        });
        queuedTorrentJobs.put(torrentView.getInfoHash(),
                new QueuedTorrentJob(torrent, torrentView, transferController));
    }

    private void persistTorrent(final QueuedTorrent torrent) {
        if (!persistenceSupport.isPersisted(torrent.getInfoHash())) {
            torrent.getProgress().setAddedOn(System.currentTimeMillis());
            persistenceSupport.store(torrent.getMetaData(), torrent.getProgress());
        }
    }

    /**
     * Add new trackers to a torrent.
     *
     * @param trackerUrls New tracker URL:s.
     * @param torrentView View of the target torrent.
     * @return A view collection to the added, unique trackers.
     */
    public Set<TrackerView> addTrackers(final TorrentView torrentView, final Collection<String> trackerUrls) {
        synchronized(queuedTorrents) {
            final Optional<QueuedTorrent> matchedTorrent = match(torrentView.getInfoHash());

            if(!matchedTorrent.isPresent()) {
                return Collections.emptySet();
            }

            final QueuedTorrent torrent = matchedTorrent.get();
            final BinaryEncodedList announceList = torrent.getMetaData().getAnnounceList();

            final Set<String> newUrls = trackerUrls.stream().map(BinaryEncodedString::new).filter(
                    url -> !announceList.contains(url)).map(url -> url.getValue()).collect(Collectors.toSet());

            torrent.getProgress().addTrackerUrls(newUrls);
            final List<TrackerView> trackableViews = newUrls.stream().map(url ->
                    trackerManager.addTracker(url, torrentView)).collect(Collectors.toList());

            return new LinkedHashSet<>(trackableViews);
        }
    }

    public boolean removeTrackers(final List<TrackableView> trackerViews) {
        synchronized(queuedTorrents) {
            if(trackerViews.isEmpty()) {
                return false;
            }
            final InfoHash infoHash = trackerViews.get(0).getTorrentView().getInfoHash();

            final List<String> removedTrackers = trackerViews.stream().filter(tv -> trackerManager.removeTracker(
                    tv.getName(), tv.getTorrentView())).map(tv -> tv.getName()).collect(Collectors.toList());

            if(!removedTrackers.isEmpty()) {
                queuedTorrents.stream().filter(t -> t.getInfoHash().equals(infoHash)).forEach(
                        t -> t.getProgress().removeTrackerUrls(removedTrackers));
            }

            return !removedTrackers.isEmpty();
        }
    }

    /**
     * Remove and stop managing a torrent.
     *
     * @param torrentView A view of the torrent to be removed.
     * @return Whether the target torrent was successfully removed.
     * @throws IOException If the torrent can't be removed from the disk.
     */
    public boolean remove(final TorrentView torrentView) throws IOException {
        synchronized (queuedTorrents) {
            final QueuedTorrent targetTorrent = queuedTorrents.stream().filter(
                    t -> t.getInfoHash().equals(torrentView.getInfoHash())).findFirst().orElse(null);
            if(targetTorrent != null) {
                final InfoHash infoHash = targetTorrent.getInfoHash();
                final QueuedTorrentJob queuedTorrentJob = queuedTorrentJobs.remove(infoHash);

                final TransferController transferController = queuedTorrentJob.getTransferController();
                torrentView.getFileTree().removeFilePriorityChangeListener(transferController);

                final Future<?> transferTaskFuture = transferTasks.remove(infoHash);
                if(transferTaskFuture != null) {
                    transferTaskFuture.cancel(true);
                }

                transferController.shutdown();
                queuedTorrents.remove(targetTorrent);
                trackerManager.removeTorrent(torrentView);
                connectionManager.reject(torrentView);
                persistenceSupport.delete(targetTorrent.getInfoHash());
                return true;
            }
        }

        return false;
    }

    private Optional<QueuedTorrent> match(final InfoHash infoHash) {
        synchronized(queuedTorrents) {
            return queuedTorrents.stream().filter(t -> t.getInfoHash().equals(infoHash)).findFirst();
        }
    }

    private void onTorrentStatusChanged(final TorrentView torrentView,
                                        final TorrentStatus oldStatus, final TorrentStatus newStatus) {
        trackerManager.issueTorrentEvent(torrentView, newStatus == TorrentStatus.ACTIVE?
                Tracker.Event.STARTED : Tracker.Event.STOPPED);

        updateTransferControllerState(torrentView, oldStatus, newStatus);
    }

    private void updateTransferControllerState(final TorrentView torrentView,
                                               final TorrentStatus oldStatus, final TorrentStatus newStatus) {
        if(newStatus == TorrentStatus.ACTIVE && oldStatus != TorrentStatus.ACTIVE) {
            synchronized (queuedTorrents) {
                final InfoHash infoHash = torrentView.getInfoHash();
                final QueuedTorrentJob torrentJob = queuedTorrentJobs.get(infoHash);
                if(torrentJob != null) {
                    final Future<?> transferTask = transferExecutor.submit(torrentJob.getTransferController());
                    transferTasks.put(infoHash, transferTask);
                }
            }
        }
        else if(newStatus != TorrentStatus.ACTIVE && oldStatus == TorrentStatus.ACTIVE) {
            synchronized (queuedTorrents) {
                final InfoHash infoHash = torrentView.getInfoHash();
                final QueuedTorrentJob torrentJob = queuedTorrentJobs.get(infoHash);
                if(torrentJob != null) {
                    final Future<?> transferTask = transferTasks.remove(infoHash);
                    if(transferTask != null) {
                        transferTask.cancel(true);
                    }
                }
            }
        }
    }
}