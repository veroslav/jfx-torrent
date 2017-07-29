/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
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
package org.matic.torrent.preferences;

import org.matic.torrent.client.ClientProperties;

import java.io.File;

public final class PathProperties {
	
	//Standard Java system properties
	public static final String LINE_SEPARATOR = "line.separator";
	
	public static final String COMPLETED_DOWNLOADS = "paths.completed.downloads";	
	public static final String COMPLETED_TORRENTS = "paths.completed.torrents";
	public static final String NEW_DOWNLOADS = "paths.new.downloads";
	public static final String LOAD_TORRENTS = "paths.load.torrents";
	public static final String NEW_TORRENTS = "paths.new.torrents";
	public static final String KEY_STORE = "paths.key.store"; 
	
	public static final String COMPLETED_DOWNLOADS_SET = "paths.completed.downloads.set";	
	public static final String COMPLETED_TORRENTS_SET = "paths.completed.torrents.set";
	public static final String NEW_DOWNLOADS_SET = "paths.new.downloads.set";
	public static final String LOAD_TORRENTS_SET = "paths.load.torrents.set";
	public static final String NEW_TORRENTS_SET = "paths.new.torrents.set";
	
	public static final String MOVE_COMPLETED_DOWNLOADS_FROM_DEFAULT_SET = "paths.completed.downloads.move";
	public static final String DELETE_LOADED_TORRENTS_SET = "paths.load.torrents.delete";
	
	public static final String DEFAULT_STORE_TORRENTS_PATH = System.getProperty("user.home") + File.separator +
			"." + ClientProperties.CLIENT_NAME.toLowerCase() + File.separator;
}
