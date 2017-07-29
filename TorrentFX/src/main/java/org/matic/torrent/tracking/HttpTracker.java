/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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

import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.exception.BinaryDecoderException;
import org.matic.torrent.hash.HashUtilities;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.client.ClientProperties;
import org.matic.torrent.tracking.listeners.TrackerResponseListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class HttpTracker extends Tracker {
	
	private static final String EVENT_COMPLETED = "completed";
	private static final String EVENT_STOPPED = "stopped";
	private static final String EVENT_STARTED = "started";	
	
	private static final String REQUEST_TYPE_ANNOUNCE = "announce";
	private static final String REQUEST_TYPE_SCRAPE = "scrape";
	
	private final Set<TrackerResponseListener> listeners;	
	private final String scrapeUrl;
	
	/**
	 * @see Tracker#Tracker(String)
	 */
	protected HttpTracker(final String url) {
		super(url);				
		listeners = new CopyOnWriteArraySet<>();
		
		final int lastSlashIndex = url.lastIndexOf('/');
		final boolean scrapeSupported = url.startsWith(REQUEST_TYPE_ANNOUNCE, lastSlashIndex + 1);
		
		scrapeUrl = scrapeSupported? url.replace(REQUEST_TYPE_ANNOUNCE, REQUEST_TYPE_SCRAPE) : null;
	}
	
	public void addListener(final TrackerResponseListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(final TrackerResponseListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListeners(final Consumer<TrackerResponseListener> notification) {
		listeners.forEach(l -> notification.accept(l));
	}
	
	/**
	 * @see Tracker#isScrapeSupported()
	 */
	@Override
	public boolean isScrapeSupported() {
		return scrapeUrl != null;
	}
	
	/**
	 * @see Tracker#scrape(TrackerSession...)
	 */
	@Override
	protected void scrape(final TrackerSession... trackerSessions) {
		if(trackerSessions.length == 0) {
			return;
		}
		
		final String requestUrl = buildScrapeRequestUrl(trackerSessions);
		
		final TrackerResponse trackerResponse = requestUrl == null? new TrackerResponse(
				TrackerResponse.Type.INVALID_URL, "Unsupported encoding") : sendRequest(requestUrl);
				
		final ScrapeResponse scrapeResponse = trackerResponse.getType() == TrackerResponse.Type.OK?
				buildScrapeResponse(trackerResponse.getResponseData(), trackerSessions) :
				new ScrapeResponse(trackerResponse.getType(), trackerResponse.getMessage());
				
		notifyListeners(l -> l.onScrapeResponseReceived(this, scrapeResponse));		
	};
	
	/**
	 * @see Tracker#connect(int)
	 */
	@Override
	protected int connect(final int transactionId) {
		//Connection establishment is not supported by HTTP trackers
		return 0;
	}
	
	/**
	 * @see Tracker#getType()
	 */
	@Override
	public Type getType() {
		return Type.TCP;
	}
	
	/**
	 * @see Tracker#getId()
	 */
	@Override
	public long getId() {
		//Not supported by HTTP trackers
		return 0;
	}
	
	/**
	 * @see Tracker#setId(long)
	 */
	@Override
	public void setId(final long id) {
		//Not supported by HTTP trackers
	}

	/**
	 * @see Tracker#announce(AnnounceParameters, TrackerSession)
	 */
	@Override
	protected void announce(final AnnounceParameters announceParameters, final TrackerSession trackerSession) {		
		final String requestUrl = buildAnnounceRequestUrl(announceParameters,
				trackerSession.getTorrentView().getInfoHash(), trackerSession.getKey());
		final TrackerResponse trackerResponse = requestUrl == null? new TrackerResponse(
				TrackerResponse.Type.INVALID_URL, "Unsupported encoding") : sendRequest(requestUrl);
				
		final AnnounceResponse announceResponse = trackerResponse.getType() == TrackerResponse.Type.OK?
				buildAnnounceResponse(trackerResponse.getResponseData(), trackerSession.getTorrentView().getInfoHash()) :
					new AnnounceResponse(trackerResponse.getType(), trackerResponse.getMessage());
		
		if(trackerResponse.getType() == TrackerResponse.Type.OK) {
			trackerSession.setLastAcknowledgedEvent(announceParameters.getTrackerEvent());
		}
		notifyListeners(l -> l.onAnnounceResponseReceived(announceResponse, trackerSession));		
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
	
	protected String buildScrapeRequestUrl(final TrackerSession... trackerSessions) {
		final StringBuilder result = new StringBuilder(scrapeUrl);
		result.append("?");
		
		result.append(Arrays.stream(trackerSessions).map(ts -> "info_hash=" + HashUtilities.urlEncodeBytes(
				ts.getTorrentView().getInfoHash().getBytes())).collect(Collectors.joining("&")));

		return result.toString();
	}
	
	protected String buildAnnounceRequestUrl(final AnnounceParameters announceParameters,
			final InfoHash infoHash, final int key) {		
		final StringBuilder result = new StringBuilder(super.getUrl());
		result.append("?info_hash=");
		result.append(HashUtilities.urlEncodeBytes(infoHash.getBytes()));
		result.append("&peer_id=");
		
		try {
			result.append(URLEncoder.encode(
					ClientProperties.PEER_ID, 
					StandardCharsets.UTF_8.name()));
		} 
		catch (final UnsupportedEncodingException uee) {
			//This will never happen because UTF-8 is always supported
			System.err.println("Unsupported encoding: " + uee.toString());
			return null;
		}
		
		result.append("&port=");
		result.append(ClientProperties.TCP_PORT);
		result.append("&uploaded=");
		result.append(announceParameters.getUploaded());
		result.append("&downloaded=");
		result.append(announceParameters.getDownloaded());
		result.append("&left=");
		result.append(announceParameters.getLeft());
		result.append("&corrupt=0&key=");		
		result.append(Integer.toHexString(key));
		
		final Event trackerEvent = announceParameters.getTrackerEvent();
		final String eventName = getEventName(trackerEvent);
		if(eventName != null) {
			result.append("&event=");
			result.append(eventName);
		}
		
		result.append("&numwant=");
		result.append(trackerEvent != Event.STOPPED? NUM_WANTED_PEERS : 0);
		
		//TODO: Add &supportcrypto=1&redundant=0 to the request parameters
		result.append("&compact=1&no_peer_id=1");
		
		return result.toString();
	}
	
	//TODO: Add proxy support for the request
	private TrackerResponse sendRequest(final String url) {		
		try {			
			final URL targetUrl = new URL(url);
			final HttpURLConnection connection = (HttpURLConnection)targetUrl.openConnection();			
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
			connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME, NetworkUtilities.getHttpUserAgent());			
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_ENCODING, NetworkUtilities.HTTP_GZIP_ENCODING);
			connection.setReadTimeout(NetworkUtilities.HTTP_CONNECTION_TIMEOUT);
			connection.setConnectTimeout(NetworkUtilities.HTTP_CONNECTION_TIMEOUT);
			connection.setInstanceFollowRedirects(true);

			final int responseCode = connection.getResponseCode();
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				try(final InputStream responseStream = connection.getInputStream()) {															
					//Check whether the response stream is gzip encoded
					final String contentEncoding = connection.getHeaderField(NetworkUtilities.HTTP_CONTENT_ENCODING);
					
					final BinaryDecoder responseDecoder = new BinaryDecoder();
					final BinaryEncodedDictionary responseMap = contentEncoding != null && 
							NetworkUtilities.HTTP_GZIP_ENCODING.equals(contentEncoding)? responseDecoder.decodeGzip(responseStream) :
								responseDecoder.decode(responseStream);							
					return new TrackerResponse(TrackerResponse.Type.OK, null, responseMap);
				}				
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
        catch(final UnknownHostException uhe) {
            return new TrackerResponse(TrackerResponse.Type.INVALID_URL, "No such host is known.");
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
	
	protected ScrapeResponse buildScrapeResponse(final BinaryEncodedDictionary responseMap,
			final TrackerSession... trackerSessions) {
		final BinaryEncodedDictionary files = (BinaryEncodedDictionary)responseMap.get(BinaryEncodingKeys.KEY_FILES);
		
		if(!validateMandatoryResponseValues(files)) {			
			return new ScrapeResponse(TrackerResponse.Type.INVALID_RESPONSE, 
					"Missing mandatory response value: files");
		}
		
		final Map<TrackerSession, ScrapeStatistics> scrapeStatistics = new HashMap<>();
		
		Arrays.stream(trackerSessions).forEach(ts -> {
			final BinaryEncodedDictionary scrapeInfo = (BinaryEncodedDictionary)files.get(
					new BinaryEncodedString(ts.getTorrentView().getInfoHash().getBytes()));
			if(scrapeInfo != null) {
				final BinaryEncodedInteger complete = (BinaryEncodedInteger)scrapeInfo.get(
						BinaryEncodingKeys.KEY_COMPLETE);
				final BinaryEncodedInteger downloaded = (BinaryEncodedInteger)scrapeInfo.get(
						BinaryEncodingKeys.KEY_DOWNLOADED);
				final BinaryEncodedInteger incomplete = (BinaryEncodedInteger)scrapeInfo.get(
						BinaryEncodingKeys.KEY_INCOMPLETE);				
				
				if(!validateMandatoryResponseValues(complete, downloaded, incomplete)) {
					return;
				}
				
				final BinaryEncodedString name = (BinaryEncodedString)scrapeInfo.get(
						BinaryEncodingKeys.KEY_NAME);
				
				final ScrapeStatistics scrapeStat = new ScrapeStatistics((int)complete.getValue(),
						(int)downloaded.getValue(), (int)incomplete.getValue(),
						name != null? name.toString() : null);
				scrapeStatistics.put(ts, scrapeStat);
			}
		});
		
		final BinaryEncodedString failureReason = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeys.KEY_FAILURE_REASON);
		
		final Map<String, String> flags = new HashMap<>();
		
		final BinaryEncodedDictionary flagsDictionary = (BinaryEncodedDictionary)responseMap.get(
				BinaryEncodingKeys.KEY_FLAGS);
		
		if(flagsDictionary != null) {
			flagsDictionary.keys().forEach(key -> flags.put(key.toString(), flagsDictionary.get(key).toString()));
		}
		
		return new ScrapeResponse(TrackerResponse.Type.OK, failureReason != null? 
				failureReason.toString() : null, flags, scrapeStatistics);
	}
	
	protected AnnounceResponse buildAnnounceResponse(final BinaryEncodedDictionary responseMap, final InfoHash infoHash) {
		final BinaryEncodedString failureReason = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeys.KEY_FAILURE_REASON);
		
		if(failureReason != null) {
			return new AnnounceResponse(TrackerResponse.Type.TRACKER_ERROR, failureReason.toString());
		}
		
		final BinaryEncodable peerList = responseMap.get(BinaryEncodingKeys.KEY_PEERS);
		
		final BinaryEncodedInteger interval = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeys.KEY_INTERVAL));
		
		final BinaryEncodedInteger complete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeys.KEY_COMPLETE));
		
		final BinaryEncodedInteger incomplete = ((BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeys.KEY_INCOMPLETE));
		
		if(!validateMandatoryResponseValues(peerList, interval)) {
			return new AnnounceResponse(TrackerResponse.Type.INVALID_RESPONSE, 
					"Missing mandatory response value");
		}
		
		final long incompleteValue = incomplete != null? incomplete.getValue() : 0;
        final long completeValue = complete != null? complete.getValue() : 0;
		final long intervalValue = interval.getValue() * 1000;
		
		final BinaryEncodedString warningMessage = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeys.KEY_WARNING_MESSAGE);
		
		final TrackerResponse.Type responseType = warningMessage != null? 
				TrackerResponse.Type.WARNING : TrackerResponse.Type.OK;
		final String trackerMessage = warningMessage != null? warningMessage.toString() : null;
		
		final BinaryEncodedInteger minInterval = (BinaryEncodedInteger)responseMap.get(
				BinaryEncodingKeys.KEY_MIN_INTERVAL);
		final Long minIntervalValue = minInterval != null? minInterval.getValue() * 1000 : null;
		
		final BinaryEncodedString trackerId = (BinaryEncodedString)responseMap.get(
				BinaryEncodingKeys.KEY_TRACKER_ID);
		final String trackerIdValue = trackerId != null? trackerId.toString() : null;
		
		final Set<PwpPeer> peers = PwpPeer.extractPeers(peerList, infoHash);
		
		return new AnnounceResponse(responseType, trackerMessage, intervalValue, minIntervalValue, trackerIdValue,
				completeValue, incompleteValue, peers);
	}

	private boolean validateMandatoryResponseValues(final BinaryEncodable... values) {
		return !Arrays.stream(values).anyMatch(v -> v == null);		
	}

	@Override
	public String toString() {
		return "HttpTracker [url=" + getUrl() + "]";
	}
}