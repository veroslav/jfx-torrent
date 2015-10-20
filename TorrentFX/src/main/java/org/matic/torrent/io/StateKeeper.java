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
package org.matic.torrent.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.exception.BinaryDecoderException;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.PathProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;

public class StateKeeper {
	
	private static final String TORRENT_FILE_EXTENSION = ".torrent";
	private static final String STATE_FILE_EXTENSION = ".state";
	
	private final String targetPath;
	
	public StateKeeper() {	
		final String path = ApplicationPreferences.getProperty(PathProperties.NEW_TORRENTS,
				PathProperties.DEFAULT_STORE_TORRENTS_PATH);
		this.targetPath = path.endsWith(File.separator)? path : path + File.separator;
	}
	
	public final void store(final QueuedTorrent torrent) {
		storeMetaData(torrent.getMetaData());
		storeState(torrent.getProperties(), torrent.getMetaData().getInfoHash().toString().toLowerCase());
	}
	
	private final boolean storeMetaData(final QueuedTorrentMetaData metaData) {
		final String metaDataFileLocation = targetPath + metaData.getInfoHash().toString() + TORRENT_FILE_EXTENSION;
		
		if(Files.exists(Paths.get(metaDataFileLocation))) {
			return true;
		}
		
		try(final BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(metaDataFileLocation))) {			
				writer.write(metaData.toExportableValue());
				writer.flush();
		}
		catch(final IOException ioe) {
			return false;
		}
		return true;
	}

	private final boolean storeState(final QueuedTorrentProgress state, final String infoHash) {	
		try(final BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(
				targetPath + infoHash.toString() + STATE_FILE_EXTENSION))) {				
				writer.write(state.toExportableValue());
				writer.flush();
		}
		catch(final IOException ioe) {
			return false;
		}
		return true;
	}
	
	public final Set<QueuedTorrent> loadAll() {
		final Set<QueuedTorrent> loadedTorrents = new HashSet<>();
		final BinaryDecoder decoder = new BinaryDecoder();
		
		final Set<String> torrentNames = new HashSet<>();
		final Path path = Paths.get(targetPath);		
		
		try {
			Files.createDirectories(path);
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		
		try(final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			directoryStream.forEach(p -> {
				if(p.endsWith(TORRENT_FILE_EXTENSION)) {
					final String filePath = p.toString();					
					final int suffixIndex = filePath.lastIndexOf('.');
					torrentNames.add(filePath.substring(0, suffixIndex)); 
				}				
			});
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
			return loadedTorrents;
		}
			
		torrentNames.forEach(n -> {	
			final Path metaDataPath = Paths.get(targetPath + n + File.separator + TORRENT_FILE_EXTENSION);
			final Path statePath = Paths.get(targetPath + n + File.separator + STATE_FILE_EXTENSION);
			
			QueuedTorrentMetaData metaData = null;			
			try(final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(metaDataPath.toFile()))) {
				metaData = new QueuedTorrentMetaData(decoder.decode(bis));
			}		
			catch(final IOException | BinaryDecoderException e) {
				e.printStackTrace();
			}
			
			QueuedTorrentProgress progress = null;
			try(final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(statePath.toFile()))) {
				progress = new QueuedTorrentProgress(decoder.decode(bis));
			}		
			catch(final IOException | BinaryDecoderException e) {
				e.printStackTrace();
			}
			loadedTorrents.add(new QueuedTorrent(metaData, progress));
		});
		
		return loadedTorrents;
	}
}