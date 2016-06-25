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
package org.matic.torrent.queue;

import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.hash.InfoHash;

import java.io.IOException;
import java.util.Iterator;

/**
 * A convenience class that provides easier access to a torrent's meta data.
 * 
 * @author Vedran Matic
 *
 */
public final class QueuedTorrentMetaData {

	private final BinaryEncodedDictionary infoDictionary;
	private final BinaryEncodedDictionary metaData;
	
	private final InfoHash infoHash;
	
	public QueuedTorrentMetaData(final BinaryEncodedDictionary metaData) {
		this.metaData = metaData;
		this.infoDictionary = (BinaryEncodedDictionary)metaData.get(BinaryEncodingKeys.KEY_INFO);
		this.infoHash = new InfoHash(((BinaryEncodedString)metaData.get(
				BinaryEncodingKeys.KEY_INFO_HASH)).getBytes());
	}
	
	public BinaryEncodable remove(final BinaryEncodedString keyName) {
		return metaData.remove(keyName);
	}
	
	public InfoHash getInfoHash() {
		return infoHash;
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
	
	public BinaryEncodedString getComment() {
		return (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_COMMENT);
	}
	
	public BinaryEncodedInteger getCreationDate() {
		return (BinaryEncodedInteger)metaData.get(BinaryEncodingKeys.KEY_CREATION_DATE);
	}

    public BinaryEncodedString getCreatedBy() {
        return (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_CREATED_BY);
    }
	
	public BinaryEncodedList getFiles() {
		return (BinaryEncodedList)infoDictionary.get(BinaryEncodingKeys.KEY_FILES);
	}

    public long getTotalLength() {
        if(isSingleFile()) {
            return getSingleFileLength().getValue();
        }

        final Iterator<BinaryEncodable> fileIterator = getFiles().iterator();
        long length = 0;
        while(fileIterator.hasNext()) {
            final BinaryEncodedDictionary fileDictionary = (BinaryEncodedDictionary)fileIterator.next();
            length += ((BinaryEncodedInteger)fileDictionary.get(
                    BinaryEncodingKeys.KEY_LENGTH)).getValue();
        }

        return length;
    }
	
	public String getName() {
		return infoDictionary.get(BinaryEncodingKeys.KEY_NAME).toString();
	}

    public long getPieceLength() {
        return ((BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_PIECE_LENGTH)).getValue();
    }

    public int getTotalPieces() {
        return (int)Math.ceil(((double)getTotalLength()) / getPieceLength());
    }

	public byte[] toExportableValue() throws IOException {
		return metaData.toExportableValue();
	}

    private BinaryEncodedInteger getSingleFileLength() {
        return (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_LENGTH);
    }
}