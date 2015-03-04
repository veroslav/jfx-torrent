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

package org.matic.torrent.gui.tree;

import java.util.HashMap;
import java.util.Map;

public final class TorrentEntryNode<T> {
	
	private final Map<String, TorrentEntryNode<T>> children;
	private final T data;
	private final String name;

	public TorrentEntryNode(final String name, final T data) {
		this.name = name;
		this.data = data;
		
		children = new HashMap<>();
	}
	
	public final String getName() {
		return name;
	}
	
	public final T getData() {
		return data;
	}
	
	public final TorrentEntryNode<T> getChild(final String name) {
		return children.get(name);
	}
	
	public final boolean contains(final String childName) {
		return children.containsKey(childName);
	}
	
	public final void add(final TorrentEntryNode<T> child) {
		children.put(child.getName(), child);
	}
}
