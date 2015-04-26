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

public final class NetworkUtilities {

	public static final String HTTP_CONTENT_ENCODING = "Content-Encoding";
	public static final String HTTP_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
	
	public static final String HTTP_CONTENT_TYPE = "Content-Type";
	public static final String HTTP_TEXT_PLAIN = "text/plain";
	
	public static final int HTTP_CONNECTION_TIMEOUT = 5000; //5 seconds
	
	//TODO: Use a real user agent value
	public static final String HTTP_USER_AGENT_VALUE = 
			"Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0";
	public static final String HTTP_ACCEPT_CHARSET = "Accept-Charset";	
	public static final String HTTP_USER_AGENT_NAME = "User-Agent";	
	public static final String HTTP_GZIP_ENCODING = "gzip";
}
