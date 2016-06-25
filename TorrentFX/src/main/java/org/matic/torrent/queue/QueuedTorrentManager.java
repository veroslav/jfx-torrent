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

import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataPersistenceSupport;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.beans.TrackerSessionView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class QueuedTorrentManager {

	private final List<QueuedTorrent> queuedTorrents = new ArrayList<>();
	private final TrackerManager trackerManager;
    private final DataPersistenceSupport persistenceSupport;
	
	public QueuedTorrentManager(final DataPersistenceSupport persistenceSupport, final TrackerManager trackerManager) {
        this.persistenceSupport = persistenceSupport;
        this.trackerManager = trackerManager;
	}

    /**
     * Allow the user to schedule a torrent's status change from the GUI.
     *
     * @param infoHash Target torrent's info hash.
     * @param requestedStatus Target status to change to.
     */
    public void requestStatusChange(final InfoHash infoHash, final TorrentStatus requestedStatus) {
        synchronized(queuedTorrents) {
            match(infoHash).ifPresent(torrent -> {
                final TorrentStatus currentStatus = torrent.getStatus();

                if(currentStatus == requestedStatus || (requestedStatus != TorrentStatus.ACTIVE &&
                        requestedStatus != TorrentStatus.STOPPED)) {
                    return;
                }

                torrent.setStatus(requestedStatus);
                trackerManager.issueTorrentEvent(torrent, requestedStatus == TorrentStatus.ACTIVE?
                        Tracker.Event.STARTED : Tracker.Event.STOPPED);
            });
        }
    }

    /**
     * Load and start managing previously stored torrents on the disk.
     *
     * @return Loaded torrents.
     */
    public List<TorrentView> loadPersisted() {
        final List<QueuedTorrent> loadedTorrents = persistenceSupport.loadAll().stream().map(
                t -> new QueuedTorrent(t.getMetaData(), t.getProgress())).collect(Collectors.toList());

        synchronized(queuedTorrents) {
            return loadedTorrents.stream().map(t -> add(t.getMetaData(), t.getProgress())).collect(Collectors.toList());
        }
    }

    /**
     * Store the torrents' progress and properties when shutting down the client.
     */
    public void storeState() {
        synchronized(queuedTorrents) {
            queuedTorrents.forEach(t -> {
                final QueuedTorrentProgress progress = t.getProgress();
                progress.setStatus(t.getStatus());
                persistenceSupport.store(t.getMetaData(), progress);
            });
        }
    }

	/**
	 * Add a torrent to be managed.
	 * 
	 * @param metaData Target torrent's metadata.
     * @param progress Target torrent's progress.
	 * @return A view of the newly created torrent.
	 */
	public TorrentView add(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
		synchronized(queuedTorrents) {
            final Optional<QueuedTorrent> torrent = match(metaData.getInfoHash());
            final Set<String> trackerUrls = progress.getTrackerUrls();
            final Set<TrackerSessionView> trackerSessionViews = new HashSet<>();
            final QueuedTorrent matchedTorrent;

            if (!torrent.isPresent()) {
                //New torrent, add it
                matchedTorrent = new QueuedTorrent(metaData, progress);

                if (!persistenceSupport.isPersisted(matchedTorrent.getInfoHash())) {
                    progress.setAddedOn(System.currentTimeMillis());
                    persistenceSupport.store(matchedTorrent.getMetaData(), matchedTorrent.getProgress());
                }

                queuedTorrents.add(matchedTorrent);
                trackerSessionViews.addAll(trackerUrls.stream().map(
                        t -> trackerManager.addTracker(t, matchedTorrent)).collect(Collectors.toSet()));
            } else {
                //Merge trackers, the torrent already exists
                matchedTorrent = torrent.get();
                final BinaryEncodedList announceList = matchedTorrent.getMetaData().getAnnounceList();
                final Set<BinaryEncodedString> newUrls = trackerUrls.stream().map(BinaryEncodedString::new).filter(
                        url -> !announceList.contains(url)).collect(Collectors.toSet());
                trackerSessionViews.addAll(newUrls.stream().map(url -> {
                    announceList.add(url);
                    return trackerManager.addTracker(url.getValue(), matchedTorrent);
                }).collect(Collectors.toSet()));
            }
            final TorrentView torrentView = new TorrentView(matchedTorrent);
            torrentView.addTrackerSessionViews(trackerSessionViews);
            return torrentView;
        }
	}

    /**
     * Add a new tracker to a torrent.
     *
     * @param url New tracker's url.
     * @param infoHash Target torrent's info hash.
     * @return A view to the added tracker.
     */
    public TrackerSessionView addTracker(final String url, final InfoHash infoHash) {
        synchronized(queuedTorrents) {
            final Optional<QueuedTorrent> torrent = match(infoHash);
            return torrent.isPresent()? trackerManager.addTracker(url, torrent.get()) : null;
        }
    }
	
	/**
	 * Remove and stop managing a torrent.
	 * 
	 * @param infoHash Info hash of the torrent to be removed.
	 * @return Whether the target torrent was successfully removed.
     * @throws IOException If the torrent can't be removed from the disk.
	 */
	public boolean remove(final InfoHash infoHash) throws IOException {
        synchronized (queuedTorrents) {
            final QueuedTorrent targetTorrent = queuedTorrents.stream().filter(
                    t -> t.getInfoHash().equals(infoHash)).findFirst().orElse(null);
            if(targetTorrent != null) {
                queuedTorrents.remove(targetTorrent);
                trackerManager.removeTorrent(targetTorrent);
                persistenceSupport.delete(targetTorrent.getInfoHash());
                return true;
            }
        }

        return false;
	}

    /**
     * Query whether a torrent is managed by this manager.
     *
     * @param infoHash Target torrent's info hash.
     * @return Whether a torrent is managed or not.
     */
    public boolean isManaging(final InfoHash infoHash) {
        return match(infoHash).isPresent();
    }

    private Optional<QueuedTorrent> match(final InfoHash infoHash) {
        synchronized(queuedTorrents) {
            return queuedTorrents.stream().filter(t -> t.getInfoHash().equals(infoHash)).findFirst();
        }
    }
}