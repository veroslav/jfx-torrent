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

package org.matic.torrent.net;

import org.matic.torrent.peer.ClientProperties;

public final class NetworkUtilities {

	public static final String HTTP_CONTENT_ENCODING = "Content-Encoding";
	public static final String HTTP_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
	
	public static final String HTTP_CONTENT_TYPE = "Content-Type";
	public static final String HTTP_TEXT_PLAIN = "text/plain";
	
	public static final int HTTP_CONNECTION_TIMEOUT = 5000; //5 seconds
	
	private static final String HTTP_USER_AGENT_VALUE = buildHttpUserAgentValue(); 
			
	public static final String HTTP_ACCEPT_CHARSET = "Accept-Charset";	
	public static final String HTTP_USER_AGENT_NAME = "User-Agent";	
	public static final String HTTP_GZIP_ENCODING = "gzip";
	
	public static String getHttpUserAgent() {
		return HTTP_USER_AGENT_VALUE;
	}
	
	private static String buildHttpUserAgentValue() {
		final StringBuilder userAgent = new StringBuilder("Mozilla/5.0 (");
		switch(ClientProperties.getOS()) {
		case Windows:
			userAgent.append("Windows NT 6.3");
			break;
		case Mac:
			userAgent.append("Macintosh; Intel Mac OS X 10_10");
			break;
		default:
			userAgent.append("X11; Linux x86_64");
		}
		userAgent.append("; rv:36.0) Gecko/20100101 Firefox/37.0");
		return userAgent.toString();
	}
}
