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

package org.matic.torrent.hash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

public final class HashUtilities {
	
	public static final int HEX_INFO_HASH_LENGTH = 40;	//160 bits / 4 bytes == 40 places
	
	private static final Pattern HEX_MATCH_PATTERN = Pattern.compile("[0-9a-fA-F]+");

	/**
	 * Check whether a string contains a valid hexadecimal number
	 * (valid values are 0123456789ABCDEF)
	 * 
	 * @param value String value to check
	 * @return Whether value is a valid hexadecimal number
	 */
	public static boolean isValidHexNumber(final String value) {
		final Matcher hexPatternMatch = HEX_MATCH_PATTERN.matcher(value);
		return hexPatternMatch.matches();
	}
	
	/**
	 * 
	 * URL encode binary data (SHA-1 hash)
	 * 
	 * @param bytes Binary data to be URL encoded
	 * @return URL encoded binary data
	 */
	public static String urlEncodeBytes(final byte[] bytes) {
		final StringBuilder encodedData = new StringBuilder();		
		final byte[] currentByte = new byte[1];
		
		for(int i = 0; i < bytes.length; ++i) {
			final int value = bytes[i];
			if((value > 47 && value < 58) || (value > 96 && value < 123)
					|| (value > 64 && value < 91) || value == 46
					|| value == 45 || value == 95 || value == 126) {
				encodedData.append((char)value);
			}
			else {
				currentByte[0] = bytes[i];
				encodedData.append("%");
				encodedData.append(DatatypeConverter.printHexBinary(currentByte).toLowerCase());
			}
		}
		
		return encodedData.toString();
	}
}