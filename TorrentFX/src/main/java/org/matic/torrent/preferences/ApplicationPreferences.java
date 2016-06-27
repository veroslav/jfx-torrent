/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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

import java.util.Arrays;
import java.util.List;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Helper class for managing application preferences storage and retrieval
 * 
 * @author vedran
 *
 */
public final class ApplicationPreferences {

	//Application preferences stored between the program sessions 
	private static final Preferences PREFERENCES = Preferences.userRoot().node(ApplicationPreferences.class.getName());
	
	//Prevent instantiation of this class
	private ApplicationPreferences() {}
	
	/**
	 * Add a listener to be notified when a value of a property changes.
	 * 
	 * @param listener Target listener
	 */
	public static void addPreferenceChangeListener(final PreferenceChangeListener listener) {
		PREFERENCES.addPreferenceChangeListener(listener);
	}
	
	/**
	 * Remove previously added property value change listener.
	 * 
	 * @param listener Target listener
	 */
	public static void removePreferenceChangeListener(final PreferenceChangeListener listener) {
		PREFERENCES.removePreferenceChangeListener(listener);
	}
	
	/**
	 * Get a boolean value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Property value or default value if not present
	 * @return
	 */
	public static boolean getProperty(final String propName, final boolean defaultValue) {
		return PREFERENCES.getBoolean(propName, defaultValue);
	}
	
	/**
	 * Update existing or create a new application property with the given boolean value
	 * 
	 * @param propName Property name
	 * @param propValue Property value
	 */
	public static void setProperty(final String propName, final boolean propValue) {
		PREFERENCES.putBoolean(propName, propValue);
	}
	
	/**
	 * Get a double value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Property value or default value if not present
	 * @return
	 */
	public static double getProperty(final String propName, final double defaultValue) {
		return PREFERENCES.getDouble(propName, defaultValue);
	}
	
	/**
	 * Update existing or create a new application property with the given double value
	 * 
	 * @param propName Property name
	 * @param propValue Property value
	 */
	public static void setProperty(final String propName, final double propValue) {
		PREFERENCES.putDouble(propName, propValue);
	}
	
	/**
	 * Get a long value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Property value or default value if not present
	 * @return
	 */
	public static long getProperty(final String propName, final long defaultValue) {
		return PREFERENCES.getLong(propName, defaultValue);
	}
	
	/**
	 * Update existing or create a new application property with the given long value
	 * 
	 * @param propName Property name
	 * @param propValue Property value
	 */
	public static void setProperty(final String propName, final long propValue) {
		PREFERENCES.putLong(propName, propValue);
	}
	
	/**
	 * Get a String value of an application property
	 * 
	 * @param propName Property name
	 * @param defaultValue Property value or default value if not present
	 * @return
	 */
	public static String getProperty(final String propName, final String defaultValue) {
		return PREFERENCES.get(propName, defaultValue);
	}
	
	/**
	 * Update existing or create a new application property with the given String value
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
	
	/**
	 * Get a property value of following form: <val1>:<val2>:...<valn>
	 * 
	 * @param propName Property name
	 * @param defaultValue Default property value
	 * @return A list of property value tokens
	 */
	public static List<String> getCompositePropertyValues(final String propName, final String defaultValue) {
		final String compositePropertyValue = ApplicationPreferences.getProperty(propName, defaultValue);		
		return Arrays.stream(compositePropertyValue.split(
				GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR)).collect(Collectors.toList());
	}
}