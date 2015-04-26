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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class UnitConverter {
	
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	public static final String UTC_TIMEZONE = "UTC";

	/**
	 * Return a humanly readable presentation of input byte count
	 * 
	 * @param byteCount Number of bytes to format
	 * @return Formatted byteCount (either B, kiB, MiB or GiB)
	 */
	public static String formatByteCount(final long byteCount) {
        if(byteCount < 1024) {
        	return byteCount + " B";
        }
        final int unit = (63 - Long.numberOfLeadingZeros(byteCount)) / 10;
        return String.format("%.1f %sB", (double)byteCount / (1L << (unit * 10)), 
        		" KMGTPE".charAt(unit) + (unit > 0? "i" : ""));
    }
	
	/**
	 * Return a humanly readable presentation of time in milliseconds
	 * 
	 * @param timeMillis Time to format (in ms)
	 * @return
	 */
	public static String formatTime(final long timeMillis, final TimeZone timeZone) {
		DATE_FORMATTER.setTimeZone(timeZone);
		final Date timeAsDate = new Date(timeMillis);		
		return UnitConverter.DATE_FORMATTER.format(timeAsDate);
	}
	
	/**
	 * Convert a short digit to (Big Endian/Network byte-order) bytes
	 * 
	 * @param digit Digit to be converted
	 * @return Converted byte value (Big Endian)
	 */
	public static byte[] getBytes(final short digit) {
		final byte[] result = {(byte)(digit >> 8), (byte)digit};
		return result;
	}
	
	/**
	 * Convert an integer digit to (Big Endian/Network byte-order) bytes
	 * 
	 * @param digit Digit to be converted
	 * @return Converted byte value (Big Endian)
	 */
	public static byte[] getBytes(final int digit) {
		final byte[] result = {(byte)(digit >> 24),
		  (byte)(digit >> 16), (byte)(digit >> 8),
		  (byte)digit};
		return result;
	}
}
