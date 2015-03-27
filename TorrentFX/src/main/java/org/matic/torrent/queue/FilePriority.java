/* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
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

/**
 * Possible priorities for a file download
 * 
 * @author vedran
 *
 */
public enum FilePriority {
	SKIP(0), LOWEST(1), LOW(2), NORMAL(3), HIGH(4), HIGHEST(5), MIXED(6);
	
	private static final String[] NAMES = {"Skip", "Lowest",
		"Low", "Normal", "High", "Highest", "Mixed"};
	
	private final int value;
	
	FilePriority(final int priority) {
		this.value = priority;
	}
	
	public final int getValue() {
		return value;
	}

	@Override
	public final String toString() {	
		return FilePriority.NAMES[value];
	}

	public static String valueOf(int i) {
		return FilePriority.NAMES[i];
	}	
}