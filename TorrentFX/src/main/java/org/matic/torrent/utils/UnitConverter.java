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

public final class UnitConverter {
	
	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

	/**
	 * Return a humanly readable presentation of input byte count
	 * 
	 * @param v Number of bytes to format
	 * @return
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
	public static String formatTime(final long timeMillis) {
		final Date timeAsDate = new Date(timeMillis);		
		return UnitConverter.DATE_FORMATTER.format(timeAsDate);
	}
}
