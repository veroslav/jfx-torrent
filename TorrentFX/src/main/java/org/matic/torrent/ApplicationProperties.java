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

package org.matic.torrent;

import java.util.prefs.Preferences;

public final class ApplicationProperties {

	//Application preferences stored between the program sessions 
	private static final Preferences APPLICATION_PREFERENCES = 
			Preferences.userRoot().node(TorrentMain.class.getName());
	
	//Path to the self signed certificate store file
	public static final String CERTIFICATE_STORE_PATH = "certificate.store.path";
	
	//Prevent instantiation of this class
	private ApplicationProperties() {}
	
	/**
	 * Get a value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Default value to set and return if it is a new property
	 * @return
	 */
	public static String getProperty(final String propName, final String defaultValue) {
		return APPLICATION_PREFERENCES.get(propName, defaultValue);
	}
	
	/**
	 * Update existing och create a new application property with the given value
	 * 
	 * @param propName Property name
	 * @param propValue Property value
	 */
	public static void setProperty(final String propName, final String propValue) {
		if(propValue != null) {
			APPLICATION_PREFERENCES.put(propName, propValue);
		}
		else {
			APPLICATION_PREFERENCES.remove(propName);
		}		
	}
}
