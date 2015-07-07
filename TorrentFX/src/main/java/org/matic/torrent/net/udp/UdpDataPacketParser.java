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
import org.matic.torrent.codec.BinaryDecoderException;
import org.matic.torrent.tracking.UdpTracker;
import org.matic.torrent.tracking.methods.dht.DhtResponse;

public final class UdpDataPacketParser {
	
	private static final int ANNOUNCE_RESPONSE_MIN_MESSAGE_LENGTH = 160;	
	private static final int CONNECTION_RESPONSE_MESSAGE_LENGTH = 128;	
	private static final int SCRAPE_RESPONSE_MIN_MESSAGE_LENGTH = 64;

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
		
		System.out.println("responseData[].length: " + responseData.length);
		
		try(final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(responseData))) {	
			/*if(responseData.length < Integer.BYTES) {
				return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR);
			}*/
			
			final int actionId = dis.readInt();
			
			System.out.println("PARSER: parsed message with id: " + actionId);
			
			//Check whether it is a response to a connection request			
			if(//responseData.length == CONNECTION_RESPONSE_MESSAGE_LENGTH &&
					actionId == UdpTracker.ACTION_CONNECT) {
				return new UdpTrackerResponse(responseData, UdpTracker.ACTION_CONNECT);
			}
			//Check whether it is a response to an announce request			
			else if(//responseData.length >= ANNOUNCE_RESPONSE_MIN_MESSAGE_LENGTH &&
					actionId == UdpTracker.ACTION_ANNOUNCE) {
				return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ANNOUNCE);
			}
			//Check whether it is a response to a scrape request 
			else if(//responseData.length >= SCRAPE_RESPONSE_MIN_MESSAGE_LENGTH &&
					actionId == UdpTracker.ACTION_SCRAPE) {
				return new UdpTrackerResponse(responseData, UdpTracker.ACTION_SCRAPE);
			}
			
			return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR);
			
		} catch (final IOException ioe) {
			return new UdpTrackerResponse(responseData, UdpTracker.ACTION_ERROR);
		}
	}
}