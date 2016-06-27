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
package org.matic.torrent.gui.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.TorrentStatus;
import org.matic.torrent.tracking.beans.TrackerSessionView;

import java.util.LinkedHashSet;
import java.util.Set;

public final class TorrentView {

    private final AvailabilityView availabilityView;
    private final QueuedTorrent queuedTorrent;

    private final IntegerProperty priority;

    private long elapsedTime;
    private long downloadedBytes;
    private long downloadSpeed;
    private long downloadLimit;

    private long remainingBytes;
    private long uploadedBytes;
    private long uploadSpeed;
    private long uploadLimit;

    private long wastedBytes;
    private int hashFailures;

    private int seedsConnected;
    private int seedsAvailable;
    private int seedsInSwarm;

    private int peersConnected;
    private int peersAvailable;
    private int peersInSwarm;

    private String shareRatio;
    private String saveDirectory;

    private long completionTime;
    private long havePieces;

    private final Set<TrackerSessionView> trackerSessionViews = new LinkedHashSet<>();

	public TorrentView(final QueuedTorrent queuedTorrent) {
		this.priority = new SimpleIntegerProperty(0);
        this.queuedTorrent = queuedTorrent;

        availabilityView = new AvailabilityView(this.queuedTorrent.getMetaData().getTotalPieces());
	}
    public void addTrackerSessionViews(final Set<TrackerSessionView> trackerSessionViews) {
        this.trackerSessionViews.addAll(trackerSessionViews);
    }

    public FileTree getFileTree() {
        return new FileTree(queuedTorrent.getMetaData(), queuedTorrent.getProgress());
    }

    public Set<TrackerSessionView> getTrackerSessionViews() {
        return trackerSessionViews;
    }

    public InfoHash getInfoHash() {
        return queuedTorrent.getInfoHash();
    }

    public QueuedTorrentMetaData getMetaData() {
        return queuedTorrent.getMetaData();
    }

    public IntegerProperty priorityProperty() {
        return priority;
    }

    public int getPriority() {
        return priority.get();
    }

    public String getFileName() {
        return queuedTorrent.getProgress().getName();
    }

    public AvailabilityView getAvailabilityView() {
        return availabilityView;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
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

    public long getWastedBytes() {
        return wastedBytes;
    }

    public int getHashFailures() {
        return hashFailures;
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

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public String getComment() {
        final BinaryEncodedString comment = queuedTorrent.getMetaData().getComment();
        return comment != null? comment.getValue() : "";
    }

    public String getHash() {
        return queuedTorrent.getInfoHash().toString().toUpperCase();
    }

    public Long getCreationTime() {
        final BinaryEncodedInteger creationTime = queuedTorrent.getMetaData().getCreationDate();
        return creationTime != null? creationTime.getValue() * 1000: null;
    }

    public long getAddedOnTime() {
        return queuedTorrent.getProgress().getAddedOn();
    }

    public String getCreatedBy() {
        final BinaryEncodedString createdBy = queuedTorrent.getMetaData().getCreatedBy();
        return createdBy != null? createdBy.getValue() : "";
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

    public long getHavePieces() {
        return havePieces;
    }
}