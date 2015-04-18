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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import org.matic.torrent.io.codec.BinaryDecoder;
import org.matic.torrent.io.codec.BinaryDecoderException;
import org.matic.torrent.io.codec.BinaryEncodable;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedInteger;
import org.matic.torrent.io.codec.BinaryEncodedList;
import org.matic.torrent.io.codec.BinaryEncodedString;
import org.matic.torrent.io.codec.BinaryEncodingKeyNames;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class HttpTracker extends Tracker {
	
	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TEXT_PLAIN = "text/plain";
	
	//TODO: Use a real user agent value
	private static final String USER_AGENT_VALUE = "Mozilla/34.0";
	private static final String ACCEPT_CHARSET = "Accept-Charset";	
	private static final String USER_AGENT_NAME = "User-Agent";	
	private static final String GZIP_ENCODING = "gzip";
	
	private static final String EVENT_COMPLETED = "completed";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_STARTED = "started";	
	
	private final TrackerResponseListener responseListener;
	private final BinaryDecoder decoder;
	
	public HttpTracker(final String url, final TrackerResponseListener responseListener) {
		super(url);				
		this.responseListener = responseListener;
		decoder = new BinaryDecoder();
	}
	
	@Override
	public final boolean isScrapeSupported() {
		//TODO: Implement method
		return false;
	};
	
	@Override
	public final Type getType() {
		return Type.TCP;
	}

	@Override
	public final void announce(final AnnounceRequest announceRequest) {				
		final TrackerResponse trackerResponse = sendRequest(announceRequest);
		responseListener.onResponseReceived(trackerResponse, this);				
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
	
	private String buildRequestUrl(final AnnounceRequest announceRequest) 
			throws UnsupportedEncodingException {		
		final TrackableTorrent torrent = announceRequest.getTorrent();
		final String urlTemplate = "info_hash=%s&peer_id=%s&uploaded=%d&downloaded=%d&left=%d&port=%d&compact=1";		
		final StringBuilder result = new StringBuilder(String.format(urlTemplate, URLEncoder.encode(
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
			targetUrl = new URL(url + "?" + buildRequestUrl(announceRequest));			
			final HttpURLConnection connection = (HttpURLConnection)targetUrl.openConnection();
			connection.setRequestProperty(ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
			connection.setRequestProperty(USER_AGENT_NAME, USER_AGENT_VALUE);			
			connection.setRequestProperty(ACCEPT_ENCODING, GZIP_ENCODING);
			
			final int responseCode = connection.getResponseCode();
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				final InputStream responseStream = connection.getInputStream();
				
				final String contentType = connection.getHeaderField(CONTENT_TYPE);
				if(contentType == null || !TEXT_PLAIN.equals(contentType)) {
					return new TrackerResponse(TrackerResponse.Type.INVALID_RESPONSE, "Not a text/plain response");
				}
				
				//Check whether the response stream is gzip encoded
				final String contentEncoding = connection.getHeaderField(CONTENT_ENCODING);
				
				final BinaryEncodedDictionary responseMap = contentEncoding != null && 
						GZIP_ENCODING.equals(contentEncoding)? decoder.decodeGzip(responseStream) :
							decoder.decode(responseStream);
				
				return buildResponse(responseMap);
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
	
	private TrackerResponse buildErrorResponse(final InputStream errorStream) throws IOException {
		final StringBuilder errorMessage = new StringBuilder();
		
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(
				errorStream, StandardCharsets.UTF_8.name()))) {
			errorMessage.append(reader.readLine());
		}
		
		return new TrackerResponse(TrackerResponse.Type.TRACKER_ERROR, errorMessage.toString());
	}
	
	//TODO: Validate that all mandatory values are present, otherwise return INVALID_RESPONSE
	private TrackerResponse buildResponse(final BinaryEncodedDictionary responseMap) {
		final BinaryEncodedString failureReason = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_FAILURE_REASON);
		
		if(failureReason != null) {
			return new TrackerResponse(TrackerResponse.Type.TRACKER_ERROR, failureReason.toString());
		}
		
		final BinaryEncodedString warningMessage = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_WARNING_MESSAGE);
		
		final TrackerResponse.Type responseType = warningMessage != null? 
				TrackerResponse.Type.WARNING : TrackerResponse.Type.NORMAL;
		final Optional<String> trackerMessage = Optional.ofNullable(
				warningMessage != null? warningMessage.toString() : null);
		
		final BinaryEncodedInteger minInterval = (BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_MIN_INTERVAL);
		final Optional<Long> minIntervalValue = Optional.ofNullable(
				minInterval != null? minInterval.getValue() : null);
		
		final BinaryEncodedString trackerId = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeyNames.KEY_TRACKER_ID);
		final Optional<String> trackerIdValue = Optional.ofNullable(
				trackerId != null? trackerId.toString() : null);
		
		final long interval = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INTERVAL)).getValue() * 1000;
		
		final long complete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_COMPLETE)).getValue();
		
		final long incomplete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeyNames.KEY_INCOMPLETE)).getValue();
		
		return new TrackerResponse(responseType, trackerMessage, interval, minIntervalValue, trackerIdValue,
				complete, incomplete, extractPeers(responseMap.get(BinaryEncodingKeyNames.KEY_PEERS)));
	}
	
	private Set<PwpPeer> extractPeers(final BinaryEncodable peerList) {		
		return peerList instanceof BinaryEncodedList? extractPeerList(
				(BinaryEncodedList)peerList) : extractPeerString((BinaryEncodedString)peerList);
	}
	
	private Set<PwpPeer> extractPeerList(final BinaryEncodedList peerList) {
		return null;
	}
	
	private Set<PwpPeer> extractPeerString(final BinaryEncodedString peerString) {
		return null;
	}
}