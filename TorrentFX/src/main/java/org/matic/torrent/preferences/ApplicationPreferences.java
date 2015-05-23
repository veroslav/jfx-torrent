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

package org.matic.torrent.preferences;

import java.util.prefs.Preferences;

/**
 * Helper class for managing application preferences storage and retrieval
 * 
 * @author vedran
 *
 */
public final class ApplicationPreferences {
	
	public static final String BOOLEAN_PROPERTY_UNSET = "0";
	public static final String BOOLEAN_PROPERTY_SET = "1";

	//Application preferences stored between the program sessions 
	private static final Preferences PREFERENCES = Preferences.userRoot().node(ApplicationPreferences.class.getName());
	
	//Prevent instantiation of this class
	private ApplicationPreferences() {}
	
	/**
	 * Get a value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Default value to set and return if it is a new property
	 * @return
	 */
	public static String getProperty(final String propName, final String defaultValue) {
		return PREFERENCES.get(propName, defaultValue);
	}
	
	/**
	 * Update existing or create a new application property with the given value
	 * 
	 * @param propName Property name
	 * @param propValue Property value
	 */
	public static void setProperty(final String propName, final String propValue) {
		if(propValue != null) {
			PREFERENCES.put(propName, propValue);
		}
		else {
			PREFERENCES.remove(propName);
		}		
	}
}