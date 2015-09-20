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

import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeyNames;
import org.matic.torrent.hash.InfoHash;

public class QueuedTorrentMetaData {

	private final BinaryEncodedDictionary metaData;
	
	private final InfoHash infoHash;
	
	public QueuedTorrentMetaData(final BinaryEncodedDictionary metaData) {
		this.metaData = metaData;
		
		this.infoHash = new InfoHash(((BinaryEncodedString)metaData.get(
				BinaryEncodingKeyNames.KEY_INFO_HASH)).getBytes());
	}
	
	public final InfoHash getInfoHash() {
		return infoHash;
	}
	
	public final BinaryEncodedDictionary getInfoDictionary() {
		return (BinaryEncodedDictionary)metaData.get(BinaryEncodingKeyNames.KEY_INFO);
	}
	
	public final String getAnnounceUrl() {
		final BinaryEncodedString url = (BinaryEncodedString)metaData.get(BinaryEncodingKeyNames.KEY_ANNOUNCE);
		return url == null? null : url.toString();
	}
	
	public final BinaryEncodedList getAnnounceList() {
		final BinaryEncodedList announceList = (BinaryEncodedList)metaData.get(BinaryEncodingKeyNames.KEY_ANNOUNCE_LIST);
		return announceList == null? new BinaryEncodedList() : announceList;
		/*final Set<String> result = new HashSet<>();
		
		if(urls != null) {
			urls.stream().flatMap(l -> ((BinaryEncodedList)l).stream()).forEach(url -> result.add(url.toString()));
		}
		
		return result;*/
	}

	@Override
	public final String toString() {
		return metaData.toExportableValue();
	}
	
}