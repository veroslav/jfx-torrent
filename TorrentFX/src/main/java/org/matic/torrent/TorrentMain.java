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

package org.matic.torrent;

import org.matic.torrent.gui.windows.ApplicationWindow;

import javafx.application.Application;
import javafx.stage.Stage;

public final class TorrentMain extends Application {

	/**
	 * Main application execution entry point. Used when the application packaging
	 * is performed by other means than by JavaFX
	 * 
	 * @param args Application parameters
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public final void start(final Stage stage) throws Exception {
		new ApplicationWindow(stage);
	}
}
