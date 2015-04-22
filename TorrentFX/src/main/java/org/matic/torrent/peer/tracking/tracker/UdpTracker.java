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

package org.matic.torrent.peer.tracking.tracker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpRequest;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class UdpTracker extends Tracker {
	
	private static final int ANNOUNCE_LENGTH = 106;	//106 bytes
	private static final int ACTION_ANNOUNCE = 1;
	
	private static final int EVENT_STOPPED = 3;
	private static final int EVENT_STARTED = 2;
	private static final int EVENT_COMPLETED = 1;
	private static final int EVENT_NONE = 0;
	
	private static final int DEFAULT_PORT = 443;
	
	private final UdpConnectionManager connectionManager;	
	private long connectionId = 0;
	
	private final InetAddress trackerAddress;
	private final int trackerPort;
	
	public UdpTracker(final String url, final UdpConnectionManager connectionManager) 
			throws IOException, URISyntaxException {
		super(url);		
		this.connectionManager = connectionManager;
		
		final URI trackerUri = new URI(url);
		final int urlPort = trackerUri.getPort();
		
		trackerAddress = InetAddress.getByName(trackerUri.getHost());
		trackerPort = urlPort != -1? urlPort : DEFAULT_PORT;
	}
	
	@Override
	public final Type getType() {
		return Type.UDP;
	}
	
	@Override
	public boolean isScrapeSupported() {
		return true;
	}
	
	@Override
	public final void scrape(final Set<TrackableTorrent> torrents) {
		//TODO: Implement method
	};

	@Override
	protected final void announce(final AnnounceRequest announceRequest) {
		final UdpRequest udpRequest = buildUdpRequest(announceRequest);
		
		if(udpRequest != null) {			
			connectionManager.send(udpRequest);
		}
	}
	
	public final void setConnectionId(final long connectionId) {
		this.connectionId = connectionId;
	}
	
	public final long getConnectionId() {
		return connectionId;
	}
	
	private int getEventValue(final Tracker.Event event) {
		switch(event) {
		case STARTED:
			return EVENT_STARTED;
		case COMPLETED:
			return EVENT_COMPLETED;
		case STOPPED:
			return EVENT_STOPPED;
		default:
			return EVENT_NONE;	
		}
	}

	final UdpRequest buildUdpRequest(final AnnounceRequest announceRequest) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(ANNOUNCE_LENGTH);
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeLong(connectionId);
			dos.writeInt(ACTION_ANNOUNCE);
			dos.writeInt(announceRequest.getTorrent().getTransactionId());
			dos.write(announceRequest.getTorrent().getInfoHashBytes());
			dos.write(ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8.name()));
			dos.writeLong(announceRequest.getDownloaded());
			dos.writeLong(announceRequest.getLeft());
			dos.writeLong(announceRequest.getUploaded());
			dos.writeInt(getEventValue(announceRequest.getEvent()));
			
			//IP address (0 = default)
			dos.writeInt(0);			
			//key (should be calculated and sent)
			dos.writeInt(0);
			//num_want (-1 = default)
			dos.writeInt(-1);
			
			dos.writeShort(ClientProperties.TCP_PORT);			
			dos.flush();
		}
		catch(final IOException ioe) {
			return null;
		}
		
		return new UdpRequest(baos.toByteArray(), trackerAddress, trackerPort);
	}
}