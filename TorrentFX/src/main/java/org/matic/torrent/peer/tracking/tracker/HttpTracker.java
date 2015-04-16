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

import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.peer.tracking.TrackableTorrent;

public final class HttpTracker extends Tracker {
	
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	
	//TODO: Use a real user agent value
	private static final String USER_AGENT_VALUE = "Mozilla/34.0";
	private static final String ACCEPT_CHARSET = "Accept-Charset";	
	private static final String USER_AGENT_NAME = "User-Agent";	
	private static final String GZIP_ENCODING = "gzip";
	
	private static final String EVENT_COMPLETED = "completed";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_STARTED = "started";	
	
	private final TrackerResponseListener responseListener;

	//TODO: Import BinaryDecoder for response parsing
	
	public HttpTracker(final String url, final TrackerResponseListener responseListener) {
		super(url);				
		this.responseListener = responseListener;
	}
	
	@Override
	public Type getType() {
		return Type.TCP;
	}

	@Override
	public final void announce(final AnnounceRequest announceParameters) {				
		final TrackerResponse trackerResponse = sendRequest(announceParameters);
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
		final String urlTemplate = "info_hash=%s&peer_id=%s&uploaded=%d&downloaded=%d&left=%d";		
		final StringBuilder result = new StringBuilder(String.format(urlTemplate, URLEncoder.encode(
				torrent.getInfoHashHexValue(), StandardCharsets.UTF_8.name()),
				URLEncoder.encode(ClientProperties.PEER_ID, StandardCharsets.UTF_8.name()),
				announceRequest.getUploaded(), announceRequest.getDownloaded(),
				announceRequest.getLeft()));
		
		final String eventName = getEventName(announceRequest.getEvent());
		if(eventName != null) {
			result.append("?event=");
			result.append(eventName);
		}
		
		return result.toString();
	}
	
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
				//BinaryEncodedMap responseMap = encoder.decode(connection.getInputStream());
				return buildResponse(/* responseMap */);
			}
			else if(responseCode >= HttpURLConnection.HTTP_BAD_REQUEST &&
					responseCode <= HttpURLConnection.HTTP_GATEWAY_TIMEOUT){
				return buildErrorResponse(connection.getErrorStream());
			}
			return new TrackerResponse(TrackerResponse.Type.READ_WRITE_ERROR);
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
	
	private TrackerResponse buildResponse(/* final BinaryEncodedMap responseMap */) {
		return new TrackerResponse(TrackerResponse.Type.NORMAL);
	}
}