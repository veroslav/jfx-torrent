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
package org.matic.torrent.gui.window;

import javafx.scene.control.TreeItem;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;

/**
 * A bean containing all of the options selected by the user, during the addition of
 * a new torrent.
 * 
 * @author vedran
 *
 */
public final class AddedTorrentOptions {
	
	private final TreeItem<TorrentFileEntry> torrentContents;
	private final QueuedTorrentProgress progress;
	private final QueuedTorrentMetaData metaData;

	private final boolean createSubfolder;
	private final boolean skipHashCheck;
	private final boolean addToTopQueue;
	private final boolean startTorrent;

	public AddedTorrentOptions(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress,
			final TreeItem<TorrentFileEntry> torrentContents, final boolean startTorrent, final boolean createSubfolder,
			final boolean addToTopQueue, final boolean skipHashCheck) {
		this.metaData = metaData;
        this.progress = progress;
		this.torrentContents = torrentContents;

		this.startTorrent = startTorrent;
		this.createSubfolder = createSubfolder;
		this.addToTopQueue = addToTopQueue;
		this.skipHashCheck = skipHashCheck;
	}
	
	public QueuedTorrentMetaData getMetaData() {
		return metaData;
	}
	
	public QueuedTorrentProgress getProgress() {
		return progress;
	}
	
	public TreeItem<TorrentFileEntry> getTorrentContents() {
		return torrentContents;
	}

    public boolean shouldCreateSubfolder() {
        return createSubfolder;
    }

    public boolean shouldAddToTopQueue() {
        return addToTopQueue;
    }

    public boolean shouldSkipHashCheck() {
        return skipHashCheck;
    }

    @Override
    public String toString() {
        return "AddedTorrentOptions{" +
                "torrentContents=" + torrentContents +
                ", progress=" + progress +
                ", metaData=" + metaData +
                ", createSubfolder=" + createSubfolder +
                ", skipHashCheck=" + skipHashCheck +
                ", addToTopQueue=" + addToTopQueue +
                ", startTorrent=" + startTorrent +
                '}';
    }
}