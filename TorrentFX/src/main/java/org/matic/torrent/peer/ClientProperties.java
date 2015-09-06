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

package org.matic.torrent.peer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientProperties {
	
	public enum Platform {
		Windows, Mac, Linux
	}
	
	private static final String CLIENT_IDENTIFIER = "-jX0001-";
	private static final String OS_PROPERTY_NAME = "os.name";
	
	public static final SecureRandom RANDOM_INSTANCE = new SecureRandom();
	private static final AtomicInteger ID_GENERATOR_BASE = new AtomicInteger(RANDOM_INSTANCE.nextInt());
	
	//Unique client id to be sent in tracker requests and to other peers
	public static final String PEER_ID = ClientProperties.generatePeerId();
	
	//Port used for incoming peer-2-peer connections
	public static final int TCP_PORT = 43893;	
	
	//UTF-8 encoding is used for all string encoding used in the client
	public static final Charset STRING_ENCODING_CHARSET = StandardCharsets.UTF_8;
	
	public static String getUserLocale() {
		final Locale currentLocale = Locale.getDefault();

		final StringBuilder userLocale = new StringBuilder(currentLocale.getLanguage());
		userLocale.append("-");
		userLocale.append(currentLocale.getCountry());
		
		return userLocale.toString();
	}
	
	/**
	 * Generate an unique integer on each call
	 * 
	 * @return Generated integer
	 */
	public static int generateUniqueId() {
		return ID_GENERATOR_BASE.incrementAndGet();		
	}	
	
	/**
	 * Get the name of the host operating system
	 * 
	 * @return Host platform 
	 */
	public static Platform getOS() {		
		final String osName = System.getProperty(OS_PROPERTY_NAME).toLowerCase();
 
		switch (osName) {
		case "win":
			return Platform.Windows;
		case "mac":
			return Platform.Mac;
		default:
			return Platform.Linux;
		}
	}
	
	/**
	 * Generate a random hexadecimal char sequence of specified length and case
	 * 
	 * @param length Target length
	 * @param upperCase Whether to generate an upper- or lower case sequence
	 * @return Generated hexadecimal sequence
	 */
	public static String generateRandomHexId(final int length, final boolean upperCase) {
        final StringBuffer buffer = new StringBuffer();
        while(buffer.length() < length){
            buffer.append(Integer.toHexString(ID_GENERATOR_BASE.incrementAndGet()));
        }

        final String result = buffer.toString().substring(0, length);
        return upperCase? result.toUpperCase() : result;
    }
	
	private static String generatePeerId() {
		final StringBuilder peerId = new StringBuilder(CLIENT_IDENTIFIER);
		peerId.append(generateRandomHexId(12, true));		
		return peerId.toString();
	}	
}