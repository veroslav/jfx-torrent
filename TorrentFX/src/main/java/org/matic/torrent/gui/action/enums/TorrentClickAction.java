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
package org.matic.torrent.gui.action.enums;

public enum TorrentClickAction {
	SHOW_PROPERTIES("Show Properties"), START_OR_STOP("Start/Stop"),
    OPEN_FOLDER("Open Folder");
	
	private final String label;
	
	TorrentClickAction(final String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}