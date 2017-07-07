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
package org.matic.torrent.net.udp;

import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.NetworkProperties;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.listeners.DhtResponseListener;
import org.matic.torrent.tracking.listeners.UdpTrackerResponseListener;
import org.matic.torrent.tracking.methods.dht.DhtResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An UDP connection manager, based on non-blocking IO.
 * It is responsible for sending of the UDP data packets
 * and notifying interested listeners when a UDP packet
 * has arrived.
 * 
 * @author Vedran Matic
 *
 */
public class UdpConnectionManager {
	
	public static final int DEFAULT_UDP_PORT = 45895;
	public static final int UDP_TRACKER_PORT = Integer.parseInt(ApplicationPreferences.getProperty(
			NetworkProperties.UDP_TRACKER_PORT, String.valueOf(UdpConnectionManager.DEFAULT_UDP_PORT)));
	public static final int DHT_PORT = Integer.parseInt(ApplicationPreferences.getProperty(
			NetworkProperties.UDP_DHT_PORT, String.valueOf(UdpConnectionManager.DEFAULT_UDP_PORT)));
		
	private static final int MAX_INPUT_PACKET_SIZE = 1024;
	private static final int MAX_OUTPUT_PACKET_SIZE = 1024;
	private static final int MAX_OUTGOING_MESSAGES = 30;
	
	private static final int REQUEST_EXECUTOR_WORKER_TIMEOUT = 60;		//60 seconds
	private static final int REQUEST_EXECUTOR_THREAD_POOL_SIZE = 5;	//parallel requests at a time
	
	private final ThreadPoolExecutor channelWriterExecutor = new ThreadPoolExecutor(
			REQUEST_EXECUTOR_THREAD_POOL_SIZE, REQUEST_EXECUTOR_THREAD_POOL_SIZE, 
			REQUEST_EXECUTOR_WORKER_TIMEOUT, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	
	private final BlockingQueue<UdpRequest> outgoingMessages;
	private final Set<UdpTrackerResponseListener> trackerListeners;
	private final Set<DhtResponseListener> dhtListeners;
	
	private final ByteBuffer inputBuffer = ByteBuffer.allocate(MAX_INPUT_PACKET_SIZE);
	
	private final ExecutorService connectionManagerExecutor;
	private Selector connectionSelector = null;	
	
	public UdpConnectionManager() {
		this.connectionManagerExecutor = Executors.newSingleThreadExecutor();
		
		outgoingMessages = new ArrayBlockingQueue<>(MAX_OUTGOING_MESSAGES);
		trackerListeners = new CopyOnWriteArraySet<>();
		dhtListeners = new CopyOnWriteArraySet<>();
		
		channelWriterExecutor.allowCoreThreadTimeOut(true);
	}
	
	public void addTrackerListener(final UdpTrackerResponseListener listener) {
		trackerListeners.add(listener);
	}
	
	public void removeTrackerListener(final UdpTrackerResponseListener listener) {
		trackerListeners.remove(listener);
	}
	
	public void addDhtListener(final DhtResponseListener listener) {
		dhtListeners.add(listener);
	}
	
	public void removeDhtListener(final DhtResponseListener listener) {
		dhtListeners.remove(listener);
	}
	
	/**
	 * Send an UDP packet request to a remote host 
	 * 
	 * @param request UDP packet request to send
	 * @return Whether the request was scheduled
	 */
	public boolean send(final UdpRequest request) {
		final boolean requestAdded = outgoingMessages.offer(request);

		if(connectionSelector != null) {
			connectionSelector.wakeup();
		}
		return requestAdded;
	}

	/**
	 * Start UDP connection manager.
	 *
	 * @param listenPort Target port for received UDP packets
	 */
	public void manage(final int listenPort) {
		try {
			connectionSelector = Selector.open();
		}
		catch(final IOException ioe) {
			System.err.println("Failed to start UDP connection manager due to: " + ioe.getMessage());
			return;
		}		
		connectionManagerExecutor.execute(() -> {
			try(final DatagramChannel channel = DatagramChannel.open()) {								
				setChannelOptions(channel, connectionSelector, listenPort);
				while(true) {					
					if(Thread.currentThread().isInterrupted()) {						
						Thread.interrupted();
						break;
					}
					try {						
						processPendingReadOperations(channel);
					}
					catch(final IOException ioe) {
						System.err.println("Selection processing resulted in an error: " + ioe.getMessage());
					}
					//Check for any pending packets to be sent
					while(!outgoingMessages.isEmpty()) {						
						final UdpRequest request = outgoingMessages.poll();
						channelWriterExecutor.execute(() -> writeToChannel(channel, request));						
					}
				}
			}
			catch(final IOException ioe) {
				System.err.println("UDP server was shutdown unexpectedly: " + ioe.getMessage());
			}								
		});
	}
	
	/**
	 * Stop UDP connection manager
	 */
	public void unmanage() {
		connectionManagerExecutor.shutdownNow();
		channelWriterExecutor.shutdownNow();
		if(connectionSelector != null) {
			connectionSelector.wakeup();
		}
	}
	
	private void processPendingReadOperations(final DatagramChannel channel) throws IOException {
		final int keysSelected = connectionSelector.select();
		if(keysSelected > 0) {
			final Set<SelectionKey> selectedKeys = connectionSelector.selectedKeys();
			final Iterator<SelectionKey> selectedKeysIterator = selectedKeys.iterator();
			
			while(selectedKeysIterator.hasNext()) {
				final SelectionKey selectedKey = selectedKeysIterator.next();
				selectedKeysIterator.remove();
				
				if(selectedKey.isValid() && selectedKey.isReadable()) {
					//Handle a read attempt from the channel
					readFromChannel(channel);
				}
			}
		}
	}
	
	private void readFromChannel(final DatagramChannel channel) throws IOException {
		inputBuffer.clear();
		final SocketAddress senderAddress = channel.receive(inputBuffer);	
		
		if(senderAddress == null) {
			return;
		}
		
		final byte[] receivedPacket = new byte[inputBuffer.position()];		
		inputBuffer.flip();
		inputBuffer.get(receivedPacket);
				
		//First try to parse a DHT message
		final DhtResponse dhtResponse = UdpDataPacketParser.parseDHTResponse(receivedPacket);
		if(dhtResponse != null) {
			notifyDhtListenersOnResponse(dhtResponse);
		}
		else {
			//Try parsing as a regular UDP tracker response message
			final UdpTrackerResponse trackerResponse = UdpDataPacketParser.parseTrackerResponse(receivedPacket);

			if(trackerResponse != null) {				
				notifyTrackerListenersOnResponse(trackerResponse);
			}
		}
	}
	
	private void writeToChannel(final DatagramChannel channel, final UdpRequest udpRequest) {
		final InetSocketAddress remoteAddress = new InetSocketAddress(
				udpRequest.getReceiverHost(), udpRequest.getReceiverPort());
		
		if(remoteAddress.isUnresolved()) {
			notifyListenersOnRequestError(udpRequest,
					Tracker.getStatusMessage(Tracker.Status.HOSTNAME_NOT_FOUND));
			return;
		}
		
		final ByteBuffer outputBuffer = ByteBuffer.allocate(MAX_OUTPUT_PACKET_SIZE);
		outputBuffer.order(ByteOrder.BIG_ENDIAN);
		outputBuffer.put(udpRequest.getRequestData());
		outputBuffer.flip();
		
		try {			
			channel.send(outputBuffer, remoteAddress);
		} catch (final IOException ioe) {
			notifyListenersOnRequestError(udpRequest, Tracker.getStatusMessage(Tracker.Status.CONNECTION_TIMEOUT));
		}
	}
	
	private void notifyListenersOnRequestError(final UdpRequest udpRequest, final String message) {
		if(udpRequest.getType() == UdpRequest.Type.DHT) {
			notifyDhtListenersOnRequestError(udpRequest, message);
		}
		else {
			notifyTrackerListenersOnRequestError(udpRequest, message);
		}
	}
	
	private void notifyDhtListenersOnResponse(final DhtResponse dhtResponse) {
		dhtListeners.forEach(l -> l.onDhtResponseReceived(dhtResponse));
	}
	
	private void notifyDhtListenersOnRequestError(final UdpRequest udpRequest, final String message) {
		dhtListeners.forEach(l -> l.onDhtRequestError(udpRequest, message));
	}
	
	private void notifyTrackerListenersOnResponse(final UdpTrackerResponse response) {		
		trackerListeners.forEach(l -> l.onUdpTrackerResponseReceived(response));
	}
	
	private void notifyTrackerListenersOnRequestError(final UdpRequest udpRequest, final String message) {		
		trackerListeners.forEach(l -> l.onUdpTrackerRequestError(udpRequest, message));
	}
	
	private void setChannelOptions(final DatagramChannel channel, final Selector connectionSelector,
                                   final int listenPort) throws IOException {
		channel.configureBlocking(false);
		channel.bind(NetworkUtilities.getSocketAddress(listenPort));
		channel.register(connectionSelector, SelectionKey.OP_READ);
	}
}