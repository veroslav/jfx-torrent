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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.matic.torrent.gui.model.TorrentJobDetails;
import org.matic.torrent.gui.tree.TorrentContentTree;
import org.matic.torrent.gui.window.AddNewTorrentOptions;
import org.matic.torrent.gui.window.AddNewTorrentWindow;
import org.matic.torrent.gui.window.UrlLoaderWindow;
import org.matic.torrent.gui.window.UrlLoaderWindowOptions;
import org.matic.torrent.io.codec.BinaryDecoder;
import org.matic.torrent.io.codec.BinaryDecoderException;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedString;
import org.matic.torrent.io.codec.BinaryEncodingKeyNames;
import org.matic.torrent.utils.HashUtilities;

/**
 * Handle file related action events, such as opening and loading files.
 * 
 * @author vedran
 */
public final class FileActionHandler {

	public final TorrentJobDetails onFileOpen(final Window owner) {
		final String torrentPath = getTorrentPath(owner);
		if(torrentPath != null) {
			final BinaryDecoder metaDataDecoder = new BinaryDecoder();
			BinaryEncodedDictionary metaDataDictionary = null;
			try {
				metaDataDictionary = metaDataDecoder.decode(
						new BufferedInputStream(new FileInputStream(torrentPath)));				
			}
			catch (final IOException | BinaryDecoderException e) {
				final Alert errorAlert = new Alert(AlertType.WARNING);
				errorAlert.setTitle("Invalid torrent file");
				errorAlert.setContentText("Unable to load " + Paths.get(torrentPath).getFileName() + "\n"
						+ "The torrent file appears to be invalid.");
				errorAlert.setHeaderText(null);
				errorAlert.showAndWait();
				return null;
			}			
			return addNewTorrentJob(owner, metaDataDictionary);			
		}
		return null;
	}
	
	public final void onFileOpenAndChooseSaveLocation(final Window owner) {
		final String torrentPath = getTorrentPath(owner);
		if(torrentPath != null) {
			final String targetFileName = Paths.get(torrentPath).getFileName().toString();
			final String saveLocationPath = getTargetDirectoryPath(owner, System.getProperty("user.home"), 
					"Choose where to download '" + targetFileName + "' to:");
			if(saveLocationPath != null) {
				//TODO: Use selected target save location
			}			
		}
	}
	
	public final TorrentJobDetails onLoadUrl(final Window owner) {
		final UrlLoaderWindow urlLoaderWindow = new UrlLoaderWindow(owner);
		final UrlLoaderWindowOptions urlLoaderWindowOptions = urlLoaderWindow.showAndWait();
		
		if(urlLoaderWindowOptions != null && 
				urlLoaderWindowOptions.getUrlType() == UrlLoaderWindow.ResourceType.URL) {						
			return addNewTorrentJob(owner, urlLoaderWindowOptions.getTorrentMetaData());
		}				
		return null;
	}
	
	public final String getTargetDirectoryPath(final Window owner, final String initialDirectory, final String title) {
		final DirectoryChooser saveLocationChooser = new DirectoryChooser();
		saveLocationChooser.setInitialDirectory(new File(initialDirectory));
		saveLocationChooser.setTitle(title);
		
		final File selectedLocation = saveLocationChooser.showDialog(owner);
		
		return selectedLocation != null? selectedLocation.getAbsolutePath() : null;
	}
	
	private TorrentJobDetails addNewTorrentJob(final Window owner, final BinaryEncodedDictionary metaData) {
		final BinaryEncodedDictionary infoDictionary = ((BinaryEncodedDictionary)metaData.get(
				BinaryEncodingKeyNames.KEY_INFO));
		final AddNewTorrentWindow addNewTorrentWindow = new AddNewTorrentWindow(
				owner, metaData, new TorrentContentTree(infoDictionary, false));
		final AddNewTorrentOptions addNewTorrentOptions = addNewTorrentWindow.showAndWait();
		if(addNewTorrentOptions == null) {
			return null;
		}
		final byte[] infoHash = ((BinaryEncodedString)metaData.get(BinaryEncodingKeyNames.KEY_INFO_HASH)).getBytes();		
		return new TorrentJobDetails(addNewTorrentOptions.getName(), HashUtilities.convertToHexValue(infoHash), 
				addNewTorrentOptions.getTorrentContents());
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
}