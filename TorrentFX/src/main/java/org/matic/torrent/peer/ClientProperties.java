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

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public final class ClientProperties {
	
	private static final char[] LETTER_HEX_VALUES = {'A', 'B', 'C', 'D', 'E', 'F'};
	
	private static final AtomicLong ID_GENERATOR_BASE = new AtomicLong(System.nanoTime());
	private static final String CLIENT_IDENTIFIER = "-jX0001-";
	
	//Unique client id to be sent in tracker requests and to other peers
	public static final String PEER_ID = ClientProperties.generatePeerId();
	
	//Port used for incoming peer-2-peer connections
	public static final int TCP_PORT = 43893;
	
	//Port used for UDP communication (tracker and DHT responses)
	public static final int UDP_PORT = 43893;
	
	//UTF-8 encoding is used for all string encoding used in the client
	public static final Charset STRING_ENCODING_CHARSET = StandardCharsets.UTF_8;
	
	public static String getUserLocale() {
		final Locale currentLocale = Locale.getDefault();

		final StringBuilder userLocale = new StringBuilder(currentLocale.getLanguage());
		userLocale.append("-");
		userLocale.append(currentLocale.getCountry());
		
		return userLocale.toString();
	}
	
	public static int generateUniqueId() {
		final StringBuilder transactionId = new StringBuilder(ClientProperties.getUniqueHashBase());
		transactionId.append(ID_GENERATOR_BASE.incrementAndGet());
		
		return transactionId.toString().hashCode();
	}	
	
	private static String getUniqueHashBase() {
		final Properties systemProps = System.getProperties();		
		final StringBuilder hashBase = new StringBuilder();
		
		hashBase.append(systemProps.getProperty("os.name"));
		hashBase.append(systemProps.getProperty("os.arch"));
		hashBase.append(systemProps.getProperty("os.version"));
		hashBase.append(systemProps.getProperty("user.name"));
		hashBase.append(systemProps.getProperty("user.home"));
		hashBase.append(systemProps.getProperty("user.dir"));
		hashBase.append(ManagementFactory.getRuntimeMXBean().getName());
		
		return hashBase.toString();
	}
	
	private static String generatePeerId() {
		final String uniqueHash = String.valueOf(Math.abs(generateUniqueId()));
		final StringBuilder peerId = new StringBuilder(CLIENT_IDENTIFIER);
		final Random random = new Random(System.nanoTime());
		
		for(int i = 0; i < 12 - uniqueHash.length(); ++i) {
			peerId.append(LETTER_HEX_VALUES[random.nextInt(LETTER_HEX_VALUES.length)]);
		}
		
		peerId.append(uniqueHash);
		return peerId.toString();
	}	
}