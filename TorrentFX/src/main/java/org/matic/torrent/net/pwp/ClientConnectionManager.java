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

import org.matic.torrent.gui.model.BitsView;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpMessage.MessageType;
import org.matic.torrent.tracking.listeners.PeerFoundListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
public class ClientConnectionManager implements PeerFoundListener {

    private static final long STALE_CONNECTION_THRESHOLD_TIME = 120000; //2m
    private static final long KEEP_ALIVE_INTERVAL = 10000;	//10 seconds

    private static final int SO_RCVBUF_VALUE = 4 * 1024;
    //private static final boolean SO_REUSEADDR = true;

    private static final int MAX_CONNECTIONS = 50;
    private static int CONNECTION_COUNT = 0;

    //Listeners for connection state changes and incoming peer messages
    private final Set<PwpConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<PwpMessageListener> messageListeners = new CopyOnWriteArraySet<>();

    //Different state connections: handshaken,initiated but not handshaken and not yet connected
    private final Map<PeerView, SelectionKey> handshakenConnections = new HashMap<>();
    private final Map<SelectionKey, PeerView> indeterminateConnections = new HashMap<>();
    private final List<PwpPeer> offlinePeers = new ArrayList<>();

    //Incoming message and connection request queues
    private final List<PwpMessageRequest> messageRequests = new ArrayList<>();
    private final List<PwpPeer> peerQueue = new ArrayList<>();

    //Torrents for which we accept incoming remote connections
    private final Map<InfoHash, TorrentView> servedTorrents = new HashMap<>();

    //Create a single HANDSHAKE message for each served torrent, more efficient
    private final Map<InfoHash, byte[]> cachedHandshakeMessageBytes = new HashMap<>();

    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
    private final Selector selector;

    private final int listenPort;

    //Last time we sent out a HANDSHAKE message on all handshaken connections
    private long lastKeepAliveSent = System.currentTimeMillis();

    /**
     *
     * Configure a new connection manager listening on a specified port
     *
     * @param listenPort Port to listen on for incoming connections
     * @throws IOException If a connection selector can't be opened
     */
    public ClientConnectionManager(final int listenPort) throws IOException {
        selector = Selector.open();
        this.listenPort = listenPort;
    }

    /**
     * Add a torrent for which to accept incoming remote peer connections.
     *
     * @param torrentView View to the target torrent
     */
    public void accept(final TorrentView torrentView) {
        synchronized(servedTorrents) {
            final InfoHash infoHash = torrentView.getInfoHash();
            servedTorrents.put(infoHash, torrentView);
            cachedHandshakeMessageBytes.put(infoHash, PwpMessageRequestFactory.buildHandshakeMessage(infoHash));
        }
    }

    /**
     * Remove a torrent for which we no longer accept incoming peer connections.
     *
     * @param torrentView View of the target torrent
     */
    public void reject(final TorrentView torrentView) {
        synchronized(servedTorrents) {
            cachedHandshakeMessageBytes.remove(torrentView.getInfoHash());
            servedTorrents.remove(torrentView);
        }
    }

    /**
     *
     * Queue peers and try to connect to them.
     *
     * @param peers Remote peers to queue for connection attempts.
     */
    @Override
    public void onPeersFound(final Collection<PwpPeer> peers, final String source) {
        synchronized(peerQueue) {
            peerQueue.addAll(peers.stream().filter(p -> !handshakenConnections.containsKey(p)
                    && !peerQueue.contains(p) && !offlinePeers.contains(p)).collect(Collectors.toList()));
        }
        selector.wakeup();
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

            while(true) {
                if(Thread.currentThread().isInterrupted()) {
                    Thread.interrupted();
                    indeterminateConnections.keySet().forEach(s -> closeChannel((SocketChannel)s.channel()));
                    indeterminateConnections.clear();
                    handshakenConnections.values().forEach(s -> closeChannel((SocketChannel)s.channel()));
                    handshakenConnections.clear();
                    offlinePeers.clear();

                    System.out.println("CCM: Shutdown completed");
                    break;
                }
                //Wait for the incoming events that we are interested in
                processPendingSelections();
                //Send any queued messages to the remote peers
                processPendingMessages();
                //Attempt to connect to queued peers, if any
                processPendingPeers();
            }
        }
        catch(final IOException ioe) {
            System.err.println("Server was shutdown due to an error: " + ioe.toString());
            if(!connectionExecutor.isShutdown()) {
                connectionExecutor.shutdownNow();
            }
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

            if(CONNECTION_COUNT < MAX_CONNECTIONS) {
                initConnection(newPeer);
            }
            else {
                offlinePeers.add(newPeer);
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

            final PwpMessageRequest finalRequest = messageRequest;

            final Collection<PeerView> messageRequestPeers = messageRequest.getPeers();
            final Collection<PeerView> targetPeers = messageRequestPeers == null || messageRequestPeers.isEmpty()?
                    handshakenConnections.keySet() : messageRequestPeers;

            targetPeers.forEach(p -> {
                final SelectionKey selectionKey = handshakenConnections.get(p);
                if(selectionKey != null) {
                    try {
                        final ClientSession clientSession = (ClientSession)selectionKey.attachment();
                        clientSession.putOnWriteQueue(finalRequest);
                        writeToChannel(selectionKey);
                    } catch(final IOException ioe) {
                        System.err.println("An error occurred while writing the peer message: " + ioe.toString());
                        disconnectPeer(selectionKey, p);
                    }
                }
            });

            if(finalRequest.getMessageType() == MessageType.KEEP_ALIVE) {
                lastKeepAliveSent = System.currentTimeMillis();
            }
        }
    }

    private void connectToOfflinePeer() {
        if(!offlinePeers.isEmpty() && (--CONNECTION_COUNT < MAX_CONNECTIONS)) {
            initConnection(offlinePeers.remove(0));
        }
    }

    private void processPendingSelections() throws IOException {
        final long timeLeftToWaitForKeepAlive = KEEP_ALIVE_INTERVAL - (System.currentTimeMillis() - lastKeepAliveSent);
        final int keysSelected = selector.select(timeLeftToWaitForKeepAlive > 0? timeLeftToWaitForKeepAlive : 0);

        if(keysSelected > 0) {
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

        //Check whether it is time to send KEEP_ALIVE to the connected peers
        if(!handshakenConnections.isEmpty() && (System.currentTimeMillis() - lastKeepAliveSent > KEEP_ALIVE_INTERVAL)) {
            synchronized(messageRequests) {
                messageRequests.add(new PwpMessageRequest(MessageType.KEEP_ALIVE,
                        PwpMessageRequestFactory.buildKeepAliveMessage(), null));
            }
        }

        //Disconnect all peers that haven't responded for a while
        /*final Set<Map.Entry<PeerView, SelectionKey>> staleHandshakenConnections = handshakenConnections.entrySet().stream().filter(e -> {
            final ClientSession session = (ClientSession)e.getValue().attachment();
            return (System.currentTimeMillis() - session.getLastMessageTime()) > STALE_CONNECTION_THRESHOLD_TIME;
        }).collect(Collectors.toSet());

        System.out.println("Disconnecting " + staleHandshakenConnections.size() + " connected peers due to inactivity");

        staleHandshakenConnections.forEach(e -> disconnectPeer(e.getValue(), e.getKey()));*/
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
            disconnectPeer(selectionKey, session.getPeerView());
        }
    }

    private void readFromChannel(final SelectionKey selectionKey) {
        final ClientSession session = (ClientSession)selectionKey.attachment();

        try {
            final Collection<PwpMessage> messages = session.read();
            if(!messages.isEmpty()) {
                final PeerView peerView = session.getPeerView();
                checkForHandshake(selectionKey, session, messages, peerView);

                if(!handshakenConnections.containsKey(peerView)) {
                    //Disconnect this peer, connection is not handshaken
                    disconnectPeer(selectionKey, peerView);
                    return;
                }

                checkForBitfield(selectionKey, messages, peerView);
                notifyMessagesReceived(messages, peerView);
            }
        }
        catch(final IOException ioe) {
            disconnectPeer(selectionKey, session.getPeerView());
        }
    }

    private void checkForHandshake(final SelectionKey selectionKey, final ClientSession session,
                                   final Collection<PwpMessage> messages, final PeerView peerView) {
        final Optional<PwpMessage> potentialHandshake = messages.stream().filter(
                m -> m.getMessageType() == PwpMessage.MessageType.HANDSHAKE).findAny();
        if(potentialHandshake.isPresent()) {
            final PwpHandshakeMessage handshake = (PwpHandshakeMessage)potentialHandshake.get();

            //Disconnect peer if do we not serve this torrent
            if(!servedTorrents.containsKey(handshake.getInfoHash())) {
                disconnectPeer(selectionKey, peerView);
                return;
            }

            final PwpPeer peer = session.getPeer();
            peer.setInfoHash(handshake.getInfoHash());
            peerView.setClientId(handshake.getPeerId());

            indeterminateConnections.remove(selectionKey);
            handshakenConnections.put(peerView, selectionKey);

            //Remove any pending connections to this peer
            synchronized(peerQueue) {
                peerQueue.remove(peer);
            }
        }
    }

    private void checkForBitfield(final SelectionKey selectionKey, final Collection<PwpMessage> messages,
                                  final PeerView peerView) {
        final Optional<PwpMessage> potentialBitfield = messages.stream().filter(
                m -> m.getMessageType() == PwpMessage.MessageType.BITFIELD).findAny();
        if(potentialBitfield.isPresent()) {
            final PwpRegularMessage bitfield = (PwpRegularMessage)potentialBitfield.get();

            final BitsView torrentPieces = servedTorrents.get(peerView.getInfoHash()).getAvailabilityView();
            final int expectedPieceCount = torrentPieces.getTotalPieces();
            final byte[] payload = bitfield.getPayload();
            final BitSet bitSet = BitSet.valueOf(payload);

            if(bitSet.length() > expectedPieceCount || (payload.length * Byte.SIZE < expectedPieceCount)) {
                //Disconnect this peer, invalid bitfield

                System.out.println("Invalid BITFIELD");

                disconnectPeer(selectionKey, peerView);
                return;
            }
            peerView.setHave(bitSet);
        }
    }

    private void disconnectPeer(final SelectionKey selectionKey, final PeerView peerView) {
        final SocketChannel channel = (SocketChannel)selectionKey.channel();
        indeterminateConnections.remove(selectionKey);
        handshakenConnections.remove(peerView);
        closeChannel(channel);
        notifyConnectionClosed(peerView);
        connectToOfflinePeer();
    }

    private void notifyMessagesReceived(final Collection<PwpMessage> messages, final PeerView peer) {
        messageListeners.forEach(l -> l.onMessagesReceived(messages, peer));
    }

    private void notifyPeerAdded(final PeerView peerView) {
        connectionListeners.forEach(l -> l.peerAdded(peerView));
    }

    private void notifyConnectionClosed(final PeerView peerView) {
        connectionListeners.forEach(l -> l.peerDisconnected(peerView));
    }

    private void finalizeConnection(final SelectionKey selectionKey) {
        final SocketChannel channel = ((SocketChannel)selectionKey.channel());
        final ClientSession clientSession = (ClientSession)selectionKey.attachment();
        final PeerView peerView = clientSession.getPeerView();
        try {
            if(channel.finishConnect()) {
                selectionKey.interestOps(SelectionKey.OP_READ);

                //Send a handshake to the remote peer
                final byte[] messageBytes = cachedHandshakeMessageBytes.get(peerView.getInfoHash());
                clientSession.putOnWriteQueue(new PwpMessageRequest(MessageType.HANDSHAKE,
                        messageBytes, Arrays.asList(peerView)));
                writeToChannel(selectionKey);
            }
        } catch (final IOException ioe) {
            // Broken connection, disconnect the peer
            synchronized(peerQueue) {
                peerQueue.remove(peerView);
            }
            disconnectPeer(selectionKey, peerView);
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

            final SelectionKey channelKey = channel.register(selector, SelectionKey.OP_READ);
            channelKey.attach(clientSession);

            indeterminateConnections.put(channelKey, clientSession.getPeerView());
            ++CONNECTION_COUNT;

            System.out.println("Accepted remote connection: ip: " + remotePeerIp + ", port " + remotePeerPort);

        } catch (final IOException ioe) {
            System.err.println("Failed to accept incoming connection: " + ioe.getMessage());
            if(channel != null) {
                closeChannel(channel);
            }
        }
    }

    private void closeChannel(final AbstractSelectableChannel channel) {
        if(channel == null) {
            return;
        }
        try {
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
        serverChannel.configureBlocking(false);
        serverChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface("tun0", listenPort));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Listening for incoming connection: " + serverChannel.toString());
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

            final PeerView peerView = session.getPeerView();
            indeterminateConnections.put(selectionKey, peerView);

            peerChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface("tun0", 0));

            final boolean isConnected = peerChannel.connect(
                    new InetSocketAddress(peer.getIp(), peer.getPort()));
            if(isConnected) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }

            ++CONNECTION_COUNT;
            notifyPeerAdded(peerView);
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
        //channel.setOption(StandardSocketOptions.SO_REUSEADDR, ClientConnectionManager.SO_REUSEADDR);
    }
}