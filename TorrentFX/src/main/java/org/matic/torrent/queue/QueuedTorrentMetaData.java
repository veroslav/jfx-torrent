/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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
package org.matic.torrent.queue;

import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.hash.InfoHash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A convenience class that provides easier access to a torrent's meta data.
 *
 * @author Vedran Matic
 *
 */
public final class QueuedTorrentMetaData {

    private static final int PIECE_HASH_LENGTH = 20;

    private final List<QueuedFileMetaData> fileMetaDatas;

    private final BinaryEncodedDictionary infoDictionary;
    private final BinaryEncodedDictionary metaData;

    private final InfoHash infoHash;

    public QueuedTorrentMetaData(final BinaryEncodedDictionary metaData) {
        this.metaData = metaData;
        this.infoDictionary = (BinaryEncodedDictionary)metaData.get(BinaryEncodingKeys.KEY_INFO);
        this.infoHash = new InfoHash(((BinaryEncodedString)metaData.get(
                BinaryEncodingKeys.KEY_INFO_HASH)).getBytes());

        fileMetaDatas = buildFileMetaDatas();
    }

    public BinaryEncodable remove(final BinaryEncodedString keyName) {
        return metaData.remove(keyName);
    }

    public InfoHash getInfoHash() {
        return infoHash;
    }

    public byte[] getPieceHash(final int pieceIndex) {
        final BinaryEncodedString pieceHashes = (BinaryEncodedString)infoDictionary.get(BinaryEncodingKeys.KEY_PIECES);
        return pieceHashes.getBytes(PIECE_HASH_LENGTH * pieceIndex, PIECE_HASH_LENGTH);
    }

    public boolean isSingleFile() {
        return getSingleFileLength() != null;
    }

    public String getAnnounceUrl() {
        final BinaryEncodedString url = (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_ANNOUNCE);
        return url == null? null : url.toString();
    }

    public BinaryEncodedList getAnnounceList() {
        final BinaryEncodedList announceList = (BinaryEncodedList)metaData.get(BinaryEncodingKeys.KEY_ANNOUNCE_LIST);
        return announceList == null? new BinaryEncodedList() : announceList;
    }

    public String getComment() {
        final BinaryEncodedString comment = (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_COMMENT);
        return comment != null? comment.getValue() : "";
    }

    public Long getCreationDate() {
        final BinaryEncodedInteger creationTime = (BinaryEncodedInteger)metaData.get(BinaryEncodingKeys.KEY_CREATION_DATE);
        return creationTime != null? creationTime.getValue() : null;
    }

    public String getCreatedBy() {
        final BinaryEncodedString createdBy = (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_CREATED_BY);
        return createdBy != null? createdBy.getValue() : "";
    }

    public List<QueuedFileMetaData> getFiles() {
        return fileMetaDatas;
    }

    public long getTotalLength() {
        if(isSingleFile()) {
            return getSingleFileLength();
        }

        return fileMetaDatas.stream().mapToLong(QueuedFileMetaData::getLength).sum();
    }

    public String getName() {
        return infoDictionary.get(BinaryEncodingKeys.KEY_NAME).toString();
    }

    public int getPieceLength() {
        return (int)((BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_PIECE_LENGTH)).getValue();
    }

    public int getTotalPieces() {
        return (int)Math.ceil(((double)getTotalLength()) / getPieceLength());
    }

    public byte[] toExportableValue() throws IOException {
        return metaData.toExportableValue();
    }

    /**
     * Get a file's order within the torrent.
     *
     * @return The position of the file within the torrent
     */
    public int getFileMetaDataIndex(final QueuedFileMetaData fileMetaData) {
        return fileMetaDatas.indexOf(fileMetaData);
    }

    private Long getSingleFileLength() {
        final BinaryEncodedInteger fileLength = (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_LENGTH);
        return fileLength != null? fileLength.getValue() : null;
    }

    private List<QueuedFileMetaData> buildFileMetaDatas() {
        final List<QueuedFileMetaData> fileMetaDatas = new ArrayList<>();

        //Check whether it is a single file torrent
        final BinaryEncodedInteger singleFileLength = (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_LENGTH);
        if(singleFileLength != null) {
            final String filePath = infoDictionary.get(BinaryEncodingKeys.KEY_NAME).toString();
            final QueuedFileMetaData singleFileMetaData = new QueuedFileMetaData(
                    Paths.get(filePath), singleFileLength.getValue(), 0);
            fileMetaDatas.add(singleFileMetaData);
            return fileMetaDatas;
        }

        final BinaryEncodedList fileMetaDataList = (BinaryEncodedList)infoDictionary.get(BinaryEncodingKeys.KEY_FILES);

        final Iterator<BinaryEncodable> listIterator = fileMetaDataList.iterator();
        long currentLength = 0;

        while(listIterator.hasNext()) {
            final BinaryEncodedDictionary fileDictionary = (BinaryEncodedDictionary)listIterator.next();
            final long fileLength = ((BinaryEncodedInteger)fileDictionary.get(
                    BinaryEncodingKeys.KEY_LENGTH)).getValue();

            final BinaryEncodedList filePaths = (BinaryEncodedList)fileDictionary.get(BinaryEncodingKeys.KEY_PATH);
            final String filePath = filePaths.stream().map(path ->
                    ((BinaryEncodedString)path).getValue()).collect(Collectors.joining(File.pathSeparator));

            final QueuedFileMetaData fileMetaData = new QueuedFileMetaData(
                    Paths.get(filePath), fileLength, currentLength);
            currentLength += fileLength;

            fileMetaDatas.add(fileMetaData);
        }

        return Collections.unmodifiableList(fileMetaDatas);
    }
}