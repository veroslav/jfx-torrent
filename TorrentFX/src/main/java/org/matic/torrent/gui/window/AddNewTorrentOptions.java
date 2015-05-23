/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015 Vedran Matic
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

import org.matic.torrent.gui.tree.TorrentContentTree;

/**
 * A bean containing all of the options selected by the user, during the addition of
 * a new torrent.
 * 
 * @author vedran
 *
 */
public final class AddNewTorrentOptions {
	
	private final TorrentContentTree torrentContentTree;
			
	private final boolean createSubfolder;
	private final boolean skipHashCheck;
	private final boolean addToTopQueue;
	private final boolean startTorrent;
	
	private final String label;
	private final String name;
	private final String path;

	public AddNewTorrentOptions(final TorrentContentTree torrentContentTree, final String name, final String path,
			final String label, final boolean startTorrent, final boolean createSubfolder, final boolean addToTopQueue,
			final boolean skipHashCheck) {
		this.torrentContentTree = torrentContentTree;
		this.name = name;
		this.path = path;
		this.label = label;
		
		this.startTorrent = startTorrent;
		this.createSubfolder = createSubfolder;
		this.addToTopQueue = addToTopQueue;
		this.skipHashCheck = skipHashCheck;
	}

	public final TorrentContentTree getTorrentContentTree() {
		return torrentContentTree;
	}

	public final boolean isCreateSubfolder() {
		return createSubfolder;
	}

	public final boolean isSkipHashCheck() {
		return skipHashCheck;
	}

	public final boolean isAddToTopQueue() {
		return addToTopQueue;
	}

	public final boolean isStartTorrent() {
		return startTorrent;
	}

	public final String getLabel() {
		return label;
	}

	public final String getName() {
		return name;
	}

	public final String getPath() {
		return path;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (addToTopQueue ? 1231 : 1237);
		result = prime * result + (createSubfolder ? 1231 : 1237);
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + (skipHashCheck ? 1231 : 1237);
		result = prime * result + (startTorrent ? 1231 : 1237);
		result = prime
				* result
				+ ((torrentContentTree == null) ? 0 : torrentContentTree
						.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddNewTorrentOptions other = (AddNewTorrentOptions) obj;
		if (addToTopQueue != other.addToTopQueue)
			return false;
		if (createSubfolder != other.createSubfolder)
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (skipHashCheck != other.skipHashCheck)
			return false;
		if (startTorrent != other.startTorrent)
			return false;
		if (torrentContentTree == null) {
			if (other.torrentContentTree != null)
				return false;
		} else if (!torrentContentTree.equals(other.torrentContentTree))
			return false;
		return true;
	}
}