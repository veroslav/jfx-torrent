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

package org.matic.torrent.codec;

public final class BinaryEncodingKeys {
	
	//STORED STATE SPECIFIC PROPERTY KEYS
	public static final BinaryEncodedString STATE_KEY_TORRENT_STATE = new BinaryEncodedString("state");
	public static final BinaryEncodedString STATE_KEY_TORRENT = new BinaryEncodedString("torrent");
	public static final BinaryEncodedString STATE_KEY_LABEL = new BinaryEncodedString("label");
	
	//TRACKER SCRAPE RESPONSE KEYS
	public static final BinaryEncodedString KEY_DOWNLOADED =  new BinaryEncodedString("downloaded");	
	public static final BinaryEncodedString KEY_FLAGS =  new BinaryEncodedString("flags");
	
	//TRACKER RESPONSE KEYS
	public static final BinaryEncodedString KEY_WARNING_MESSAGE =  new BinaryEncodedString("warning message");
	public static final BinaryEncodedString KEY_FAILURE_REASON =  new BinaryEncodedString("failure reason");		
	public static final BinaryEncodedString KEY_MIN_INTERVAL =  new BinaryEncodedString("min interval");
	public static final BinaryEncodedString KEY_TRACKER_ID =  new BinaryEncodedString("tracker id");
	public static final BinaryEncodedString KEY_INCOMPLETE =  new BinaryEncodedString("incomplete");
	public static final BinaryEncodedString KEY_INTERVAL =  new BinaryEncodedString("interval");			
	public static final BinaryEncodedString KEY_COMPLETE =  new BinaryEncodedString("complete");			
	public static final BinaryEncodedString KEY_PEER_ID =  new BinaryEncodedString("peer id");	
	public static final BinaryEncodedString KEY_PEERS =  new BinaryEncodedString("peers");
	public static final BinaryEncodedString KEY_PORT =  new BinaryEncodedString("port");
	public static final BinaryEncodedString KEY_IP =  new BinaryEncodedString("ip");		
	
	//TORRENT META FILE KEYS	
	public static final BinaryEncodedString KEY_ANNOUNCE_LIST = new BinaryEncodedString("announce-list");
	public static final BinaryEncodedString KEY_CREATION_DATE = new BinaryEncodedString("creation date");
	public static final BinaryEncodedString KEY_PIECE_LENGTH = new BinaryEncodedString("piece length");
	public static final BinaryEncodedString KEY_ENCODING =  new BinaryEncodedString("encoding");
	public static final BinaryEncodedString KEY_ANNOUNCE = new BinaryEncodedString("announce");	
	public static final BinaryEncodedString KEY_COMMENT =  new BinaryEncodedString("comment");
	public static final BinaryEncodedString KEY_LENGTH =  new BinaryEncodedString("length");
	public static final BinaryEncodedString KEY_PIECES =  new BinaryEncodedString("pieces");
	public static final BinaryEncodedString KEY_FILES =  new BinaryEncodedString("files");
	public static final BinaryEncodedString KEY_PATH =  new BinaryEncodedString("path");	
	public static final BinaryEncodedString KEY_INFO =  new BinaryEncodedString("info");	
	public static final BinaryEncodedString KEY_NAME =  new BinaryEncodedString("name");	
		
	//CUSTOM PROPERTY KEYS
	public static final BinaryEncodedString KEY_INFO_HASH = new BinaryEncodedString("jfxInfoHash");		
}