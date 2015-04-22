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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An UDP connection manager, handling receiving and sending of
 * UDP packets from and to remote peers and trackers that support
 * UDP protocol
 * 
 * @author vedran
 *
 */
public class UdpConnectionManager {
	
	private static final int MAX_OUTGOING_MESSAGES = 30;
	private static final int RECEIVER_BUFFER_SIZE = 10;
	private static final int THREAD_POOL_SIZE = 2;
	private static final int SERVER_PORT = 44893;
		
	private final BlockingQueue<UdpRequest> outgoingMessages;
	private final Set<UdpConnectionListener> listeners;	
	private final DatagramSocket serverSocket;
	private final ExecutorService threadPool;
	
	private final Runnable receiverWorker = () -> handleIncoming();
	private final Runnable senderWorker = () -> handleOutgoing();	
	
	public UdpConnectionManager() throws IOException {
		serverSocket = new DatagramSocket(SERVER_PORT);
		listeners = new CopyOnWriteArraySet<>();
		outgoingMessages = new ArrayBlockingQueue<>(MAX_OUTGOING_MESSAGES);
		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public final void init() {
		threadPool.execute(receiverWorker);
		threadPool.execute(senderWorker);		
	}
	
	public final void stop() {
		serverSocket.close();
		threadPool.shutdownNow();
	}
	
	public final void addListener(final UdpConnectionListener listener) {		
		listeners.add(listener);
	}
	
	public final void removeListener(final UdpConnectionListener listener) {		
		listeners.remove(listener);
	}
	
	public boolean send(final UdpRequest request) {		
		return outgoingMessages.offer(request);
	}
	
	private void notifyListeners(final UdpResponse response) {		
		listeners.stream().forEach(l -> l.onUdpResponseReceived(response));
	}
	
	private void handleOutgoing() {
		while(true) {
			try (final DatagramSocket clientSocket = new DatagramSocket()){
				final UdpRequest outgoingRequest = outgoingMessages.take();				
				
				final byte[] packetData = outgoingRequest.getRequestData();
				final DatagramPacket outgoingPacket = new DatagramPacket(packetData, 
						packetData.length, outgoingRequest.getReceiverAddress(),
						outgoingRequest.getReceiverPort());
				clientSocket.send(outgoingPacket);								
			} catch(final InterruptedException e) {
				System.out.println("Interrupted, UDP client going down");
				return;
			} catch(final IOException ioe) {
				System.err.println("Failed to open outgoing UDP socket: " + ioe);
				return;
			}
		}
	}
	
	private void handleIncoming() {		
		System.out.println("UDP server listening on port " + SERVER_PORT);
		final byte[] receiverBuffer = new byte[RECEIVER_BUFFER_SIZE];
		final DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
		
		while(true) {									
			try {
				serverSocket.receive(receivedPacket);	
				//TODO: Process received packet and notify listeners
				notifyListeners(new UdpResponse());
			} catch (final IOException ioe) {
				if(serverSocket.isClosed()) {
					//We've been interrupted, connection manager is going down, exit
					System.out.println("Interrupted, UDP server going down");
					return;
				}
				else {
					System.err.println("An error occured while receiving an UDP message: " + ioe);
				}
			}
		}		
	}
}