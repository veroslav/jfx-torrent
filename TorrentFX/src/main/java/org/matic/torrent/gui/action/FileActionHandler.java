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

package org.matic.torrent.gui.action;

import java.io.File;
import java.nio.file.Paths;

import org.matic.torrent.gui.windows.AddNewTorrentOptions;
import org.matic.torrent.gui.windows.AddNewTorrentWindow;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Handle file related action events, such as opening and loading files.
 */
public final class FileActionHandler {

	public final void onFileOpen(final Window owner) {
		final String torrentPath = getTorrentPath(owner);
		if(torrentPath != null) {
			final AddNewTorrentWindow addNewTorrentWindow = new AddNewTorrentWindow(owner, torrentPath);
			final AddNewTorrentOptions addNewTorrentOptions = addNewTorrentWindow.showAndWait();
			System.out.println(addNewTorrentOptions);
		}
	}
	
	public final void onFileOpenAndChooseSaveLocation(final Window owner) {
		final String torrentPath = getTorrentPath(owner);
		if(torrentPath != null) {
			final String targetFileName = Paths.get(torrentPath).getFileName().toString();
			final String saveLocationPath = getSaveLocationPath(owner, targetFileName);
			if(saveLocationPath != null) {
				System.out.println("Target file: " + targetFileName);
				System.out.println("Save location: " + saveLocationPath);
			}			
		}
	}
	
	private String getTorrentPath(final Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.setTitle("Select a .torrent to open");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Torrents", "*.torrent"),
                new FileChooser.ExtensionFilter("All Files", "*.*")                );
		
		final File selectedFile = fileChooser.showOpenDialog(owner);
		
		return selectedFile != null? selectedFile.getAbsolutePath() : null;
	}
	
	private String getSaveLocationPath(final Window owner, final String targetFileName) {
		final DirectoryChooser saveLocationChooser = new DirectoryChooser();
		saveLocationChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		saveLocationChooser.setTitle("Choose where to download '" + targetFileName + "' to:");
		
		final File selectedLocation = saveLocationChooser.showDialog(owner);
		
		return selectedLocation != null? selectedLocation.getAbsolutePath() : null;
	}
}