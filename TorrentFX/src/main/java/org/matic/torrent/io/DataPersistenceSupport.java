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
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.TorrentTemplate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DataPersistenceSupport {

    private static final String TORRENT_FILE_EXTENSION = ".torrent";
    private static final String STATE_FILE_EXTENSION = ".state";

    private final String workPath;

    public DataPersistenceSupport(final String workPath) {
        this.workPath = workPath;
    }

    /**
     * Delete a torrent's meta data (.torrent) and state (.state) files from the disk
     *
     * @param infoHash Info hash if the torrent to be deleted
     * @throws IOException If the files to delete can't be found
     */
    public void delete(final InfoHash infoHash) throws IOException {
        final Path targetPath = Paths.get(workPath);
        final Path torrentFilePath = Paths.get(targetPath.toString(),
                infoHash.toString() + TORRENT_FILE_EXTENSION);
        final Path stateFilePath = Paths.get(targetPath.toString(),
                infoHash.toString() + STATE_FILE_EXTENSION);

        Files.deleteIfExists(torrentFilePath);
        Files.deleteIfExists(stateFilePath);
    }

    /**
     * Check whether a torrent has been stored on the disk.
     *
     * @param infoHash Info hash of the torrent to check.
     * @return Whether the torrent is stored on the disk.
     */
    public boolean isPersisted(final InfoHash infoHash) {
        return Files.exists(Paths.get(workPath + infoHash.toString().toLowerCase() + STATE_FILE_EXTENSION));
    }

    /**
     * Save a torrent's meta data (.torrent) and state (.state) files to the disk.
     *
     * @param metaData Meta data to be saved.
     * @param progress Torrent's progress to be saved.
     */
    public void store(final QueuedTorrentMetaData metaData, final QueuedTorrentProgress progress) {
        storeMetaData(metaData);
        storeProgress(progress, metaData.getInfoHash().toString().toLowerCase());
    }

    /**
     * Load all of the torrents and their state data from their disk location.
     *
     * @return Loaded torrents.
     */
    public List<TorrentTemplate> loadAll() {
        final List<TorrentTemplate> loadedTorrents = new ArrayList<>();
        final BinaryDecoder decoder = new BinaryDecoder();

        final Set<String> torrentNames = new HashSet<>();
        final Path path = Paths.get(workPath);

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

            final QueuedTorrentProgress progress;
            try(final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(statePath))) {
                progress = new QueuedTorrentProgress(decoder.decode(bis));
                loadedTorrents.add(new TorrentTemplate(metaData, progress));
            }
            catch(final IOException | BinaryDecoderException e) {
                e.printStackTrace();
            }
        });

        return loadedTorrents;
    }

    private boolean storeMetaData(final QueuedTorrentMetaData metaData) {
        final String metaDataFileLocation = workPath + metaData.getInfoHash().toString() + TORRENT_FILE_EXTENSION;

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

    private boolean storeProgress(final QueuedTorrentProgress progress, final String infoHash) {
        try(final BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(
                workPath + infoHash.toString() + STATE_FILE_EXTENSION))) {
            writer.write(progress.toExportableValue());
            writer.flush();
        }
        catch(final IOException ioe) {
            return false;
        }
        return true;
    }
}