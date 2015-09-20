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

package org.matic.torrent.net.udp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.exception.BinaryDecoderException;
import org.matic.torrent.tracking.UdpTracker;
import org.matic.torrent.tracking.methods.dht.DhtResponse;

public final class UdpDataPacketParser {
	
	private static final int MIN_CONNECTION_RESPONSE_MESSAGE_LENGTH = 16;	
	private static final int MIN_ANNOUNCE_RESPONSE_MESSAGE_LENGTH = 20;	
	private static final int MIN_SCRAPE_RESPONSE_MESSAGE_LENGTH = 8;

	public static DhtResponse parseDHTResponse(final byte[] packetData) {
		final BinaryDecoder decoder = new BinaryDecoder();
		try {
			return new DhtResponse(decoder.decode(new ByteArrayInputStream(packetData)));
		} catch (final IOException | BinaryDecoderException e) {
			//This was not a DHT protocol message, simply return null
			return null;
		}
	}
	
	public static UdpTrackerResponse parseTrackerResponse(final byte[] responseData) {		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(responseData))) {	
			
			final int responseLength = responseData.length;
			final int actionId = dis.readInt();
			
			//Check whether it is a response to a connection request			
			if(actionId == UdpTracker.ACTION_CONNECT) {
				if(responseLength >= MIN_CONNECTION_RESPONSE_MESSAGE_LENGTH) {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_CONNECT, null);
				}
				else {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR,
							"Connection response too short: " + responseLength);
				}
			}
			//Check whether it is a response to an announce request			
			else if(actionId == UdpTracker.ACTION_ANNOUNCE) {
				if(responseLength >= MIN_ANNOUNCE_RESPONSE_MESSAGE_LENGTH) {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ANNOUNCE, null);
				}
				else {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR,
							"Announce response too short: " + responseLength);
				}
			}
			//Check whether it is a response to a scrape request 
			else if(actionId == UdpTracker.ACTION_SCRAPE) {
				if(responseLength >= MIN_SCRAPE_RESPONSE_MESSAGE_LENGTH) {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_SCRAPE, null);
				}
				else {
					return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR,
							"Scrape response too short: " + responseLength);
				}
			}
			
			return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR, "No matching message type was found");
			
		} catch (final IOException ioe) {
			return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR, ioe.getMessage());
		}
	}
}