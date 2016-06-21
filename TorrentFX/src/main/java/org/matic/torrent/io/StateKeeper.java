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
package org.matic.torrent.io;

import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.exception.BinaryDecoderException;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.PathProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;

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

public class StateKeeper {
	
	private static final String TORRENT_FILE_EXTENSION = ".torrent";
	private static final String STATE_FILE_EXTENSION = ".state";
	
	/**
	 * Delete a torrent's meta data (.torrent) and state (.state) files from the disk
	 * 
	 * @param torrent Torrent to delete
	 * @throws IOException If the files to delete can't be found
	 */
	public static void delete(final QueuedTorrent torrent) throws IOException {		
		//TODO: Don't use default target path, as the torrent might have been re-located.
		final Path targetPath = Paths.get(StateKeeper.buildTargetPath());
		final Path torrentFilePath = Paths.get(targetPath.toString(),
				torrent.getMetaData().getInfoHash().toString() + TORRENT_FILE_EXTENSION);
		final Path stateFilePath = Paths.get(targetPath.toString(),
				torrent.getMetaData().getInfoHash().toString() + STATE_FILE_EXTENSION);
		
		Files.deleteIfExists(torrentFilePath);
		Files.deleteIfExists(stateFilePath);
	}
	
	/**
	 * Save a torrent's meta data (.torrent) and state (.state) files to the disk
	 * 
	 * @param torrent Torrent to save
	 */
	public static void store(final QueuedTorrent torrent) {
		final String targetPath = StateKeeper.buildTargetPath(); 
		storeMetaData(torrent.getMetaData(), targetPath);
		storeState(torrent.getProgress(),
				torrent.getMetaData().getInfoHash().toString().toLowerCase(), targetPath);
	}
	
	/**
	 * Load all of the torrents and their state data from their disk location
	 * 
	 * @return Loaded torrents
	 */
	public static Set<QueuedTorrent> loadAll() {
		final Set<QueuedTorrent> loadedTorrents = new HashSet<>();
		final BinaryDecoder decoder = new BinaryDecoder();
		
		final Set<String> torrentNames = new HashSet<>();
		final Path path = Paths.get(StateKeeper.buildTargetPath());		
		
		try {
			Files.createDirectories(path);
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		
		try(final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			directoryStream.forEach(p -> {				
				if(p.toString().endsWith(TORRENT_FILE_EXTENSION)) {
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
			final String metaDataPath = n + TORRENT_FILE_EXTENSION;
			final String statePath = n + STATE_FILE_EXTENSION;
			
			QueuedTorrentMetaData metaData = null;			
			try(final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(metaDataPath))) {
				metaData = new QueuedTorrentMetaData(decoder.decode(bis));
			}		
			catch(final IOException | BinaryDecoderException e) {
				e.printStackTrace();
			}
			
			QueuedTorrentProgress progress = null;
			try(final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(statePath))) {
				progress = new QueuedTorrentProgress(decoder.decode(bis));
			}		
			catch(final IOException | BinaryDecoderException e) {
				e.printStackTrace();
			}
			loadedTorrents.add(new QueuedTorrent(metaData, progress));
		});
		
		return loadedTorrents;
	}
	
	private static String buildTargetPath() {
		final String path = ApplicationPreferences.getProperty(PathProperties.NEW_TORRENTS,
				PathProperties.DEFAULT_STORE_TORRENTS_PATH);
		return path.endsWith(File.separator)? path : path + File.separator;
	}
	
	private static boolean storeMetaData(final QueuedTorrentMetaData metaData, final String targetPath) {
		final String metaDataFileLocation = targetPath + metaData.getInfoHash().toString() + TORRENT_FILE_EXTENSION;
		
		if(Files.exists(Paths.get(metaDataFileLocation))) {
			return true;
		}
		
		metaData.remove(BinaryEncodingKeys.KEY_INFO_HASH);

		try(final BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(metaDataFileLocation))) {			
				writer.write(metaData.toExportableValue());
				writer.flush();
		}
		catch(final IOException ioe) {
			return false;
		}
		return true;
	}

	private static boolean storeState(final QueuedTorrentProgress state, final String infoHash, final String targetPath) {	
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
}