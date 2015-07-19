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

package org.matic.torrent.tracking;

public final class ScrapeStatistics {
	private final int downloaded;
	private final int incomplete;
	private final int complete;
	
	private final String name;

	/**
	 * Bean containing the result of an HTTP tracker torrent scrape
	 * 
	 * @param complete Number of peers with the entire file (seeders)
	 * @param downloaded Total number of times the tracker has registered a completion
	 * @param incomplete number of non-seeder peers (leechers)
	 * @param name The torrent's internal name (optional)
	 */
	public ScrapeStatistics(final int complete, final int downloaded,
			final int incomplete, final String name) {
		this.downloaded = downloaded;
		this.incomplete = incomplete;
		this.complete = complete;
		this.name = name;
	}

	public int getDownloaded() {
		return downloaded;
	}

	public int getIncomplete() {
		return incomplete;
	}

	public int getComplete() {
		return complete;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ScrapeStatistics [downloaded=" + downloaded + ", incomplete="
				+ incomplete + ", complete=" + complete + ", name=" + name
				+ "]";
	}		
}