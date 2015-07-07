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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.UnresolvedAddressException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.tracking.listeners.DhtResponseListener;
import org.matic.torrent.tracking.listeners.UdpTrackerResponseListener;
import org.matic.torrent.tracking.methods.dht.DhtResponse;

/**
 * An UDP connection manager, based on non-blocking IO.
 * It is responsible for sending of the UDP data packets
 * and notifying interested listeners when a UDP packet
 * has arrived.
 * 
 * @author vedran
 *
 */
public final class UdpConnectionManager {
	
	public static final int DEFAULT_UDP_PORT = 45895;
		
	private static final int MAX_INPUT_PACKET_SIZE = 1024;
	private static final int MAX_OUTPUT_PACKET_SIZE = 1024;
	private static final int MAX_OUTGOING_MESSAGES = 30;
	
	private final BlockingQueue<UdpRequest> outgoingMessages;
	private final Set<UdpTrackerResponseListener> trackerListeners;
	private final Set<DhtResponseListener> dhtListeners;
	
	private final ByteBuffer outputBuffer = ByteBuffer.allocate(MAX_OUTPUT_PACKET_SIZE);
	private final ByteBuffer inputBuffer = ByteBuffer.allocate(MAX_INPUT_PACKET_SIZE);
	
	private final ExecutorService connectionManagerExecutor;
	private Selector connectionSelector = null;
	
	public UdpConnectionManager() {
		this.connectionManagerExecutor = Executors.newSingleThreadExecutor();		
		
		outgoingMessages = new ArrayBlockingQueue<>(MAX_OUTGOING_MESSAGES);
		trackerListeners = new CopyOnWriteArraySet<>();
		dhtListeners = new CopyOnWriteArraySet<>();
		
		outputBuffer.order(ByteOrder.BIG_ENDIAN);
	}
	
	public final void addTrackerListener(final UdpTrackerResponseListener listener) {		
		trackerListeners.add(listener);
	}
	
	public final void removeTrackerListener(final UdpTrackerResponseListener listener) {		
		trackerListeners.remove(listener);
	}
	
	public final void addDHTListener(final DhtResponseListener listener) {		
		dhtListeners.add(listener);
	}
	
	public final void removeDHTListener(final DhtResponseListener listener) {		
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
	 * Start UDP connection manager
	 * 
	 * @param networkInterface Network interface on which to listen
	 * @param listenPort Target port for received UDP packets
	 */
	public void manage(final String networkInterface, final int listenPort) {
		connectionManagerExecutor.execute(() -> {
			try(final DatagramChannel channel = DatagramChannel.open()) {
				connectionSelector = Selector.open();								
				setChannelOptions(channel, connectionSelector, networkInterface, listenPort);
				
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
						writeToChannel(channel, outgoingMessages.poll());
					}
				}
			}
			catch(final IOException ioe) {
				System.err.println("UDP server was shutdown unexpectedly: " + ioe.getMessage());
			}			
			connectionManagerExecutor.shutdown();
		});
	}
	
	/**
	 * Stop UDP connection manager
	 */
	public final void unmanage() {		
		connectionManagerExecutor.shutdownNow();
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
			notifyDHTListeners(dhtResponse);
		}
		else {
			//Try parsing as a regular UDP tracker response message
			final UdpTrackerResponse trackerResponse = UdpDataPacketParser.parseTrackerResponse(receivedPacket);
			if(trackerResponse != null) {
				notifyTrackerListeners(trackerResponse);
			}
		}
	}
	
	private void writeToChannel(final DatagramChannel channel, final UdpRequest udpRequest) {
		outputBuffer.clear();
		outputBuffer.put(udpRequest.getRequestData());
		outputBuffer.flip();
		
		try {
			channel.send(outputBuffer, new InetSocketAddress(
					udpRequest.getReceiverHost(), udpRequest.getReceiverPort()));			
		} catch (final IOException | UnresolvedAddressException e) {
			System.err.println("An error occurred while sending UDP packet [" + udpRequest.getReceiverHost() + "]");
		}
	}
	
	private void notifyDHTListeners(final DhtResponse dhtResponse) {
		dhtListeners.stream().forEach(l -> l.onDhtResponseReceived(dhtResponse));
	}
	
	private void notifyTrackerListeners(final UdpTrackerResponse response) {		
		trackerListeners.stream().forEach(l -> l.onUdpTrackerResponseReceived(response));
	}
	
	private void setChannelOptions(final DatagramChannel channel, final Selector connectionSelector,
			final String networkInterface, final int listenPort) throws IOException {
		channel.configureBlocking(false);
		channel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface(networkInterface, listenPort));
		channel.register(connectionSelector, SelectionKey.OP_READ);
	}
}