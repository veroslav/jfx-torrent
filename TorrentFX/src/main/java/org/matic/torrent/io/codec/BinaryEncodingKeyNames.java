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

import java.nio.charset.Charset;

public final class BinaryEncodingKeyNames {
	
	public static final BinaryEncodedString KEY_PATH =  new BinaryEncodedString(
			"path".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));

	public static final BinaryEncodedString KEY_INFO =  new BinaryEncodedString(
			"info".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_NAME =  new BinaryEncodedString(
			"name".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_PIECES =  new BinaryEncodedString(
			"pieces".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_FILES =  new BinaryEncodedString(
			"files".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_LENGTH =  new BinaryEncodedString(
			"length".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_COMMENT =  new BinaryEncodedString(
			"comment".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_ENCODING =  new BinaryEncodedString(
			"encoding".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_CREATION_DATE = new BinaryEncodedString(
			"creation date".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_ANNOUNCE = new BinaryEncodedString(
			"announce".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
	
	public static final BinaryEncodedString KEY_ANNOUNCE_LIST = new BinaryEncodedString(
			"announce-list".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
		
	protected static final BinaryEncodedString KEY_INFO_HASH = new BinaryEncodedString(
			"jfxInfoHash".getBytes(Charset.forName(BinaryEncodedString.ENCODING_UTF8)));
		
}

