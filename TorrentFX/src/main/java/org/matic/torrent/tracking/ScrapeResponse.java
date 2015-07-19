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

import java.util.Collections;
import java.util.Map;

import org.matic.torrent.tracking.TrackerResponse.Type;

public final class ScrapeResponse {

	private final Map<TrackerSession, ScrapeStatistics> scrapeStatistics;
	private final Map<String, String> flags;

	private final String message;
	private final Type type;

	/**
	 * Constructor for building an error response
	 * 
	 * @param type Type of tracker error
	 * @param errorMessage Error message detailing the error
	 */
	public ScrapeResponse(final Type type, final String errorMessage) {		
		this(type, errorMessage, Collections.emptyMap(), Collections.emptyMap());
	}
	
	/**
	 * Constructor for building normal scrape response
	 * 
	 * @param type Type of response (OK)
	 * @param message Any tracker message (usually absent)
	 * @param flags miscellaneous tracker flags
	 * @param scrapeStatistics Obtained tracker statistics mapped to torrent sessions
	 */
	public ScrapeResponse(final Type type, final String message, final Map<String, String> flags,
			final Map<TrackerSession, ScrapeStatistics> scrapeStatistics) {
		this.scrapeStatistics = scrapeStatistics;
		this.flags = flags;
		this.message = message;
		this.type = type;
	}

	public Map<TrackerSession, ScrapeStatistics> getScrapeStatistics() {
		return scrapeStatistics;
	}

	public Map<String, String> getFlags() {
		return flags;
	}

	public String getMessage() {
		return message;
	}

	public Type getType() {
		return type;
	}
}