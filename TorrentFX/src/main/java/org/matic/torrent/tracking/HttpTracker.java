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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.codec.BinaryDecoderException;
import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeyNames;
import org.matic.torrent.hash.HashUtilities;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.tracking.listeners.HttpTrackerResponseListener;

public final class HttpTracker extends Tracker {
	
	private static final String EVENT_COMPLETED = "completed";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_STARTED = "started";	
	
	private static final String REQUEST_TYPE_ANNOUNCE = "announce";
	private static final String REQUEST_TYPE_SCRAPE = "scrape";
	
	private final HttpTrackerResponseListener responseListener;
	private final BinaryDecoder decoder;
	private final String scrapeUrl;
	
	public HttpTracker(final String url, final HttpTrackerResponseListener responseListener) {
		super(url);				
		this.responseListener = responseListener;
		decoder = new BinaryDecoder();
		
		final int lastSlashIndex = url.lastIndexOf('/');
		final boolean scrapeSupported = url.startsWith(REQUEST_TYPE_ANNOUNCE, lastSlashIndex + 1);
		
		scrapeUrl = scrapeSupported? url.replace(REQUEST_TYPE_ANNOUNCE, REQUEST_TYPE_SCRAPE) : null;
	}
	
	@Override
	public boolean isScrapeSupported() {
		return scrapeUrl != null;
	}
	
	@Override
	protected final void scrape(final Set<TrackerSession> trackerSessions) {
		 //TODO: Implement method
	};
	
	@Override
	protected void connect(final int transactionId) {
		//Connection establishment is not supported by HTTP trackers
	}
	
	@Override
	public final Type getType() {
		return Type.TCP;
	}
	
	@Override
	public long getId() {
		//Not supported by HTTP trackers
		return 0;
	}
	
	@Override
	public void setId(final long id) {
		//Not supported by HTTP trackers
	}

	@Override
	protected final void announce(final AnnounceParameters announceParameters, final TrackerSession trackerSession) {				
		final AnnounceResponse trackerResponse = sendRequest(announceParameters, trackerSession);
		responseListener.onAnnounceResponseReceived(trackerResponse, trackerSession);				
	}
	
	protected String getScrapeUrl() {
		return scrapeUrl;
	}
	
	private String getEventName(final Tracker.Event event) {
		switch(event) {
		case STARTED:
			return EVENT_STARTED;
		case COMPLETED:
			return EVENT_COMPLETED;
		case STOPPED:
			return EVENT_STOPPED;
		default:
			return null;
		}
	}
	
	protected String buildRequestUrl(final AnnounceParameters announceParameters, final InfoHash infoHash) 
			throws UnsupportedEncodingException {
				
		final StringBuilder result = new StringBuilder(super.getUrl());
		result.append("?info_hash=");
		result.append(HashUtilities.urlEncodeBytes(infoHash.getBytes()));
		result.append("&peer_id=");
		result.append(URLEncoder.encode(
				ClientProperties.PEER_ID, 
				StandardCharsets.UTF_8.name()));
		
		result.append("&port=");
		result.append(ClientProperties.TCP_PORT);
		result.append("&uploaded=");
		result.append(announceParameters.getUploaded());
		result.append("&downloaded=");
		result.append(announceParameters.getDownloaded());
		result.append("&left=");
		result.append(announceParameters.getLeft());
		result.append("&corrupt=0&key=6F187D4A");
		
		final Event trackerEvent = announceParameters.getTrackerEvent();
		final String eventName = getEventName(trackerEvent);
		if(eventName != null) {
			result.append("&event=");
			result.append(eventName);
		}
		
		result.append("&numwant=");
		result.append(trackerEvent != Event.STOPPED? NUM_WANTED_PEERS : 0);
		result.append("&compact=1&no_peer_id=1"); //&supportcrypto=1&redundant=0");
		
		return result.toString();
	}
	
	//TODO: Add proxy support for the request
	private AnnounceResponse sendRequest(final AnnounceParameters announceParameters,
			final TrackerSession trackerSession) {			
		URL targetUrl = null;
		
		try {
			targetUrl = new URL(buildRequestUrl(announceParameters, trackerSession.getInfoHash()));			
			
			HttpURLConnection.setFollowRedirects(false);
			final HttpURLConnection connection = (HttpURLConnection)targetUrl.openConnection();			
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
			connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME, NetworkUtilities.getHttpUserAgent());			
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_ENCODING, NetworkUtilities.HTTP_GZIP_ENCODING);
			connection.setConnectTimeout(NetworkUtilities.HTTP_CONNECTION_TIMEOUT);
			
			final int responseCode = connection.getResponseCode();
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				try(final InputStream responseStream = connection.getInputStream()) {					
					/*final String contentType = connection.getHeaderField(NetworkUtilities.HTTP_CONTENT_TYPE);
					if(contentType == null) {
						return new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, "Response content type not set");
					}*/
					
					//Check whether the response stream is gzip encoded
					final String contentEncoding = connection.getHeaderField(NetworkUtilities.HTTP_CONTENT_ENCODING);
					
					final BinaryEncodedDictionary responseMap = contentEncoding != null && 
							NetworkUtilities.HTTP_GZIP_ENCODING.equals(contentEncoding)? decoder.decodeGzip(responseStream) :
								decoder.decode(responseStream);
					final AnnounceResponse trackerResponse = buildResponse(
							responseMap, trackerSession.getInfoHash());
					trackerSession.setLastTrackerEvent(announceParameters.getTrackerEvent());
					return trackerResponse;
				}				
			}
			else if(responseCode >= HttpURLConnection.HTTP_BAD_REQUEST &&
					responseCode <= HttpURLConnection.HTTP_GATEWAY_TIMEOUT){
				return buildErrorResponse(connection.getErrorStream());
			}
			return new AnnounceResponse(AnnounceResponse.Type.READ_WRITE_ERROR, "Error response: code " + responseCode);
		}
		catch(final BinaryDecoderException bde) {
			return new AnnounceResponse(AnnounceResponse.Type.INVALID_RESPONSE, bde.getMessage());
		}
		catch(final MalformedURLException mue) {
			return new AnnounceResponse(AnnounceResponse.Type.INVALID_URL, mue.getMessage());
		}
		catch(final IOException ioe) {
			return new AnnounceResponse(AnnounceResponse.Type.READ_WRITE_ERROR, ioe.getMessage());
		}
	}
	
	protected AnnounceResponse buildErrorResponse(final InputStream errorStream) throws IOException {
		final StringBuilder errorMessage = new StringBuilder();
		
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(
				errorStream, StandardCharsets.UTF_8.name()))) {
			errorMessage.append(reader.readLine());
		}
		
		return new AnnounceResponse(AnnounceResponse.Type.TRACKER_ERROR, errorMessage.toString());
	}
	
	protected AnnounceResponse buildResponse(final BinaryEncodedDictionary responseMap, final InfoHash infoHash) {
		final BinaryEncodedString failureReason = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_FAILURE_REASON);
		
		if(failureReason != null) {
			return new AnnounceResponse(AnnounceResponse.Type.TRACKER_ERROR, failureReason.toString());
		}
		
		final BinaryEncodable peerList = responseMap.get(BinaryEncodingKeyNames.KEY_PEERS);
		
		final BinaryEncodedInteger interval = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INTERVAL));
		
		final BinaryEncodedInteger complete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_COMPLETE));
		
		final BinaryEncodedInteger incomplete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INCOMPLETE));
		
		if(!validateMandatoryResponseValues(peerList, interval, complete, incomplete)) {			
			return new AnnounceResponse(AnnounceResponse.Type.INVALID_RESPONSE, 
					"Missing mandatory response value");
		}
		
		final long incompleteValue = incomplete.getValue();
		final long intervalValue = interval.getValue();
		final long completeValue = complete.getValue();
		
		final BinaryEncodedString warningMessage = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_WARNING_MESSAGE);
		
		final AnnounceResponse.Type responseType = warningMessage != null? 
				AnnounceResponse.Type.WARNING : AnnounceResponse.Type.OK;
		final String trackerMessage = warningMessage != null? warningMessage.toString() : null;
		
		final BinaryEncodedInteger minInterval = (BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_MIN_INTERVAL);
		final Long minIntervalValue = minInterval != null? minInterval.getValue() : null;
		
		final BinaryEncodedString trackerId = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_TRACKER_ID);
		final String trackerIdValue = trackerId != null? trackerId.toString() : null;
		
		final Set<PwpPeer> peers = extractPeers(peerList, infoHash);
		
		return new AnnounceResponse(responseType, trackerMessage, intervalValue, minIntervalValue, trackerIdValue,
				completeValue, incompleteValue, peers);
	}
	
	protected Set<PwpPeer> extractPeers(final BinaryEncodable peerList, final InfoHash infoHash) {				
		return peerList instanceof BinaryEncodedList? extractPeerList(
				(BinaryEncodedList)peerList, infoHash) : extractPeerString((BinaryEncodedString)peerList, infoHash);
	}
	
	protected Set<PwpPeer> extractPeerList(final BinaryEncodedList peerList, final InfoHash infoHash) {
		return peerList.stream().map(p -> {
			final BinaryEncodedDictionary peerInfo = (BinaryEncodedDictionary)p;
			//TODO: Extract peer id, do we need it?
			final String peerIp = ((BinaryEncodedString)peerInfo.get(BinaryEncodingKeyNames.KEY_IP)).toString();
			final long peerPort = ((BinaryEncodedInteger)peerInfo.get(BinaryEncodingKeyNames.KEY_PORT)).getValue();
			return new PwpPeer(peerIp, (int)peerPort, infoHash);
		}).collect(Collectors.toSet());
	}
	
	protected Set<PwpPeer> extractPeerString(final BinaryEncodedString peerString, final InfoHash infoHash) {
		final Set<PwpPeer> peers = new HashSet<>();
		final byte[] peerInfo = peerString.getBytes();
		for(int i = 0; i < peerInfo.length; i += 6) {
			final byte[] rawIp = {peerInfo[i], peerInfo[i + 1], peerInfo[i + 2], peerInfo[i + 3]};
			int peerPort = 0;
			peerPort += ((peerInfo[i + 4] & 0xff)) << 8;
			peerPort += ((peerInfo[i + 5] & 0xff));
			
			try {
				final InetAddress peerIp = InetAddress.getByAddress(rawIp);
				peers.add(new PwpPeer(peerIp.getHostAddress(), peerPort, infoHash));
			} 
			catch(final UnknownHostException uhe) {
				//Invalid peer ip format, simply discard this peer
				continue;
			}
		}
		
		return peers;
	}
	
	private boolean validateMandatoryResponseValues(final BinaryEncodable... values) {
		return !Arrays.stream(values).anyMatch(v -> v == null);		
	}

	@Override
	public String toString() {
		return "HttpTracker [url=" + url + "]";
	}
}