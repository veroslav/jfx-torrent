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
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.queue.enums.TorrentStatus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TorrentView {

    private final BitsView availabilityView;
    private final QueuedTorrent queuedTorrent;

    private final LongProperty selectedLength;
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

    private final Set<TrackableView> trackerViews = new LinkedHashSet<>();
    private final Set<PeerView> peerViews = new LinkedHashSet<>();

	public TorrentView(final QueuedTorrent queuedTorrent) {
		this.priority = new SimpleIntegerProperty(0);
		this.selectedLength = new SimpleLongProperty(0);
        this.queuedTorrent = queuedTorrent;

        availabilityView = new BitsView(this.queuedTorrent.getMetaData().getTotalPieces());
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
        return new FileTree(queuedTorrent.getMetaData(), queuedTorrent.getProgress());
    }

    public Set<TrackableView> getTrackerViews() {
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
    
    public LongProperty selectedLengthProperty() {
    	return selectedLength;
    }

    public void addQueueStatusChangeListener(final ChangeListener<QueueStatus> listener) {
        queuedTorrent.queueStatusProperty().addListener(listener);
    }

    public long getSelectedLength() {
    	return selectedLength.get();
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

    public BitsView getAvailabilityView() {
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

    public QueueStatus getQueueStatus() {
        return queuedTorrent.getQueueStatus();
    }

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public String getComment() {
        final BinaryEncodedString comment = queuedTorrent.getMetaData().getComment();
        return comment != null? comment.getValue() : "";
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