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

package org.matic.torrent.tracking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.udp.UdpConnectionManager;
import org.matic.torrent.net.udp.UdpRequest;

public final class UdpTracker extends Tracker {
	
	private static final int ANNOUNCE_LENGTH = 106;	//106 bytes
	private static final int ACTION_CONNECT = 0;
	private static final int ACTION_ANNOUNCE = 1;
	
	private static final int STATE_STOPPED = 3;
	private static final int STATE_STARTED = 2;
	private static final int STATE_COMPLETED = 1;
	private static final int STATE_ACTIVE = 0;
	
	private static final int DEFAULT_PORT = 443;
	
	private final UdpConnectionManager connectionManager;	
	private final URI trackerUri;
	private long connectionId = 0;
	
	private final int trackerPort;
	
	public UdpTracker(final String url, final UdpConnectionManager connectionManager) 
			throws URISyntaxException {
		super(url);		
		this.connectionManager = connectionManager;
		this.trackerUri = new URI(url);
		
		final int urlPort = trackerUri.getPort();
		trackerPort = urlPort != -1? urlPort : DEFAULT_PORT;
	}
	
	@Override
	public Type getType() {
		return Type.UDP;
	}
	
	@Override
	public boolean isScrapeSupported() {
		return true;
	}
	
	@Override
	public void scrape(final Set<InfoHash> torrentInfoHashes) {
		//TODO: Implement method
	};

	@Override
	protected void announce(final AnnounceParameters announceParameters,
			final TorrentTracker trackedTorrent) {
		final UdpRequest udpRequest = buildUdpRequest(announceParameters, trackedTorrent.getInfoHash(),
				trackedTorrent.getTransactionId());
		
		if(udpRequest != null) {			
			connectionManager.send(udpRequest);
			trackedTorrent.setLastTrackerEvent(announceParameters.getTrackerEvent());
		}
	}
	
	public void setConnectionId(final long connectionId) {
		this.connectionId = connectionId;
	}
	
	public long getConnectionId() {
		return connectionId;
	}
	
	private int getState(final Tracker.Event event) {
		switch(event) {
		case STARTED:
			return STATE_STARTED;
		case COMPLETED:
			return STATE_COMPLETED;
		case STOPPED:
			return STATE_STOPPED;
		default:
			return STATE_ACTIVE;	
		}
	}

	UdpRequest buildUdpRequest(final AnnounceParameters announceParameters, final InfoHash infoHash,
			final int transactionId) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(ANNOUNCE_LENGTH);
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			//dos.writeLong(connectionId);
			dos.writeLong(0x41727101980L);
			
			dos.writeInt(ACTION_CONNECT);			
			
			//dos.writeInt(transactionId);
			dos.writeInt(38543);
			
			/*dos.write(infoHash.getBytes());
			dos.write(ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8.name()));
			dos.writeLong(announceParameters.getDownloaded());
			dos.writeLong(announceParameters.getLeft());
			dos.writeLong(announceParameters.getUploaded());
			dos.writeInt(getState(announceParameters.getTrackerEvent()));
			
			//IP address (0 = default)
			dos.writeInt(0);			
			//key (should be calculated and sent)
			dos.writeInt(0);
			//num_want (-1 = default)
			dos.writeInt(-1);
			
			dos.writeShort(ClientProperties.TCP_PORT);*/			
			dos.flush();
		}
		catch(final IOException ioe) {
			return null;
		}
		
		return new UdpRequest(baos.toByteArray(), trackerUri.getHost(), trackerPort);
	}
}