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

import org.matic.torrent.codec.BinaryEncodedDictionary;

public final class TrackerResponse {

	public enum Type {
		INVALID_URL, READ_WRITE_ERROR, OK, TRACKER_ERROR, INVALID_RESPONSE, WARNING
	}
	
	private final BinaryEncodedDictionary responseData;
	private final String message;
	private final Type type;
	
	public TrackerResponse(final Type type, final String message, final BinaryEncodedDictionary responseData) {
		this.responseData = responseData;
		this.message = message;
		this.type = type;
	}
	
	public TrackerResponse(final Type type, final String message) {
		this(type, message, null);
	}

	public BinaryEncodedDictionary getResponseData() {
		return responseData;
	}

	public String getMessage() {
		return message;
	}

	public Type getType() {
		return type;
	}	
}