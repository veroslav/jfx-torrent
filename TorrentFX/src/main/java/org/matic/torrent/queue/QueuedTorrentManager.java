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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.stream.Collectors;

import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.gui.model.DhtView;
import org.matic.torrent.gui.model.LocalPeerDiscoveryView;
import org.matic.torrent.gui.model.PeerExchangeView;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.model.TrackableView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataPersistenceSupport;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.methods.dht.DhtSession;
import org.matic.torrent.tracking.methods.peerdiscovery.LocalPeerDiscoverySession;
import org.matic.torrent.tracking.methods.pex.PeerExchangeSession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class QueuedTorrentManager implements PreferenceChangeListener {

    private final ObservableList<QueuedTorrent> queuedTorrents = FXCollections.observableArrayList();
    private final QueueController queueController = new QueueController(queuedTorrents);
    private final TrackerManager trackerManager;
    private final DataPersistenceSupport persistenceSupport;

    public QueuedTorrentManager(final DataPersistenceSupport persistenceSupport, final TrackerManager trackerManager) {
        this.persistenceSupport = persistenceSupport;
        this.trackerManager = trackerManager;
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
    public void requestStatusChange(final TorrentView torrentView, final TorrentStatus requestedStatus) {
        synchronized(queuedTorrents) {
            match(torrentView.getInfoHash()).ifPresent(torrent ->
                    queueController.changeStatus(torrent, requestedStatus));
        }
    }

    /**
     * Load and start managing previously stored torrents on the disk.
     *
     * @return Loaded torrents.
     */
    public List<TorrentView> loadPersisted() {
        final List<TorrentTemplate> loadedTorrentTemplates = persistenceSupport.loadAll();

        synchronized(queuedTorrents) {
            return addTorrents(loadedTorrentTemplates);
        }
    }

    /**
     * Store the torrents' progress and properties when shutting down the client.
     */
    public void storeState() {
        synchronized(queuedTorrents) {
            queuedTorrents.forEach(t -> {
                final QueuedTorrentProgress progress = t.getProgress();
                final TorrentStatus status = t.getQueueStatus() == QueueStatus.INACTIVE?
                        TorrentStatus.STOPPED : TorrentStatus.ACTIVE;
                progress.setStatus(status);
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
        synchronized(queuedTorrents) {
            return torrentTemplates.stream().filter(template -> {
                //We are only interested in new torrents, filter out existing ones
                final Optional<QueuedTorrent> torrent = match(template.getMetaData().getInfoHash());
                return !torrent.isPresent();
            }).map(template -> {
                final QueuedTorrentMetaData metaData = template.getMetaData();
                final QueuedTorrentProgress progress = template.getProgress();
                final QueuedTorrent newTorrent = new QueuedTorrent(metaData, progress);

                if (!persistenceSupport.isPersisted(newTorrent.getInfoHash())) {
                    progress.setAddedOn(System.currentTimeMillis());
                    persistenceSupport.store(newTorrent.getMetaData(), newTorrent.getProgress());
                }

                final TorrentView torrentView = new TorrentView(newTorrent);
                newTorrent.statusProperty().addListener((obs, oldV, newV) -> onTorrentStatusChanged(torrentView, newV));
                queuedTorrents.add(newTorrent);

                final Set<String> trackerUrls = progress.getTrackerUrls();
                final Set<TrackableView> trackableViews = new LinkedHashSet<>();

                trackableViews.addAll(trackerUrls.stream().map(
                        t -> trackerManager.addTracker(t, torrentView)).collect(Collectors.toSet()));
                torrentView.priorityProperty().bind(newTorrent.priorityProperty());

                final TrackableView[] internalTrackables = {new DhtView(new DhtSession(torrentView)),
                        new LocalPeerDiscoveryView(new LocalPeerDiscoverySession(torrentView)),
                        new PeerExchangeView(new PeerExchangeSession(torrentView))};
                trackableViews.addAll(Arrays.asList(internalTrackables));
                torrentView.addTrackableViews(trackableViews);
                return torrentView;
            }).collect(Collectors.toList());
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

            final List<BinaryEncodedString> newUrls = trackerUrls.stream().map(BinaryEncodedString::new).filter(
                    url -> !announceList.contains(url)).collect(Collectors.toList());

            final Set<TrackerView> trackableViews = new LinkedHashSet<>();
            trackableViews.addAll(newUrls.stream().map(url -> {
                announceList.add(url);
                return trackerManager.addTracker(url.getValue(), torrentView);
            }).collect(Collectors.toSet()));

            return trackableViews;
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
                //TODO: Remove statusProperty listener (this) from this torrent
                queuedTorrents.remove(targetTorrent);
                trackerManager.removeTorrent(torrentView);
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

    private void onTorrentStatusChanged(final TorrentView torrentView, final TorrentStatus newStatus) {
        trackerManager.issueTorrentEvent(torrentView, newStatus == TorrentStatus.ACTIVE?
                Tracker.Event.STARTED : Tracker.Event.STOPPED);
    }
}