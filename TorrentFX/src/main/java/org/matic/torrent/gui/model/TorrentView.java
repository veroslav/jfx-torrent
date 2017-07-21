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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.action.TorrentPriorityChangeListener;
import org.matic.torrent.queue.action.TorrentStatusChangeEvent;
import org.matic.torrent.queue.action.TorrentStatusChangeListener;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A snapshot of a torrent state to be shown in the GUI.
 *
 * @author Vedran Matic
 */
public final class TorrentView {

    private final BitsView availabilityView;
    private final QueuedTorrent queuedTorrent;

    private final LongProperty selectedLength;
    private final IntegerProperty priority;

    private final StringProperty lifeCycleChange = new SimpleStringProperty();

    private final IntegerProperty hashFailures = new SimpleIntegerProperty(0);
    private final LongProperty downloadedBytes = new SimpleLongProperty(0);
    private final LongProperty wastedBytes = new SimpleLongProperty();

    private long elapsedTime;
    private long downloadSpeed;
    private long downloadLimit;

    private long remainingBytes;
    private long uploadedBytes;
    private long uploadSpeed;
    private long uploadLimit;

    private int seedsConnected;
    private int seedsAvailable;
    private int seedsInSwarm;

    private int peersConnected;
    private int peersAvailable;
    private int peersInSwarm;

    private String shareRatio;
    private String saveDirectory;

    private long completionTime;

    private final ObservableList<TrackableView> trackerViews = FXCollections.observableArrayList();
    private final Set<PeerView> peerViews = new LinkedHashSet<>();

    private final List<TorrentPriorityChangeListener> priorityChangeListeners = new CopyOnWriteArrayList<>();
    private final List<TorrentStatusChangeListener> statusChangeListeners = new CopyOnWriteArrayList<>();

    private final FileTree fileTree;

    public TorrentView(final QueuedTorrent queuedTorrent) {
        this.priority = new SimpleIntegerProperty(0);
        this.selectedLength = new SimpleLongProperty(0);
        this.queuedTorrent = queuedTorrent;

        availabilityView = new BitsView(this.queuedTorrent.getMetaData().getTotalPieces());
        fileTree = new FileTree(queuedTorrent.getMetaData(), queuedTorrent.getProgress());

        this.priority.addListener((obs, oldV, newV) ->
                lifeCycleChange.setValue(String.valueOf(newV.intValue())));

        queuedTorrent.statusProperty().addListener((obs, oldV, newV) -> {
            final TorrentStatusChangeEvent statusChangeEvent = new TorrentStatusChangeEvent(this, oldV, newV);
            statusChangeListeners.forEach(l -> l.onTorrentStatusChanged(statusChangeEvent));
        });

        queuedTorrent.addPriorityChangeListener(event ->
                priorityChangeListeners.forEach(l -> l.onTorrentPriorityChanged(event)));

        this.saveDirectory = queuedTorrent.getProgress().getSavePath().toString();
    }

    public boolean addPeerViews(final Collection<PeerView> peerViews) {
        return this.peerViews.addAll(peerViews);
    }

    public Collection<PeerView> getPeerViews() {
        return peerViews;
    }

    public void addTrackerViews(final Set<? extends TrackableView> trackerViews) {
        this.trackerViews.addAll(trackerViews);
    }

    public FileTree getFileTree() {
        return fileTree;
    }

    public ObservableList<TrackableView> getTrackerViews() {
        return trackerViews;
    }

    public String getTrackerUrl() {
        return queuedTorrent.getMetaData().getAnnounceUrl();
    }

    public InfoHash getInfoHash() {
        return queuedTorrent.getInfoHash();
    }

    public QueuedTorrentMetaData getMetaData() {
        return queuedTorrent.getMetaData();
    }

    public QueuedTorrentProgress getProgress() { return queuedTorrent.getProgress(); }

    public final LongProperty selectedLengthProperty() {
        return selectedLength;
    }

    public final StringProperty lifeCycleChangeProperty() {
        return lifeCycleChange;
    }

    public String getLifeCycleChange() {
        return lifeCycleChange.getValue();
    }

    public void addQueueStatusChangeListener(final ChangeListener<QueueType> listener) {
        queuedTorrent.queueTypeProperty().addListener(listener);
    }

    public void removeQueueStatusChangeListener(final ChangeListener<QueueType> listener) {
        queuedTorrent.queueTypeProperty().removeListener(listener);
    }

    public void addTorrentStatusChangeListener(final TorrentStatusChangeListener listener) {
        statusChangeListeners.add(listener);
    }

    public void removeTorrentStatusChangeListener(final TorrentStatusChangeListener listener) {
        statusChangeListeners.remove(listener);
    }

    public void addTorrentPriorityChangeListener(final TorrentPriorityChangeListener listener) {
        priorityChangeListeners.add(listener);
    }

    public void removeTorrentPriorityChangeListener(final TorrentPriorityChangeListener listener) {
        priorityChangeListeners.remove(listener);
    }

    public long getSelectedLength() {
        return selectedLength.get();
    }

    public final IntegerProperty priorityProperty() {
        return priority;
    }

    public int getPriority() {
        return priority.get();
    }

    public String getFileName() {
        return queuedTorrent.getProgress().getName();
    }

    public BitsView getAvailabilityView() {
        return availabilityView;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public LongProperty downloadedBytesProperty() {
        return downloadedBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public long getDownloadSpeed() {
        return downloadSpeed;
    }

    public long getDownloadLimit() {
        return downloadLimit;
    }

    public long getRemainingBytes() {
        return remainingBytes;
    }

    public long getUploadedBytes() {
        return uploadedBytes;
    }

    public long getUploadSpeed() {
        return uploadSpeed;
    }

    public long getUploadLimit() {
        return uploadLimit;
    }

    public LongProperty wastedBytesProperty() {
        return wastedBytes;
    }

    public long getWastedBytes() {
        return wastedBytes.get();
    }

    public IntegerProperty hashFailuresProperty() {
        return hashFailures;
    }

    public int getHashFailures() {
        return hashFailures.get();
    }

    public int getSeedsConnected() {
        return seedsConnected;
    }

    public int getSeedsAvailable() {
        return seedsAvailable;
    }

    public int getSeedsInSwarm() {
        return seedsInSwarm;
    }

    public int getPeersConnected() {
        return peersConnected;
    }

    public int getPeersAvailable() {
        return peersAvailable;
    }

    public int getPeersInSwarm() {
        return peersInSwarm;
    }

    public String getShareRatio() {
        return shareRatio;
    }

    public TorrentStatus getStatus() {
        return queuedTorrent.getStatus();
    }

    public QueueType getQueueType() {
        return queuedTorrent.getQueueType();
    }

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public String getComment() {
        return queuedTorrent.getMetaData().getComment();
    }

    public Long getCreationTime() {
        return queuedTorrent.getMetaData().getCreationDate();
    }

    public long getAddedOnTime() {
        return queuedTorrent.getProgress().getAddedOn();
    }

    public String getCreatedBy() {
        return queuedTorrent.getMetaData().getCreatedBy();
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public int getTotalPieces() {
        return queuedTorrent.getMetaData().getTotalPieces();
    }

    public long getPieceLength() {
        return queuedTorrent.getMetaData().getPieceLength();
    }

    public long getTotalLength() {return queuedTorrent.getMetaData().getTotalLength(); }

    public void setHavePiece(final int pieceIndex) {
        this.availabilityView.setHave(pieceIndex, true);
    }

    public void setHavePieces(final byte[] havePieces) {
        this.availabilityView.setHaveFrom(havePieces);
    }

    public long getHavePieces() {
        return this.availabilityView.getHavePiecesCount();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((queuedTorrent == null) ? 0 : queuedTorrent.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TorrentView other = (TorrentView) obj;
        if (queuedTorrent == null) {
            if (other.queuedTorrent != null)
                return false;
        } else if (!queuedTorrent.equals(other.queuedTorrent))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TorrentView{" +
                "queuedTorrent=" + queuedTorrent +
                '}';
    }
}