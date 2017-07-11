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
package org.matic.torrent.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class UnitConverter {
	
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    private static final NumberFormat DOUBLE_FORMATTER = new DecimalFormat("#0.0");
	public static final String UTC_TIMEZONE = "UTC";

    /**
     * Reverse all bits in a byte array. This is useful when calling BitSet.valueOf(byte[]) to
     * initialize a remote peer's bitfield. The bitfield itself is Big-endian, while BitSet
     * uses Little-endian. Thus, bytes are reversed with this method before calling BitSet.valueOf(byte[])
     *
     * @param array Array that will have its bits reversed for each byte
     * @return Reversed byte array
     */
    public static byte[] reverseBits(final byte[] array) {
        final byte[] reversed = new byte[array.length];
        for(int i = 0; i < array.length; ++i) {
            byte in = array[i];
            byte out = 0;
            for (int ii = 0; ii < 8; ii++) {
                final byte bit = (byte) (in & 1);
                out = (byte)((out << 1) | bit);
                in = (byte)(in >> 1);
            }
            reversed[i] = out;
        }
        return reversed;
    }

    /**
     * Simple formatter for floating point numbers. Mostly used to have a uniform number
     * presentation across the application.
     *
     * @param number Floating point number to be formatted
     * @return Formatted number
     */
    public static String formatDouble(final double number) {
        return DOUBLE_FORMATTER.format(number);
    }

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
	 * Return a humanly readable date presentation of milliseconds
	 * 
	 * @param timeMillis Time to format (in ms)
	 * @param timeZone Time zone to use for formatting
	 * @return Date formatted to humanly readable representation
	 */
	public static String formatMillisToDate(final long timeMillis, final TimeZone timeZone) {
		DATE_FORMATTER.setTimeZone(timeZone);
		final Date timeAsDate = new Date(timeMillis);		
		return UnitConverter.DATE_FORMATTER.format(timeAsDate);
	}
	
	/**
	 * Return a humanly readable time presentation of milliseconds
	 * 
	 * @param timeMillis Time to format (in ms)
	 * @return Time formatted to humanly readable representation
	 */
	public static String formatMillisToTime(final long timeMillis) {
		long millis = timeMillis;
		
		final long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		
        final StringBuilder result = new StringBuilder();
        
        if(days > 0) {
        	result.append(days);
        	result.append("d ");
        }
        if(hours > 0) {
        	result.append(hours);
        	result.append("h ");
        }
        if(minutes > 0) {
        	result.append(minutes);
        	result.append("m ");
        }

    	result.append(seconds);
    	result.append("s");
        
        return result.toString();
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

    /**
     * Convert bytes (Big Endian/Network byte-order) into an integer digit.
     *
     * @param bytes Byte array to be converted
     * @return Converted integer value (Big Endian)
     */
    public static int getInt(final byte[] bytes) {
        return bytes[3] & 0xFF | (bytes[2] & 0xFF) << 8
                | (bytes[1] & 0xFF) << 16 | (bytes[0] & 0xFF) << 24;
    }
}