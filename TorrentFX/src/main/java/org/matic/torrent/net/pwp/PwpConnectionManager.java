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

package org.matic.torrent.net.pwp;

import org.matic.torrent.net.NetworkUtilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * A connection manager for PWP-connections. It listens for incoming connections and also
 * allows for adding new connections to remote peers.
 * 
 * @author vedran
 *
 */
public final class PwpConnectionManager {
	
	public static final String DEFAULT_NETWORK_INTERFACE = "";
	
	private static final long KEEP_CONNECTION_ALIVE_TIMEOUT = 90000;	//1:30 minutes	
	private static final int SO_RCVBUF_VALUE = 4 * 1024;	
	private static final boolean SO_REUSEADDR = true;	
	private static final int WORKER_POOL_SIZE = 5;
	
	private final ExecutorService connectionManagerExecutor;
	private final ExecutorService workerPool;
		
	private final Map<SocketChannel, PwpPeerSession> activeSessions;	
	private final Map<PwpPeer, SocketChannel> activeConnections;
	private final Map<SocketChannel, PwpConnectionIO> ioBuffers;
	private final Map<String, List<PwpPeer>> activePeers;
	
	private final Set<PwpPeer> inProgressConnectionRequests;
	private final List<PwpPeer> peerConnectionRequestQueue;	
	
	private final Set<PwpConnectionListener> connectionListeners;
	private final Set<PwpMessageListener> messageListeners;
	private final List<PwpMessageRequest> messageRequests;	
	
	private Selector connectionSelector;
	
	private final String name;
	private final int maxConnections;
	private final int listenPort;
	
	/**
	 * 
	 * Configure a new connection manager listening on a specified port and serving maxConnections connections
	 * 
	 * @param name Name identifier for this connection manager (for debugging purposes)
	 * @param listenPort Port to listen on for incoming connections
	 * @param maxConnections Max simultaneous connections served by this connection manager
	 * @throws IOException If a connection selector can't be opened
	 */
	public PwpConnectionManager(final String name, final int listenPort, final int maxConnections) {
		this.name = name;
		this.maxConnections = maxConnections;
		this.listenPort = listenPort;
		
		ioBuffers = new HashMap<>();
		activePeers = new HashMap<>();
		activeSessions = new HashMap<>();		
		activeConnections = new HashMap<>();
						
		peerConnectionRequestQueue = Collections.synchronizedList(new ArrayList<>());
		messageRequests = Collections.synchronizedList(new ArrayList<>());
		inProgressConnectionRequests = new HashSet<>();
		
		connectionListeners = new CopyOnWriteArraySet<>();
		messageListeners = new CopyOnWriteArraySet<>();
				
		connectionManagerExecutor = Executors.newSingleThreadExecutor();
		workerPool = Executors.newFixedThreadPool(PwpConnectionManager.WORKER_POOL_SIZE);				
	}
	
	/**
	 * Add a listener to be notified when a peer is connected/disconnected
	 * 
	 * @param listener Listener to add
	 */
	public final void addConnectionListener(final PwpConnectionListener listener) {		
		connectionListeners.add(listener);
	}
	
	/**
	 * Remove a listener previously interested in connection state notifications
	 * 
	 * @param listener Listener to remove
	 */
	public final void removeConnectionListener(final PwpConnectionListener listener) {
		connectionListeners.remove(listener);
	}
	
	/**
	 * Add a listener to be notified when a message is received from a peer
	 * 
	 * @param listener Listener to add
	 */
	public final void addMessageListener(final PwpMessageListener listener) {		
		messageListeners.add(listener);
	}
	
	/**
	 * Remove a listener previously interested in message arrival notifications
	 * 
	 * @param listener Listener to remove
	 */
	public final void removeMessageListener(final PwpMessageListener listener) {
		messageListeners.remove(listener);
	}
	
	/**
	 * 
	 * Start managing the connections (both incoming and outgoing)
	 * 
	 * @param listenInterface Network interface used to listen for incoming connections
	 */
	public final void manage(final String listenInterface) {
		
		try {
			connectionSelector = Selector.open();
		} 
		catch (final IOException ioe) {
			System.err.println("Failed to start PWP connection manager due to: " + ioe.getMessage());
			return;
		}
		
		connectionManagerExecutor.execute(() -> {
			try(final ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
				configureServerChannel(listenInterface, serverChannel);				
				System.out.println(name + ": Up and running, waiting for selection events...");
				
				long lastKeepAliveTimeout = System.currentTimeMillis();
				
				while(true) {										
					if(Thread.currentThread().isInterrupted()) {
						System.out.println(name + ": Received interrupted signal, will shut down...");
						//Release and shutdown connection channels, release resources
						disconnectAll();
						break;
					}
					try {
						//Wait for new selection events from active connections
						processPendingSelections(lastKeepAliveTimeout);
					}
					catch(final IOException ioe) {
						System.err.println("Selection processing resulted in an error: " + ioe.getMessage());
					}
					
					//Keep alive connections by sending KEEP_ALIVE, where needed
					if(System.nanoTime() - lastKeepAliveTimeout > 
						PwpConnectionManager.KEEP_CONNECTION_ALIVE_TIMEOUT) {
						keepActiveConnectionsAlive();
						lastKeepAliveTimeout = System.currentTimeMillis();						
					}	
					//Check for any pending messages to be sent to connected peers
					processPendingMessages();
					//Check for new connection requests to remote peers
					processPendingConnections();
				}
			}
			catch(final IOException ioe) {
				System.err.println(name + ": An error occured while running the server: " + ioe.getMessage());
			}
			workerPool.shutdown();
			System.out.println(name + ": Connection manager was shutdown!");
		}); 
	}
	
	/**
	 * 
	 * Initialize the shutdown of selector thread and allow it to die
	 */
	public final void unmanage() {		
		connectionManagerExecutor.shutdownNow();
		if(connectionSelector != null) {
			connectionSelector.wakeup();
		}
	}
	
	/**
	 * Send a message to a peer connected on the given connection
	 * 
	 * @param messageRequest Message to be sent to receiving peer
	 * @return Whether message request was added to message sender queue
	 */
	public final boolean send(final PwpMessageRequest messageRequest) {
		if(connectionSelector == null) {
			return false;
		}
		boolean messageAdded = false;
		synchronized(messageRequests) {
			messageAdded = messageRequests.add(messageRequest);
		}
		connectionSelector.wakeup();
		return messageAdded;
	}
	
	/**
	 * 
	 * Attempt to make a connection to a remote peer
	 * 
	 * @param peer A connection request to a remote peer
	 * @return Whether connection request was added to connection requester queue
	 */
	public final boolean connectTo(final PwpPeer peer) {	
		if(connectionSelector == null) {
			return false;
		}
		boolean connectionRequestAdded = false;
		synchronized (peerConnectionRequestQueue) {
			connectionRequestAdded = peerConnectionRequestQueue.add(peer);			
		}		
		connectionSelector.wakeup();
		return connectionRequestAdded;
	}
	
	private void keepActiveConnectionsAlive() {
		//Only send KEEP_ALIVE if we haven't sent anything within KEEP_CONNECTION_ALIVE_TIMEOUT
		activeSessions.forEach((connection, session) -> {
			if(session.getLastSentTime() > PwpConnectionManager.KEEP_CONNECTION_ALIVE_TIMEOUT) {
				write(connection, PwpMessageRequestFactory.newKeepAliveMessage());
			}
		});
		
		//Disconnect peers from which we haven't received anything for a while
		final Iterator<Map.Entry<SocketChannel, PwpPeerSession>> connectionIterator = activeSessions.entrySet().iterator();
		while(connectionIterator.hasNext()) {	
			final Map.Entry<SocketChannel, PwpPeerSession> entry = connectionIterator.next();
			if(System.currentTimeMillis() - entry.getValue().getLastReceivedTime() > 
				PwpConnectionManager.KEEP_CONNECTION_ALIVE_TIMEOUT) {				
				closeConnection(entry.getKey());				
				connectionIterator.remove();
				//TODO: Notify all listeners + extract listener notifications to a method
			}
		}
	}
	
	private void disconnectAll() {
		activeSessions.forEach((connection, session) -> closeConnection(connection));
		activeSessions.clear();
	}
	
	private void configureServerChannel(final String listenInterface,
			final ServerSocketChannel serverChannel) throws IOException {
		if(!serverChannel.isOpen()) {
			final String errorMessage = name + ": Failed to start server on port " + 
					listenPort + " with interface " + listenInterface;
			throw new IOException(errorMessage);
		}
		setChannelOptions(serverChannel);
		
		serverChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface(listenInterface, listenPort), maxConnections);
		serverChannel.configureBlocking(false);
		
		serverChannel.register(connectionSelector, SelectionKey.OP_ACCEPT);
	}
	 
	private void write(final SocketChannel connection, final byte[] messageData) {
		try {
			final boolean writeSuccess = ioBuffers.get(connection).write(connection, messageData);
		    if(!writeSuccess) {
		    	final SelectionKey selectionKey = connection.keyFor(connectionSelector);
		    	selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
		    	return;
		    }			
			final SelectionKey selectionKey = connection.keyFor(connectionSelector);
	    	selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);		
	    	activeSessions.get(connection).updateLastSentTime();
		} catch (final IOException ioe) {
			//TODO: Cleanup PwpPeerSession for this connection, ESPECIALLY ByteBuffer's and pending message requests (may block
			//indefinitely otherwise)!
			closeConnection(connection);
			//Remove connection from activeConnections and notify listeners				
			activeSessions.remove(connection);
			
			//TODO: Notify listeners that the connection was lost			
		}		
	}
	
	private void writeToChannel(final SelectionKey selectedKey) {
		System.out.println(name + ": Handling OP_WRITE");
		
		final SocketChannel peerConnection = (SocketChannel)selectedKey.channel();
		
		//TODO: Need to obtain previous data that was partially written (i.e. rest of byte[] forming messageData to send
		//write(peerChannel, activeConnections.get(peerConnection).getWriterBuffer());
		//TODO: session.addMessageRequest(PwpMessageRequest) as BlockingQueue.add()
	}
	
	private void processPendingSelections(final long lastKeepAliveTimeout) throws IOException {
		final int keysSelected = connectionSelector.select(PwpConnectionManager.KEEP_CONNECTION_ALIVE_TIMEOUT -
				(System.currentTimeMillis() - lastKeepAliveTimeout));
		if(keysSelected > 0) {
			final Set<SelectionKey> selectedKeys = connectionSelector.selectedKeys();
			final Iterator<SelectionKey> selectedKeysIterator = selectedKeys.iterator();
			
			while(selectedKeysIterator.hasNext()) {
				final SelectionKey selectedKey = selectedKeysIterator.next();
				selectedKeysIterator.remove();
				
				if(selectedKey.isValid()) {
					handleKeySelection(selectedKey);
				}
			}
		}
	}
	
	private void processPendingMessages() {
		while(true) {
			PwpMessageRequest messageRequest = null;
			synchronized(messageRequests) {
				if(!messageRequests.isEmpty()) {
					messageRequest = messageRequests.remove(0);
				}
			}
			if(messageRequest == null) {
				return;
			}
			final PwpMessageRequest messageToSend = messageRequest;				
			write(messageToSend.getConnection(), messageToSend.getMessageData());
		}
	}
	
	private void processPendingConnections() {
		while(true) {
			PwpPeer peer = null;
			synchronized(peerConnectionRequestQueue) {							 
				if(!peerConnectionRequestQueue.isEmpty()) {
					peer = peerConnectionRequestQueue.remove(0);
				}
			}	
			if(peer == null) {
				break;
			}			
			final boolean isNewPeer = inProgressConnectionRequests.add(peer) || 
					!activeConnections.containsKey(peer);
			
			if(!isNewPeer) {
				continue;
			}			
			
			SocketChannel peerConnection = null;			
			try {
				peerConnection = SocketChannel.open();
				peerConnection.configureBlocking(false);
				setChannelOptions(peerConnection);

				final SelectionKey selectionKey = peerConnection.register(connectionSelector, 
						SelectionKey.OP_CONNECT, peer);
				final boolean isConnected = peerConnection.connect(
						new InetSocketAddress(peer.getPeerIp(), peer.getPeerPort()));				
				
				if(isConnected) {
					System.out.println(name + ": Immediately connected to remote peer, removing interest in OP_CONNECT");
					selectionKey.interestOps(0);
					//TODO: Immediately send HANDSHAKE to this connection: write(peerConnection, HANDSHAKE.getBytes());
				}
			}
			catch(final IOException ioe) {
				System.err.println(name + ": Failed to connect to peer: " + ioe.getMessage());
				if(peerConnection != null) {
					inProgressConnectionRequests.remove(peer);
					closeConnection(peerConnection);					
				}
			}
		}
	}
	
	private void handleKeySelection(final SelectionKey selectedKey) {
		if(selectedKey.isAcceptable()) {
			//Handle a new connection request
			acceptConnection(selectedKey);
		}
		else if(selectedKey.isWritable()) {
			//Handle a write attempt to the channel
			writeToChannel(selectedKey);
		}
		else if(selectedKey.isReadable()) {
			//Handle a read attempt to the channel
			readFromChannel(selectedKey);
		}		
		else if(selectedKey.isConnectable()) {
			//Handle remote peer accepting our connection attempt
			finalizeConnection(selectedKey);			
		}
	}
	
	private void finalizeConnection(final SelectionKey selectedKey) {
		final SocketChannel peerConnection = ((SocketChannel)selectedKey.channel());		
		try {
			if(peerConnection.finishConnect()) {	
				selectedKey.interestOps(SelectionKey.OP_READ);
				ioBuffers.put(peerConnection, new PwpConnectionIO(
						PwpConnectionIO.READER_BUFFER_SIZE,
						PwpConnectionIO.WRITER_BUFFER_SIZE));
				
				System.out.println(name + ": Successfully connected to the remote peer");
				//TODO: Immediately send HANDSHAKE to this connection
			}
		} catch (final IOException ioe) {
			// Broken connection, disconnect the peer
			System.err.println(name + ": Broken connection attempt, disconnecting");
			
			final PwpPeer peer = (PwpPeer)selectedKey.attachment();
			inProgressConnectionRequests.remove(peer);
			closeConnection(peerConnection);
		}
	}
	
	private void readFromChannel(final SelectionKey selectedKey) {
		System.out.println(name + ": Handling OP_READ");			
		final SocketChannel connection = (SocketChannel)selectedKey.channel();
		final PwpConnectionIO messageReader = ioBuffers.get(connection);
		
		try {
			final List<PwpMessage> messages = messageReader.read(connection);
			if(!messages.isEmpty()) {
				//TODO: Check and handle handshake first (register PwpConnectionManager as PwpMessageListener for
				//incoming HANDSHAKES?)
				//TODO: Notify listeners that messages were received
			}			
		}
		catch(final IOException ioe) {
			closeConnection(connection);
			activeSessions.remove(connection);
			//TODO: Notify listeners that the connection is closed
		}
	}
	
	private void acceptConnection(final SelectionKey selectedKey) {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectedKey.channel();
		SocketChannel connection = null;
		try {
			connection = serverSocketChannel.accept();
			connection.configureBlocking(false);
			
			//TODO: Remove logging and gathering of remote peer's ip and port
			final InetSocketAddress connectionAddress = (InetSocketAddress)connection.getRemoteAddress();
			final String remotePeerIp = connectionAddress.getAddress().getHostAddress();
			final int remotePeerPort = connectionAddress.getPort();
			
			connection.register(selectedKey.selector(), SelectionKey.OP_READ);
			ioBuffers.put(connection, new PwpConnectionIO(
					PwpConnectionIO.READER_BUFFER_SIZE,
					PwpConnectionIO.WRITER_BUFFER_SIZE));
			
			System.out.println(name + ": Accepted remote connection: ip: " + remotePeerIp + ", port " + remotePeerPort);
		} catch (final IOException ioe) {
			System.err.println(name + ": Failed to accept incoming connection: " + ioe.getMessage());
			if(connection != null) {				
				closeConnection(connection);				
			}
		}
	}
	
	private void closeConnection(final SocketChannel connection) {
		try {
			connection.close();
			final InetSocketAddress connectionAddress = (InetSocketAddress)connection.getRemoteAddress();
			final String remotePeerIp = connectionAddress.getAddress().getHostAddress();
			final int remotePeerPort = connectionAddress.getPort();
			System.out.println("Disconnected peer: " + remotePeerIp + ":" + remotePeerPort);
		} 
		catch(final IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void setChannelOptions(final NetworkChannel channel) throws IOException {
		channel.setOption(StandardSocketOptions.SO_RCVBUF, PwpConnectionManager.SO_RCVBUF_VALUE);
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, PwpConnectionManager.SO_REUSEADDR);
	}
}