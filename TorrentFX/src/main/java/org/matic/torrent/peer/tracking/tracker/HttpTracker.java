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

import org.matic.torrent.io.codec.BinaryDecoder;
import org.matic.torrent.io.codec.BinaryDecoderException;
import org.matic.torrent.io.codec.BinaryEncodable;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedInteger;
import org.matic.torrent.io.codec.BinaryEncodedList;
import org.matic.torrent.io.codec.BinaryEncodedString;
import org.matic.torrent.io.codec.BinaryEncodingKeyNames;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class HttpTracker extends Tracker {
	
	private static final String EVENT_COMPLETED = "completed";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_STARTED = "started";	
	
	private static final String REQUEST_TYPE_ANNOUNCE = "announce";
	private static final String REQUEST_TYPE_SCRAPE = "scrape";
	
	private final TrackerResponseListener responseListener;
	private final BinaryDecoder decoder;
	private final String scrapeUrl;
	
	public HttpTracker(final String url, final TrackerResponseListener responseListener) {
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
	protected final void scrape(final Set<TrackableTorrent> torrents) {
		 //TODO: Implement method
	};
	
	@Override
	public final Type getType() {
		return Type.TCP;
	}

	@Override
	protected final void announce(final AnnounceRequest announceRequest) {				
		final TrackerResponse trackerResponse = sendRequest(announceRequest);
		responseListener.onResponseReceived(trackerResponse, this);				
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
	
	protected String buildRequestUrl(final AnnounceRequest announceRequest) 
			throws UnsupportedEncodingException {		
		final TrackableTorrent torrent = announceRequest.getTorrent();
		final String urlTemplate = "info_hash=%s&peer_id=%s&uploaded=%d&downloaded=%d&left=%d&port=%d&compact=1";		
		final StringBuilder result = new StringBuilder(url);
				result.append("?");
				result.append(String.format(urlTemplate, URLEncoder.encode(
				torrent.getInfoHashHexValue(), StandardCharsets.UTF_8.name()),
				URLEncoder.encode(ClientProperties.PEER_ID, StandardCharsets.UTF_8.name()),
				announceRequest.getUploaded(), announceRequest.getDownloaded(),
				announceRequest.getLeft(), ClientProperties.TCP_PORT));
		
		final String eventName = getEventName(announceRequest.getEvent());
		if(eventName != null) {
			result.append("?event=");
			result.append(eventName);
		}
		
		return result.toString();
	}
	
	//TODO: Add proxy support for the request
	private TrackerResponse sendRequest(final AnnounceRequest announceRequest) {			
		URL targetUrl = null;
		
		try {
			targetUrl = new URL(buildRequestUrl(announceRequest));			
			final HttpURLConnection connection = (HttpURLConnection)targetUrl.openConnection();
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
			connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME, NetworkUtilities.HTTP_USER_AGENT_VALUE);			
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_ENCODING, NetworkUtilities.HTTP_GZIP_ENCODING);
			
			final int responseCode = connection.getResponseCode();
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				final InputStream responseStream = connection.getInputStream();
				
				final String contentType = connection.getHeaderField(NetworkUtilities.HTTP_CONTENT_TYPE);
				if(contentType == null || !NetworkUtilities.HTTP_TEXT_PLAIN.equals(contentType)) {
					return new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, "Not a text/plain response");
				}
				
				//Check whether the response stream is gzip encoded
				final String contentEncoding = connection.getHeaderField(NetworkUtilities.HTTP_CONTENT_ENCODING);
				
				final BinaryEncodedDictionary responseMap = contentEncoding != null && 
						NetworkUtilities.HTTP_GZIP_ENCODING.equals(contentEncoding)? decoder.decodeGzip(responseStream) :
							decoder.decode(responseStream);
				
				return buildResponse(responseMap, announceRequest.getTorrent().getInfoHashHexValue());
			}
			else if(responseCode >= HttpURLConnection.HTTP_BAD_REQUEST &&
					responseCode <= HttpURLConnection.HTTP_GATEWAY_TIMEOUT){
				return buildErrorResponse(connection.getErrorStream());
			}
			return new TrackerResponse(TrackerResponse.Type.READ_WRITE_ERROR, "Error response: code " + responseCode);
		}
		catch(final BinaryDecoderException bde) {
			return new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, bde.getMessage());
		}
		catch(final MalformedURLException mue) {
			return new TrackerResponse(TrackerResponse.Type.INVALID_URL, mue.getMessage());
		}
		catch(final IOException ioe) {
			return new TrackerResponse(TrackerResponse.Type.READ_WRITE_ERROR, ioe.getMessage());
		}
	}
	
	protected TrackerResponse buildErrorResponse(final InputStream errorStream) throws IOException {
		final StringBuilder errorMessage = new StringBuilder();
		
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(
				errorStream, StandardCharsets.UTF_8.name()))) {
			errorMessage.append(reader.readLine());
		}
		
		return new TrackerResponse(TrackerResponse.Type.TRACKER_ERROR, errorMessage.toString());
	}
	
	protected TrackerResponse buildResponse(final BinaryEncodedDictionary responseMap, final String infoHash) {
		final BinaryEncodedString failureReason = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_FAILURE_REASON);
		
		if(failureReason != null) {
			return new TrackerResponse(TrackerResponse.Type.TRACKER_ERROR, failureReason.toString());
		}
		
		final BinaryEncodable peerList = responseMap.get(BinaryEncodingKeyNames.KEY_PEERS);
		
		final BinaryEncodedInteger interval = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INTERVAL));
		
		final BinaryEncodedInteger complete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_COMPLETE));
		
		final BinaryEncodedInteger incomplete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INCOMPLETE));
		
		if(!validateMandatoryResponseValues(peerList, interval, complete, incomplete)) {
			return new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, 
					"Missing mandatory response value");
		}
		
		final long incompleteValue = incomplete.getValue();
		final long intervalValue = interval.getValue();
		final long completeValue = complete.getValue();
		
		final BinaryEncodedString warningMessage = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_WARNING_MESSAGE);
		
		final TrackerResponse.Type responseType = warningMessage != null? 
				TrackerResponse.Type.WARNING : TrackerResponse.Type.NORMAL;
		final String trackerMessage = warningMessage != null? warningMessage.toString() : null;
		
		final BinaryEncodedInteger minInterval = (BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_MIN_INTERVAL);
		final Long minIntervalValue = minInterval != null? minInterval.getValue() : null;
		
		final BinaryEncodedString trackerId = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_TRACKER_ID);
		final String trackerIdValue = trackerId != null? trackerId.toString() : null;
		
		final Set<PwpPeer> peers = extractPeers(peerList, infoHash);
		
		return new TrackerResponse(responseType, trackerMessage, intervalValue, minIntervalValue, trackerIdValue,
				completeValue, incompleteValue, peers);
	}
	
	protected Set<PwpPeer> extractPeers(final BinaryEncodable peerList, final String infoHash) {				
		return peerList instanceof BinaryEncodedList? extractPeerList(
				(BinaryEncodedList)peerList, infoHash) : extractPeerString((BinaryEncodedString)peerList, infoHash);
	}
	
	protected Set<PwpPeer> extractPeerList(final BinaryEncodedList peerList, final String infoHash) {
		return peerList.stream().map(p -> {
			final BinaryEncodedDictionary peerInfo = (BinaryEncodedDictionary)p;
			//TODO: Extract peer id, do we need it?
			final String peerIp = ((BinaryEncodedString)peerInfo.get(BinaryEncodingKeyNames.KEY_IP)).toString();
			final long peerPort = ((BinaryEncodedInteger)peerInfo.get(BinaryEncodingKeyNames.KEY_PORT)).getValue();
			return new PwpPeer(peerIp, (int)peerPort, infoHash);
		}).collect(Collectors.toSet());
	}
	
	protected Set<PwpPeer> extractPeerString(final BinaryEncodedString peerString, final String infoHash) {
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
}