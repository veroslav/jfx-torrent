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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.udp.UdpRequest;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.utils.ResourceManager;

public final class UdpTracker extends Tracker {
		
	public static final int CONNECTION_ATTEMPT_DELAY = 15000;	//15 seconds
	public static final int MAX_CONNECTION_ATTEMPTS = 4;
	
	//Default UDP connectionId
	public static final long DEFAULT_CONNECTION_ID = 0x41727101980L;
	private static final int MESSAGE_LENGTH = 106;	//106 bytes
	
	public static final int ACTION_CONNECT = 0;
	public static final int ACTION_ANNOUNCE = 1;
	public static final int ACTION_SCRAPE = 2;
	public static final int ACTION_ERROR = 3;
	
	private static final int EVENT_STOPPED = 3;
	private static final int EVENT_STARTED = 2;
	private static final int EVENT_COMPLETED = 1;
	private static final int EVENT_ACTIVE = 0;
	
	private static final int DEFAULT_PORT = 443;
		
	private final AtomicLong connectionId = new AtomicLong(DEFAULT_CONNECTION_ID);
	private final AtomicInteger connectionAttempts = new AtomicInteger(0);
	private final AtomicLong lastConnectionAttempt = new AtomicLong(0);
	
	private final URI trackerUri;
	private final int trackerPort;
	
	public UdpTracker(final String url) 
			throws URISyntaxException {
		super(url);		
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
	protected void scrape(final TrackerSession... trackerSessions) {
		if(trackerSessions.length == 0 || connectionId.get() == DEFAULT_CONNECTION_ID) {
			return;
		}
		final Set<TrackerSession> matchingTorrents = Arrays.stream(trackerSessions).filter(
				t -> t.getTracker().equals(this)).collect(Collectors.toSet());
		final UdpRequest scrapeRequest = buildScrapeRequest(matchingTorrents);
		
		if(scrapeRequest != null) {
			ResourceManager.INSTANCE.getUdpTrackerConnectionManager().send(scrapeRequest);
		}
	};

	@Override
	protected void announce(final AnnounceParameters announceParameters,
			final TrackerSession trackerSession) {
		final UdpRequest announceRequest = buildAnnounceRequest(announceParameters, trackerSession.getInfoHash(),
				trackerSession.getTransactionId());
		
		if(announceRequest != null) {
			trackerSession.setLastTrackerEvent(announceParameters.getTrackerEvent());
			ResourceManager.INSTANCE.getUdpTrackerConnectionManager().send(announceRequest);
		}
	}
	
	@Override
	protected int connect(final int transactionId) {
		final int connectionAttempt = connectionAttempts.updateAndGet((value) -> {			
			if(System.currentTimeMillis() - lastConnectionAttempt.get() < 
					CONNECTION_ATTEMPT_DELAY) {
				return value;
			}
			else {
				lastConnectionAttempt.set(System.currentTimeMillis());
				return ++value;
			}
		});
		if(connectionAttempt <= MAX_CONNECTION_ATTEMPTS) {
			final UdpRequest udpRequest = buildConnectionRequest(transactionId);
			
			if(udpRequest != null) {
				ResourceManager.INSTANCE.getUdpTrackerConnectionManager().send(udpRequest);		
			}
		}
		return connectionAttempt;
	}
	
	@Override
	public void setLastResponse(final long lastResponse) {
		super.setLastResponse(lastResponse);
		connectionAttempts.set(0);
	}
	
	@Override
	public void setLastScrape(final long lastScrape) {	
		super.setLastScrape(lastScrape);
		connectionAttempts.set(0);
	}

	@Override
	public long getId() {
		return connectionId.get();
	}
	
	@Override
	public void setId(final long connectionId) {
		this.connectionId.set(connectionId);
	}
	
	private int toRequestEvent(final Tracker.Event event) {
		switch(event) {
		case STARTED:
			return EVENT_STARTED;
		case COMPLETED:
			return EVENT_COMPLETED;
		case STOPPED:
			return EVENT_STOPPED;
		default:
			return EVENT_ACTIVE;	
		}
	}
	
	protected UdpRequest buildConnectionRequest(final int transactionId) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(MESSAGE_LENGTH);
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeLong(DEFAULT_CONNECTION_ID);
			dos.writeInt(ACTION_CONNECT);			
			dos.writeInt(transactionId);
		}
		catch(final IOException ioe) {
			return null;
		}
		return new UdpRequest(UdpRequest.Type.TRACKER, transactionId, 
				baos.toByteArray(), trackerUri.getHost(), trackerPort);
	}

	protected UdpRequest buildAnnounceRequest(final AnnounceParameters announceParameters, final InfoHash infoHash,
			final int transactionId) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(MESSAGE_LENGTH);
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			
			final byte[] clientId = ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8.name());
			final int requestEvent = toRequestEvent(announceParameters.getTrackerEvent());
		
			dos.writeLong(connectionId.get());
			dos.writeInt(ACTION_ANNOUNCE);			
			dos.writeInt(transactionId);			
			dos.write(infoHash.getBytes());
			dos.write(clientId);
			dos.writeLong(announceParameters.getDownloaded());
			dos.writeLong(announceParameters.getLeft());
			dos.writeLong(announceParameters.getUploaded());
			dos.writeInt(requestEvent);
			
			//IP address (0 = default)
			dos.writeInt(0);			
			//key (TODO: should be calculated and sent)
			dos.writeInt(42);
			//num_want (-1 = default)
			dos.writeInt(requestEvent != EVENT_STOPPED? NUM_WANTED_PEERS : 0);
			
			dos.writeShort(ResourceManager.INSTANCE.getUdpTrackerPort());			
			dos.flush();
		}
		catch(final IOException ioe) {
			return null;
		}		
		return new UdpRequest(UdpRequest.Type.TRACKER, transactionId,
				baos.toByteArray(), trackerUri.getHost(), trackerPort);
	}
	
	protected UdpRequest buildScrapeRequest(final Set<TrackerSession> trackerSessions) {	
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(MESSAGE_LENGTH + (trackerSessions.size() * 20));
		try(final DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeLong(connectionId.get());
			dos.writeInt(ACTION_SCRAPE);
			dos.writeInt(super.getScrapeTransactionId());
			
			for(final TrackerSession trackerSession : trackerSessions) {
				dos.write(trackerSession.getInfoHash().getBytes());
			}
		}
		catch(final IOException ioe) {
			return null;
		}
		return new UdpRequest(UdpRequest.Type.TRACKER, super.getScrapeTransactionId(),
				baos.toByteArray(), trackerUri.getHost(), trackerPort);
	}

	@Override
	public String toString() {
		return "UdpTracker [connectionId=" + connectionId + ", url=" + getUrl()
				+ "]";
	}
}