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

package org.matic.torrent.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HashUtilities {
	
	public static final int HEX_INFO_HASH_LENGTH = 40;	//160 bits / 4 bytes == 40 places
	
	private static final Pattern HEX_MATCH_PATTERN = Pattern.compile("[0-9a-fA-F]+");
	private static final char[] HEX_VALUES = "0123456789ABCDEF".toCharArray();

	/**
	 * Convert (SHA-1) byte value to it's hexadecimal representation
	 * 
	 * @param bytes Bytes to be converted
	 * @return Hexadecimal representation of input bytes
	 */
	public static String convertToHexValue(final byte[] bytes) {
		final char[] hexChars = new char[bytes.length * 2];
	    for(int j = 0; j < bytes.length; j++) {
	        final int v = bytes[j] & 0xFF;
	        hexChars[j*2] = HEX_VALUES[v >>> 4];
	        hexChars[j*2+1] = HEX_VALUES[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
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
}