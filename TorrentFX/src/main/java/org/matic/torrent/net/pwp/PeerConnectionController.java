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
package org.matic.torrent.net.pwp;

import org.matic.torrent.gui.model.BitsView;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.net.pwp.PwpMessage.MessageType;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.action.TorrentStatusChangeEvent;
import org.matic.torrent.queue.action.TorrentStatusChangeListener;
import org.matic.torrent.queue.enums.TorrentStatus;
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
 * @author Vedran Matic
 *
 */
public class PeerConnectionController implements PeerFoundListener, TorrentStatusChangeListener {

    private static final long STALE_CONNECTION_THRESHOLD_TIME = 300000; //5m
    private static final long KEEP_ALIVE_INTERVAL = 30000;	//30 seconds

    private static final int SO_RCVBUF_VALUE = 1024 * 1024;

    private static final int MAX_CONNECTIONS_PER_TORRENT = Integer.MAX_VALUE;   //100;
    private static final int GLOBAL_CONNECTION_LIMIT = Integer.MAX_VALUE;       //500;
    private static final int INCOMING_CONNECTION_LIMIT = Integer.MAX_VALUE;     //50;
    private int totalConnectionCount = 0;

    //Listeners for connection state changes and incoming peer messages
    private final Set<PwpConnectionStateListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<PwpMessageListener> messageListeners = new CopyOnWriteArraySet<>();

    //Different state connections: handshaken, initiated but not handshaken and not yet connected
    private final Map<InfoHash, Map<PeerView, SelectionKey>> handshakenConnections = new HashMap<>();
    private final Map<InfoHash, Map<SelectionKey, PeerView>> halfOpenConnections = new HashMap<>();
    private final Map<InfoHash, List<PwpPeer>> offlinePeers = new HashMap<>();

    //Incoming messages, connection request queues and torrent status changes
    private final List<TorrentStatusChangeEvent> statusChanges = new ArrayList<>();
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
    public PeerConnectionController(final int listenPort) throws IOException {
        selector = Selector.open();
        this.listenPort = listenPort;
    }

    @Override
    public void onTorrentStatusChanged(final TorrentStatusChangeEvent changeEvent) {
        synchronized(statusChanges) {
            if(changeEvent.getNewStatus() == TorrentStatus.STOPPED) {
                statusChanges.add(changeEvent);
                selector.wakeup();
            }
        }
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
            cachedHandshakeMessageBytes.put(infoHash, PwpMessageFactory.buildHandshakeMessage(infoHash));
            torrentView.addTorrentStatusChangeListener(this);
        }
    }

    /**
     * Remove a torrent for which we no longer accept incoming peer connections.
     *
     * @param torrentView View of the target torrent
     */
    public void reject(final TorrentView torrentView) {
        synchronized(servedTorrents) {
            torrentView.removeTorrentStatusChangeListener(this);
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
            final Collection<PwpPeer> newPeers = peers.stream().filter(p ->
                    (!handshakenConnections.containsKey(p.getInfoHash()) ||
                    !handshakenConnections.get(p.getInfoHash()).containsKey(p))
                    && !peerQueue.contains(p) &&
                    (!offlinePeers.containsKey(p.getInfoHash()) || !offlinePeers.get(p.getInfoHash()).contains(p))
            ).collect(Collectors.toList());

            newPeers.stream().forEach(p -> {
                final InfoHash infoHash = p.getInfoHash();
                final int torrentConnectionCount = getTorrentConnectionCount(infoHash);

                if(torrentConnectionCount < MAX_CONNECTIONS_PER_TORRENT &&
                        totalConnectionCount < GLOBAL_CONNECTION_LIMIT) {
                    peerQueue.add(p);
                }
                else {
                    offlinePeers.putIfAbsent(infoHash, new ArrayList<>());
                    offlinePeers.compute(infoHash, (key, offline) -> {
                        offline.add(p);
                        return offline;
                    });
                }
            });
        }
        selector.wakeup();
    }

    private int getTorrentConnectionCount(final InfoHash infoHash) {
        final int indeterminateConnectionCount = halfOpenConnections.containsKey(infoHash)?
                halfOpenConnections.get(infoHash).size() : 0;
        final int handshakenConnectionCount = handshakenConnections.containsKey(infoHash)?
                handshakenConnections.get(infoHash).size() : 0;

        return indeterminateConnectionCount + handshakenConnectionCount;
    }

    /**
     * Add a listener to be notified when a peer is connected/disconnected
     *
     * @param listener Listener to add
     */
    public void addConnectionListener(final PwpConnectionStateListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Remove a listener previously interested in connection state notifications
     *
     * @param listener Listener to remove
     */
    public void removeConnectionListener(final PwpConnectionStateListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Add a listener to be notified when a message is received from a peer
     *
     * @param listener Listener to add
     */
    public void addMessageListener(final PwpMessageListener listener) {
        messageListeners.add(listener);
    }

    /**
     * Remove a listener previously interested in message arrival notifications
     *
     * @param listener Listener to remove
     */
    public void removeMessageListener(final PwpMessageListener listener) {
        messageListeners.remove(listener);
    }

    /**
     * Send a message to a peer connected on the given connection.
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
     * Start managing the connections (both incoming and outgoing).
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
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            initServerChannel(serverChannel);
        }
        catch(final IOException ioe) {
            System.err.println("Server failed to start on port " + listenPort + " due to: " + ioe.toString());
        }

        while(true) {
            if(Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                if(serverChannel != null) {
                    try {
                        serverChannel.close();
                    }
                    catch (final IOException ioe) {
                        //Can't do much here, we are shutting down
                    }
                }
                cleanupConnections();

                System.out.println("CCM: Shutdown completed");
                break;
            }
            try {
                //Wait for the incoming events that we are interested in
                processPendingSelections();
            }
            catch(final IOException ioe) {
                System.err.println("Server selector failed due to: " + ioe.toString() + " Shutting down server.");
                if(!connectionExecutor.isShutdown()) {
                    connectionExecutor.shutdownNow();
                }
                continue;
            }
            //Send any queued messages to the remote peers
            processPendingMessages();
            //Attempt to connect to queued peers, if any
            processPendingPeers();
            //React on torrent status changes
            processTorrentStatusChanges();
        }
    }

    private void cleanupConnections() {
        halfOpenConnections.values().stream().flatMap(m -> m.keySet().stream()).forEach(
                s -> closeChannel((SocketChannel)s.channel()));
        halfOpenConnections.clear();
        handshakenConnections.values().stream().flatMap(m -> m.values().stream()).forEach(
                s -> closeChannel((SocketChannel)s.channel()));
        handshakenConnections.clear();
        offlinePeers.clear();
    }

    private void processTorrentStatusChanges() {
        while(true) {
            TorrentStatusChangeEvent statusChange = null;
            synchronized(statusChanges) {
                if(!statusChanges.isEmpty()) {
                    statusChange = statusChanges.remove(0);
                }
            }
            if(statusChange == null) {
                return;
            }

            final InfoHash infoHash = statusChange.getTorrentView().getInfoHash();
            if(!handshakenConnections.containsKey(infoHash)) {
                return;
            }

            final Map<PeerView, SelectionKey> torrentPeers = handshakenConnections.get(infoHash);

            if(torrentPeers != null) {
                if(statusChange.getNewStatus() == TorrentStatus.STOPPED) {
                    handshakenConnections.remove(infoHash);
                    torrentPeers.entrySet().forEach(p ->
                        disconnectPeer(p.getValue(), p.getKey(), "Torrent stopped"));
                }
            }
        }
    }

    private void processPendingPeers() {
        while(true) {
            PwpPeer peer = null;
            synchronized(peerQueue) {
                if(!peerQueue.isEmpty()) {
                    peer = peerQueue.remove(ClientProperties.RANDOM_INSTANCE.nextInt(peerQueue.size()));
                }
            }
            if(peer == null) {
                return;
            }

            final PwpPeer newPeer = peer;
            final int torrentConnectionCount = getTorrentConnectionCount(newPeer.getInfoHash());

            final TorrentView targetTorrent;
            synchronized(servedTorrents) {
                targetTorrent = servedTorrents.get(newPeer.getInfoHash());
                if (targetTorrent == null) {
                    continue;
                }
            }
            final TorrentStatus torrentStatus = targetTorrent.getStatus();

            if((torrentStatus != TorrentStatus.STOPPED && torrentStatus != TorrentStatus.ERROR) &&
                    totalConnectionCount < GLOBAL_CONNECTION_LIMIT &&
                    torrentConnectionCount < MAX_CONNECTIONS_PER_TORRENT) {
                initConnection(newPeer);
            } else {
                offlinePeers.putIfAbsent(newPeer.getInfoHash(), new ArrayList<>());
                offlinePeers.compute(newPeer.getInfoHash(), (key, offline) -> {
                    if(!offline.contains(newPeer)) {
                        offline.add(newPeer);
                    }
                    return offline;
                });
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
            final Collection<PeerView> targetPeers = messageRequestPeers.isEmpty()?
                    handshakenConnections.values().stream().flatMap(
                            m -> m.keySet().stream()).collect(Collectors.toSet()) : messageRequestPeers;

            targetPeers.forEach(p -> {
                final Map<PeerView, SelectionKey> peerSelectionKeys = handshakenConnections.get(p.getInfoHash());
                if(peerSelectionKeys == null) {
                    return;
                }
                final SelectionKey selectionKey = peerSelectionKeys.get(p);
                if(selectionKey != null) {
                    try {
                        final PeerSession peerSession = (PeerSession)selectionKey.attachment();
                        peerSession.putOnWriteQueue(finalRequest);
                        writeToChannel(selectionKey);
                    } catch(final IOException ioe) {
                        disconnectPeer(selectionKey, p, "Channel write failed: " + ioe);
                    }
                }
            });

            if(finalRequest.getRequestType() == MessageType.KEEP_ALIVE) {
                lastKeepAliveSent = System.currentTimeMillis();
            }
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

        //Check whether it is time to send KEEP_ALIVE to the handshaken peers
        if(!handshakenConnections.isEmpty() && (System.currentTimeMillis() - lastKeepAliveSent > KEEP_ALIVE_INTERVAL)) {
            synchronized(messageRequests) {
                messageRequests.add(new PwpMessageRequest(new PwpMessage(MessageType.KEEP_ALIVE,
                        PwpMessageFactory.buildKeepAliveMessage())));
            }
        }

        //Disconnect all peers that haven't responded for a while
        final Set<Map.Entry<PeerView, SelectionKey>> staleHandshakenConnections = handshakenConnections.values().stream()
                .flatMap(m -> m.entrySet().stream()).filter(e -> {
                    final PeerSession session = (PeerSession) e.getValue().attachment();
                    return (System.currentTimeMillis() - session.getLastActivityTime()) > STALE_CONNECTION_THRESHOLD_TIME;
                }).collect(Collectors.toSet());
        staleHandshakenConnections.forEach(e ->
            disconnectPeer(e.getValue(), e.getKey(), "Stale connection"));

        //Cancel pending connection attempts if they are taking too long
        final Set<Map.Entry<SelectionKey, PeerView>> staleIndeterminateConnections =
                halfOpenConnections.values().stream().flatMap(m -> m.entrySet().stream()).filter(e -> {
                    final PeerSession session = (PeerSession) e.getKey().attachment();
                    return session.getLastActivityTime() < System.currentTimeMillis() - 10000;
                }).collect(Collectors.toSet());
        staleIndeterminateConnections.stream().map(e -> {
            disconnectPeer(e.getKey(), e.getValue(), "Stale half-open connection");
            return e.getValue().getInfoHash();
        }).forEach(infoHash -> halfOpenConnections.remove(infoHash));
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

        final PeerSession session = (PeerSession)selectionKey.attachment();

        try {
            final boolean allBytesWritten = session.flushWriteQueue();
            if(!allBytesWritten) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (final IOException ioe) {
            disconnectPeer(selectionKey, session.getPeerView(), "Channel flush failed: " + ioe);
        }
    }

    private void readFromChannel(final SelectionKey selectionKey) {
        final PeerSession session = (PeerSession)selectionKey.attachment();

        try {
            final Collection<PwpMessage> messages = session.read();
            if(!messages.isEmpty()) {
                final PeerView peerView = session.getPeerView();
                checkForHandshake(selectionKey, session, messages);

                final Map<PeerView, SelectionKey> targetPeers = handshakenConnections.get(peerView.getInfoHash());

                if(targetPeers == null || !targetPeers.containsKey(peerView)) {
                    return;
                }

                checkForBitfield(selectionKey, messages, peerView);
                messages.forEach(m -> notifyMessageReceived(new PwpMessageEvent(m, peerView)));
            }
        }
        catch(final IOException | InvalidPeerMessageException e) {
            disconnectPeer(selectionKey, session.getPeerView(), "Channel read failed: " + e);
        }
    }

    private void checkForHandshake(final SelectionKey selectionKey, final PeerSession session,
                                   final Collection<PwpMessage> messages) {
        final Optional<PwpMessage> potentialHandshake = messages.stream().filter(
                m -> m.getMessageType() == PwpMessage.MessageType.HANDSHAKE).findAny();
        if(potentialHandshake.isPresent()) {
            final PwpHandshakeMessage handshake = (PwpHandshakeMessage)potentialHandshake.get();
            final InfoHash infoHash = handshake.getInfoHash();
            final PeerView peerView = session.getPeerView();

            final TorrentView targetTorrent;
            final int torrentConnectionCount;

            synchronized(servedTorrents) {
                torrentConnectionCount = getTorrentConnectionCount(infoHash);
                targetTorrent = servedTorrents.get(infoHash);
            }

            //Disconnect the peer if do we not serve this torrent or have reached torrent connections limit
            if(targetTorrent == null || !(targetTorrent.getStatus() == TorrentStatus.ACTIVE ||
                    targetTorrent.getStatus() == TorrentStatus.PAUSED) ||
                    torrentConnectionCount >= MAX_CONNECTIONS_PER_TORRENT) {
                disconnectPeer(selectionKey, peerView, "Either not served torrent or connection limit reached.");
                return;
            }

            if(!session.isIncoming()) {
                targetTorrent.getProgress().addPeer(peerView.getPeer());
            }

            peerView.setInfoHash(infoHash);
            peerView.setClientId(handshake.getPeerId());

            final Map<SelectionKey, PeerView> incomingConnections = halfOpenConnections.get(null);
            if(incomingConnections != null) {
               incomingConnections.remove(selectionKey);
            }

            handshakenConnections.putIfAbsent(peerView.getInfoHash(), new HashMap<>());
            handshakenConnections.compute(peerView.getInfoHash(), (key, connections) -> {
                connections.put(peerView, selectionKey);
                return connections;
            });

            //Remove any pending connections to this peer
            synchronized(peerQueue) {
                peerQueue.remove(peerView.getPeer());
            }

            notifyConnectionStateChange(peerView, true, "Handshake success");
        }
    }

    private void checkForBitfield(final SelectionKey selectionKey, final Collection<PwpMessage> messages,
                                  final PeerView peerView) {
        final Optional<PwpMessage> potentialBitfield = messages.stream().filter(
                m -> m.getMessageType() == PwpMessage.MessageType.BITFIELD).findAny();
        if(potentialBitfield.isPresent()) {
            final PwpMessage bitfield = potentialBitfield.get();

            final BitsView torrentPieces = servedTorrents.get(peerView.getInfoHash()).getAvailabilityView();
            final int expectedPieceCount = torrentPieces.getTotalPieces();
            final BitSet bitSet = PwpMessageFactory.parseBitfieldMessage(bitfield);

            if(bitSet.length() > expectedPieceCount || (bitfield.getPayload().length * Byte.SIZE < expectedPieceCount)) {
                //Disconnect this peer, invalid bitfield
                disconnectPeer(selectionKey, peerView, "Invalid bitfield");
                return;
            }
        }
    }

    private void disconnectPeer(final SelectionKey selectionKey, final PeerView peerView, final String cause) {
        final SocketChannel channel = (SocketChannel)selectionKey.channel();
        final InfoHash peerInfoHash = peerView.getInfoHash();

        final Map<SelectionKey, PeerView> indeterminateConnectionsForTorrent = halfOpenConnections.get(peerInfoHash);
        if(indeterminateConnectionsForTorrent != null) {
            indeterminateConnectionsForTorrent.remove(selectionKey);
        }

        final Map<PeerView, SelectionKey> handshakenConnectionsForTorrent = handshakenConnections.get(peerInfoHash);
        if(handshakenConnectionsForTorrent != null ) {
            handshakenConnectionsForTorrent.remove(peerView);
        }

        --totalConnectionCount;
        closeChannel(channel);
        notifyConnectionStateChange(peerView, false, cause);

        synchronized(servedTorrents) {
            final TorrentView targetTorrent = servedTorrents.get(peerInfoHash);
            if(targetTorrent != null) {
                final TorrentStatus torrentStatus = targetTorrent.getStatus();
                if(!(torrentStatus == TorrentStatus.ACTIVE || torrentStatus == TorrentStatus.PAUSED)) {
                    return;
                }
            }
        }

        //Make a connection to an offline peer, to replace the disconnected peer
        final List<PwpPeer> targetOfflinePeers = offlinePeers.get(peerInfoHash);
        if(targetOfflinePeers != null) {
            targetOfflinePeers.add(peerView.getPeer());


            int torrentConnectionCount = getTorrentConnectionCount(peerInfoHash);
            while (!targetOfflinePeers.isEmpty() && (totalConnectionCount < GLOBAL_CONNECTION_LIMIT)
                    && torrentConnectionCount < MAX_CONNECTIONS_PER_TORRENT) {
                initConnection(targetOfflinePeers.remove(0));
                torrentConnectionCount = getTorrentConnectionCount(peerInfoHash);
            }
        }

        //Store to offline connections if we initiated this connection originally
        /*final PeerSession session = (PeerSession)selectionKey.attachment();
        if(!session.isIncoming()) {
            offlinePeers.putIfAbsent(peerInfoHash, new ArrayList<>());
            offlinePeers.compute(peerInfoHash, (key, offline) -> {
                final PwpPeer peer = session.getPeer();
                if(!offline.contains(peer)) {
                    offline.add(peer);
                }
                return offline;
            });
        }*/
    }

    private void notifyMessageReceived(final PwpMessageEvent messageEvent) {
        messageListeners.stream().filter(l -> l.getPeerMessageAcceptanceFilter().test(messageEvent)).forEach(
                l -> l.onMessageReceived(messageEvent));
    }

    private void notifyConnectionStateChange(final PeerView peerView, final boolean connected, final String cause) {
        final PeerConnectionStateChangeEvent event = new PeerConnectionStateChangeEvent(
                peerView, connected? PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.CONNECTED :
                PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED, cause);
        connectionListeners.stream().filter(
                l -> l.getPeerStateChangeAcceptanceFilter().test(event)).forEach(l -> l.peerConnectionStateChanged(event));
    }

    private void finalizeConnection(final SelectionKey selectionKey) {
        final SocketChannel channel = ((SocketChannel)selectionKey.channel());
        final PeerSession peerSession = (PeerSession)selectionKey.attachment();
        final PeerView peerView = peerSession.getPeerView();
        try {
            if(channel.finishConnect()) {
                selectionKey.interestOps(SelectionKey.OP_READ);
                final InfoHash infoHash = peerView.getInfoHash();
                //Send a handshake to the remote peer
                synchronized (servedTorrents) {
                    if(!cachedHandshakeMessageBytes.containsKey(infoHash)) {
                        throw new IOException("Torrent not served anymore: " + peerView);
                    }
                    final byte[] messageBytes = cachedHandshakeMessageBytes.get(infoHash);
                    peerSession.putOnWriteQueue(new PwpMessageRequest(new PwpMessage(MessageType.HANDSHAKE,
                            messageBytes), peerView));
                }
                writeToChannel(selectionKey);
            }
        } catch (final IOException ioe) {
            // Broken connection, disconnect the peer
            synchronized(peerQueue) {
                peerQueue.remove(peerView);
            }
            disconnectPeer(selectionKey, peerView, "Failed to finalize connection: " + ioe);
        }
    }

    private void acceptConnection(final SelectionKey selectionKey) {
        //Check if we can accept more connections or whether a limit has been reached
        final int incomingConnectionCount = halfOpenConnections.containsKey(null)?
                halfOpenConnections.get(null).size() : 0;

        if(totalConnectionCount >= GLOBAL_CONNECTION_LIMIT ||
                incomingConnectionCount >= INCOMING_CONNECTION_LIMIT) {
            selectionKey.cancel();

            System.out.println("Cancelled incoming connection, limit reached: total connections = " +
                    totalConnectionCount + ", incoming connections = " + incomingConnectionCount);

            return;
        }
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
        SocketChannel channel = null;

        try {
            channel = serverSocketChannel.accept();
            channel.configureBlocking(false);

            final InetSocketAddress connectionAddress = (InetSocketAddress)channel.getRemoteAddress();
            final String remotePeerIp = connectionAddress.getAddress().getHostAddress();
            final int remotePeerPort = connectionAddress.getPort();

            final PwpPeer peer = new PwpPeer(remotePeerIp, remotePeerPort, null);
            final PeerSession peerSession = new PeerSession(channel, new PeerView(peer), true);

            final SelectionKey channelKey = channel.register(selector, SelectionKey.OP_READ);
            channelKey.attach(peerSession);

            halfOpenConnections.putIfAbsent(null, new HashMap<>());
            halfOpenConnections.compute(null, (key, connections) -> {
                connections.put(channelKey, peerSession.getPeerView());
                return connections;
            });

            ++totalConnectionCount;
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
        serverChannel.bind(NetworkUtilities.getSocketAddress(listenPort));
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
            final PeerView peerView = new PeerView(peer);
            final PeerSession session = new PeerSession(peerChannel, peerView, false);
            selectionKey.attach(session);

            halfOpenConnections.putIfAbsent(peer.getInfoHash(), new HashMap<>());
            halfOpenConnections.compute(peer.getInfoHash(), (key, connections) -> {
                connections.put(selectionKey, session.getPeerView());
                return connections;
            });

            peerChannel.bind(NetworkUtilities.getSocketAddress(0));

            final boolean isConnected = peerChannel.connect(
                    new InetSocketAddress(peer.getIp(), peer.getPort()));

            if(isConnected) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
            ++totalConnectionCount;
            //notifyPeerConnected(peerView);
        } catch(final IOException ioe) {
            if(peerChannel != null) {
                try {
                    peerChannel.close();
                } catch(final IOException e) {}
            }
        }
    }

    private void setChannelOptions(final NetworkChannel channel) throws IOException {
        channel.setOption(StandardSocketOptions.SO_RCVBUF, PeerConnectionController.SO_RCVBUF_VALUE);
    }
}