/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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
import org.matic.torrent.peer.ClientProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 
 * A connection manager for PWP-connections. It listens for incoming connections and also
 * allows for adding new connections to remote peers.
 * 
 * @author vedran
 *
 */
public final class ClientConnectionManager {

	private static final int SO_RCVBUF_VALUE = 4 * 1024;	
	private static final boolean SO_REUSEADDR = true;

    private static final int MAX_CONNECTIONS = 50;
    private static int CONNECTION_COUNT = 0;

	private final Set<PwpConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
	private final Set<PwpMessageListener> messageListeners = new CopyOnWriteArraySet<>();

    private final Map<PwpPeer, SelectionKey> handshakenConnections = new HashMap<>();
    private final Map<SelectionKey, PwpPeer> indeterminateConnections = new HashMap<>();

    private final List<PwpMessageRequest> messageRequests = new ArrayList<>();
    private final List<PwpPeer> peerQueue = new ArrayList<>();

    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
    private final Selector selector;

    private final int listenPort;

	/**
	 * 
	 * Configure a new connection manager listening on a specified port and serving maxConnections connections
	 *
	 * @param listenPort Port to listen on for incoming connections
	 * @throws IOException If a connection selector can't be opened
	 */
	public ClientConnectionManager(final int listenPort) throws IOException {
        selector = Selector.open();
        this.listenPort = listenPort;
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
     * Queue peers and try to connect to them.
     *
     * @param peers Remote peers to queue for connection attempts.
     */
    public void connectTo(final Collection<PwpPeer> peers) {
        synchronized(peerQueue) {
            peerQueue.addAll(peers.stream().filter(p -> !handshakenConnections.containsKey(p)
                    && !peerQueue.contains(p)).collect(Collectors.toList()));
        }
        selector.wakeup();
    }

    /**
     * Send a message to a peer connected on the given connection
     *
     * @param messageRequest Message to be sent to receiving peer
     * @return Whether message request was added to message sender queue
     */
    public void send(final PwpMessageRequest messageRequest) {
        synchronized(messageRequests) {
            messageRequests.add(messageRequest);
        }
        selector.wakeup();
    }

    /**
     *
     * Start managing the connections (both incoming and outgoing)
     */
    public void launch() {
        connectionExecutor.execute(this::handleConnections);
    }

    /**
     * Initialize the shutdown of selector thread and allow it to die
     */
    public void shutdown() {
        connectionExecutor.shutdownNow();
        selector.wakeup();
    }

    private void handleConnections() {
        try(final ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            initServerChannel(serverChannel);

            System.out.println("CCM: Up and running");

            while(true) {
                if(Thread.currentThread().isInterrupted()) {
                    indeterminateConnections.keySet().forEach(s -> closeChannel((SocketChannel)s.channel()));
                    indeterminateConnections.clear();
                    handshakenConnections.values().forEach(s -> closeChannel((SocketChannel)s.channel()));
                    handshakenConnections.clear();

                    System.out.println("CCM: Shutdown completed");

                    break;
                }
                processPendingSelections();
                processPendingMessages();
                processPendingPeers();
            }
        } catch(final IOException ioe) {
            System.err.println("Server was shutdown due to an error: " + ioe);
        }
    }

    private void processPendingPeers() {
        while(true) {
            PwpPeer newPeer = null;
            synchronized(peerQueue) {
                if(!peerQueue.isEmpty()) {
                    newPeer = peerQueue.remove(0);
                }
            }
            if(newPeer == null) {
                return;
            }
            if(CONNECTION_COUNT++ < MAX_CONNECTIONS) {
                 initConnection(newPeer);
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
            final SelectionKey selectionKey = handshakenConnections.get(messageRequest.getPeer());
            if(selectionKey != null) {
                try {
                    final ClientSession clientSession = (ClientSession)selectionKey.attachment();
                    clientSession.putOnWriteQueue(messageRequest);
                    writeToChannel(selectionKey);
                } catch(final IOException ioe) {
                    //System.err.println("An error occurred while writing the peer message: " + ioe.toString());
                    handshakenConnections.remove(messageRequest.getPeer());
                    closeChannel((SocketChannel)selectionKey.channel());
                }
            }
        }
    }

    private void processPendingSelections() throws IOException {
        final int keysSelected = selector.select();
        if(keysSelected > 0) {

            //System.out.println("CCM: pending key selections = " + keysSelected);

            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
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

    private void handleKeySelection(final SelectionKey selectedKey) {
        if(selectedKey.isAcceptable()) {

            System.out.println("CCM: OP_ACCEPTABLE");

            //Handle a new connection request
            acceptConnection(selectedKey);
        }
        else if(selectedKey.isWritable()) {

            System.out.println("CCM: OP_WRITABLE");

            //Handle a write attempt to the channel
            writeToChannel(selectedKey);
        }
        else if(selectedKey.isReadable()) {

            //System.out.println("CCM: OP_READABLE");

            //Handle a read attempt to the channel
            readFromChannel(selectedKey);
        }
        else if(selectedKey.isConnectable()) {

            //System.out.println("CCM: OP_CONNECTABLE");

            //Handle remote peer accepting our connection attempt
            finalizeConnection(selectedKey);
        }
    }

    private void writeToChannel(final SelectionKey selectionKey) {
        if(selectionKey == null) {
            return;
        }

        final ClientSession session = (ClientSession)selectionKey.attachment();

        try {
            final boolean allBytesWritten = session.flushWriteQueue();
            if(!allBytesWritten) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (final IOException ioe) {
            indeterminateConnections.remove(selectionKey);
            handshakenConnections.remove(session.getPeer());
            closeChannel((SocketChannel)selectionKey.channel());
        }
    }

    private void readFromChannel(final SelectionKey selectionKey) {
        final SocketChannel channel = (SocketChannel)selectionKey.channel();
        final ClientSession session = (ClientSession)selectionKey.attachment();

        try {
            final Collection<PwpMessage> messages = session.read();

            //System.out.println("CCM: Received " + messages.size() + " messages");

            if(!messages.isEmpty()) {
                final Optional<PwpMessage> potentialHandshake = messages.stream().filter(
                        m -> m.getMessageType() == PwpMessage.MessageType.HANDSHAKE).findAny();

                final PwpPeer peer = session.getPeer();
                if(potentialHandshake.isPresent()) {

                    //System.out.println("CCM: Got HANDSHAKE from " + session.getPeer());

                    //final HandshakeMessage handshake = MessageParser.parseHandhake(potentialHandshake.get());

                    //peer.setInfoHash(handshake.getInfoHash());
                    indeterminateConnections.remove(selectionKey);
                    handshakenConnections.put(peer, selectionKey);
                    synchronized(peerQueue) {
                        peerQueue.remove(peer);
                    }
                }

                notifyMessagesReceived(messages, peer);
            }
        }
        catch(final IOException ioe) {

            //System.err.println("Got exception while reading from peer: " + ioe.toString());

            indeterminateConnections.remove(selectionKey);
            handshakenConnections.remove(session.getPeer());
            closeChannel(channel);
        }
    }

    private void notifyMessagesReceived(final Collection<PwpMessage> messages, final PwpPeer peer) {

        messages.forEach(m -> System.out.println("CCM: Received " + m.getMessageType() + " from " + peer));

        messageListeners.forEach(l -> l.onMessagesReceived(messages, peer));
    }

    private void finalizeConnection(final SelectionKey selectionKey) {
        final SocketChannel channel = ((SocketChannel)selectionKey.channel());
        final ClientSession clientSession = (ClientSession)selectionKey.attachment();
        final PwpPeer peer = clientSession.getPeer();
        try {
            if(channel.finishConnect()) {
                selectionKey.interestOps(SelectionKey.OP_READ);

                //Send a handshake to the remote peer
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(new byte[]{ClientSession.PROTOCOL_NAME_LENGTH});
                baos.write(ClientSession.PROTOCOL_NAME.getBytes());
                baos.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                baos.write(peer.getInfoHash().getBytes());
                baos.write(ClientProperties.PEER_ID.getBytes());

                clientSession.putOnWriteQueue(new PwpMessageRequest(baos.toByteArray(), peer));
                writeToChannel(selectionKey);
            }
        } catch (final IOException ioe) {
            // Broken connection, disconnect the peer
            //System.err.println("Broken connection attempt, disconnecting: " + ioe.toString());

            synchronized(peerQueue) {
                peerQueue.remove(peer);
            }
            indeterminateConnections.remove(selectionKey);
            closeChannel(channel);
        }
    }

    private void acceptConnection(final SelectionKey selectionKey) {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
        SocketChannel channel = null;
        try {
            channel = serverSocketChannel.accept();
            channel.configureBlocking(false);

            final InetSocketAddress connectionAddress = (InetSocketAddress)channel.getRemoteAddress();
            final String remotePeerIp = connectionAddress.getAddress().getHostAddress();
            final int remotePeerPort = connectionAddress.getPort();

            final PwpPeer peer = new PwpPeer(remotePeerIp, remotePeerPort, null);
            final ClientSession clientSession = new ClientSession(channel, peer);
            selectionKey.attach(clientSession);

            indeterminateConnections.put(selectionKey, peer);
            channel.register(selectionKey.selector(), SelectionKey.OP_READ);

            System.out.println("Accepted remote connection: ip: " + remotePeerIp + ", port " + remotePeerPort);
        } catch (final IOException ioe) {
            System.err.println("Failed to accept incoming connection: " + ioe.getMessage());
            if(channel != null) {
                closeChannel(channel);
            }
        }
    }

    private void closeChannel(final SocketChannel channel) {

        //System.out.println("CCM: Closing channel = " + channel);

        if(channel == null) {
            return;
        }
        try {
            --CONNECTION_COUNT;
            channel.close();
        }
        catch(final IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void initServerChannel(final ServerSocketChannel serverChannel) throws IOException {
        if(!serverChannel.isOpen()) {
            final String errorMessage = "Failed to start server on port " + listenPort;
            throw new IOException(errorMessage);
        }

        setChannelOptions(serverChannel);

        serverChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface("", listenPort), 200);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void initConnection(final PwpPeer peer) {
        SocketChannel peerChannel = null;
        try {
            peerChannel = SocketChannel.open();
            peerChannel.configureBlocking(false);

            setChannelOptions(peerChannel);

            final SelectionKey selectionKey = peerChannel.register(
                    selector, SelectionKey.OP_CONNECT, peer);
            final ClientSession session = new ClientSession(peerChannel, peer);
            selectionKey.attach(session);
            indeterminateConnections.put(selectionKey, peer);

            final boolean isConnected = peerChannel.connect(
                    new InetSocketAddress(peer.getPeerIp(), peer.getPeerPort()));
            if(isConnected) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }

            //System.out.println("CCM: Connection to a remote peer initiated");

        } catch(final IOException ioe) {
            if(peerChannel != null) {
                try {
                    peerChannel.close();
                } catch(final IOException e) {}
            }
        }
    }

	private void setChannelOptions(final NetworkChannel channel) throws IOException {
		channel.setOption(StandardSocketOptions.SO_RCVBUF, ClientConnectionManager.SO_RCVBUF_VALUE);
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, ClientConnectionManager.SO_REUSEADDR);
	}
}