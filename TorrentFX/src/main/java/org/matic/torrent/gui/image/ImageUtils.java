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

package org.matic.torrent.gui.image;

import org.matic.torrent.gui.tree.TorrentEntryTreeItem;

import javafx.scene.image.Image;

public final class ImageUtils {

	public static final Image FOLDER_CLOSED_IMAGE = new Image(
			TorrentEntryTreeItem.class.getResourceAsStream("/images/appbar.folder.png"), 25, 25, true, true);
	public static final Image FOLDER_OPENED_IMAGE = new Image(
			TorrentEntryTreeItem.class.getResourceAsStream("/images/appbar.folder.open.png"), 25, 25, true, true);
	public static final Image FILE_IMAGE = new Image(
			TorrentEntryTreeItem.class.getResourceAsStream("/images/appbar.page.small.png"), 25, 25, true, true);
}
