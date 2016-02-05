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
package org.matic.torrent.queue;

import java.io.IOException;

import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.hash.InfoHash;

/**
 * A convenience class that provides easier access to a torrent's meta data
 * 
 * @author Vedran Matic
 *
 */
public class QueuedTorrentMetaData {

	private final BinaryEncodedDictionary infoDictionary;
	private final BinaryEncodedDictionary metaData;
	
	private final InfoHash infoHash;
	
	public QueuedTorrentMetaData(final BinaryEncodedDictionary metaData) {
		this.metaData = metaData;
		this.infoDictionary = (BinaryEncodedDictionary)metaData.get(BinaryEncodingKeys.KEY_INFO);
		this.infoHash = new InfoHash(((BinaryEncodedString)metaData.get(
				BinaryEncodingKeys.KEY_INFO_HASH)).getBytes());
	}
	
	public final BinaryEncodable remove(final BinaryEncodedString keyName) {
		return metaData.remove(keyName);
	}
	
	public final InfoHash getInfoHash() {
		return infoHash;
	}
	
	public final BinaryEncodedDictionary getInfoDictionary() {
		return infoDictionary;
	}
	
	public final String getAnnounceUrl() {
		final BinaryEncodedString url = (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_ANNOUNCE);
		return url == null? null : url.toString();
	}
	
	public final BinaryEncodedList getAnnounceList() {
		final BinaryEncodedList announceList = (BinaryEncodedList)metaData.get(BinaryEncodingKeys.KEY_ANNOUNCE_LIST);
		return announceList == null? new BinaryEncodedList() : announceList;		
	}
	
	public final BinaryEncodedString getComment() {
		return (BinaryEncodedString)metaData.get(BinaryEncodingKeys.KEY_COMMENT);
	}
	
	public final BinaryEncodedInteger getCreationDate() {
		return (BinaryEncodedInteger)metaData.get(BinaryEncodingKeys.KEY_CREATION_DATE);
	}
	
	public final BinaryEncodedList getFiles() {
		return (BinaryEncodedList)infoDictionary.get(BinaryEncodingKeys.KEY_FILES);
	}
	
	public final BinaryEncodedInteger getLength() {
		return (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_LENGTH);
	}
	
	public final String getName() {
		return infoDictionary.get(BinaryEncodingKeys.KEY_NAME).toString();
	}
	
	public final BinaryEncodedInteger getPieceLength() {
		return (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeys.KEY_PIECE_LENGTH);
	}

	public final byte[] toExportableValue() throws IOException {
		return metaData.toExportableValue();
	}
}