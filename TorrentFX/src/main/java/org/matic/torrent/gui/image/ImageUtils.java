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

import javafx.scene.image.Image;

import org.matic.torrent.gui.window.ApplicationWindow;

public final class ImageUtils {

	public static final Image FOLDER_CLOSED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.folder.png"));
	public static final Image FOLDER_OPENED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.folder.open.png"));
	public static final Image FILE_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.page.small.png"));
	
	public static final Image DOWNLOADS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.arrow.down.up.png"), 25, 25, true, true);
	public static final Image LABEL_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.tag.label.png"), 25, 25, true, true);
	public static final Image RSS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.rss.png"), 25, 25, true, true);	
}
