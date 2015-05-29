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

package org.matic.torrent.io.codec;

import org.matic.torrent.peer.ClientProperties;

public final class BinaryEncodingKeyNames {
	
	//TRACKER RESPONSE KEYS
	public static final BinaryEncodedString KEY_FAILURE_REASON =  new BinaryEncodedString(
			"failure reason".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_WARNING_MESSAGE =  new BinaryEncodedString(
			"warning message".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_INTERVAL =  new BinaryEncodedString(
			"interval".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_MIN_INTERVAL =  new BinaryEncodedString(
			"min interval".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_TRACKER_ID =  new BinaryEncodedString(
			"tracker id".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_COMPLETE =  new BinaryEncodedString(
			"complete".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_INCOMPLETE =  new BinaryEncodedString(
			"incomplete".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_PEERS =  new BinaryEncodedString(
			"peers".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_PEER_ID =  new BinaryEncodedString(
			"peer id".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_IP =  new BinaryEncodedString(
			"ip".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_PORT =  new BinaryEncodedString(
			"port".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	//TORRENT META FILE KEYS
	public static final BinaryEncodedString KEY_PATH =  new BinaryEncodedString(
			"path".getBytes(ClientProperties.STRING_ENCODING_CHARSET));

	public static final BinaryEncodedString KEY_INFO =  new BinaryEncodedString(
			"info".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_NAME =  new BinaryEncodedString(
			"name".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_PIECES =  new BinaryEncodedString(
			"pieces".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_FILES =  new BinaryEncodedString(
			"files".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_LENGTH =  new BinaryEncodedString(
			"length".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_COMMENT =  new BinaryEncodedString(
			"comment".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_ENCODING =  new BinaryEncodedString(
			"encoding".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_CREATION_DATE = new BinaryEncodedString(
			"creation date".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_ANNOUNCE = new BinaryEncodedString(
			"announce".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
	
	public static final BinaryEncodedString KEY_ANNOUNCE_LIST = new BinaryEncodedString(
			"announce-list".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		
	public static final BinaryEncodedString KEY_INFO_HASH = new BinaryEncodedString(
			"jfxInfoHash".getBytes(ClientProperties.STRING_ENCODING_CHARSET));
		
}

