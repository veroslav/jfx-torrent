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

package org.matic.torrent.gui.windows;

import java.nio.file.Paths;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

/**
 * A window showing contents of a torrent to be opened and added to a list of torrents
 * 
 * @author vedran
 *
 */
public final class AddNewTorrentWindow {
	
	private final Dialog<ButtonType> window;
	private final String torrentPath;

	public AddNewTorrentWindow(final Window owner, final String torrentPath) {
		this.torrentPath = torrentPath;
		window = new Dialog<>();
		window.initOwner(owner);
		
		initComponents();
	}
	
	public final AddNewTorrentOptions showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();
		return null;
	}
	
	private void initComponents() {
		window.setHeaderText(null);
		window.setTitle(Paths.get(torrentPath).getFileName() + " - Add New Torrent");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private Node layoutContent() {
		final HBox mainPane = new HBox();
		
		mainPane.setPrefSize(900, 400);
		
		return mainPane;
	}
	
	private Pane buildSaveOptionsPane() {
		return null;
	}
	
	private Pane buildNamePane() {
		return null;
	}
	
	private Pane buildTorrentOptionsPane() {
		return null;
	}
	
	private Pane buildTorrentContentsPane() {
		return null;
	}
}