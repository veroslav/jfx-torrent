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
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.TorrentStatusChangeEvent;
import org.matic.torrent.queue.TorrentStatusChangeListener;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.tracking.listeners.PeerFoundListener;
import org.matic.torrent.utils.UnitConverter;

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
 * @author Vedran Matic
 *
 */
public class ClientConnectionManager implements PeerFoundListener, TorrentStatusChangeListener {

    private static final long STALE_CONNECTION_THRESHOLD_TIME = 300000; //5m
    private static final long KEEP_ALIVE_INTERVAL = 10000;	//10 seconds

    private static final int SO_RCVBUF_VALUE = 4 * 1024;
    //private static final boolean SO_REUSEADDR = true;

    private static final int MAX_CONNECTIONS_PER_TORRENT = 100;
    private static final int GLOBAL_CONNECTION_LIMIT = 500;
    private static final int INCOMING_CONNECTION_LIMIT = 50;
    private int totalConnectionCount = 0;

    //Listeners for connection state changes and incoming peer messages
    private final Set<PwpConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<PwpMessageListener> messageListeners = new CopyOnWriteArraySet<>();

    //Different state connections: handshaken, initiated but not handshaken and not yet connected
    private final Map<InfoHash, Map<PeerView, SelectionKey>> handshakenConnections = new HashMap<>();
    private final Map<InfoHash, Map<SelectionKey, PeerView>> indeterminateConnections = new HashMap<>();
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
    public ClientConnectionManager(final int listenPort) throws IOException {
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
            cachedHandshakeMessageBytes.put(infoHash, PwpMessageRequestFactory.buildHandshakeMessage(infoHash));
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
        final int indeterminateConnectionCount = indeterminateConnections.containsKey(infoHash)?
                indeterminateConnections.get(infoHash).size() : 0;
        final int handshakenConnectionCount = handshakenConnections.containsKey(infoHash)?
                handshakenConnections.get(infoHash).size() : 0;
        /*final int incomingConnectionCount = indeterminateConnections.containsKey(null)?
                indeterminateConnections.get(null).size() : 0;

        System.out.println("TorrentConCount: " + (indeterminateConnectionCount + handshakenConnectionCount) +
                " [indeterminate: " + indeterminateConnectionCount + ", handshaken: " + handshakenConnectionCount +
                "], incoming : " + incomingConnectionCount + ", totalConCount: " + totalConnectionCount);*/

        return indeterminateConnectionCount + handshakenConnectionCount;
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
        indeterminateConnections.values().stream().flatMap(m -> m.keySet().stream()).forEach(
                s -> closeChannel((SocketChannel)s.channel()));
        indeterminateConnections.clear();
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
                    torrentPeers.entrySet().forEach(p -> {
                        //System.out.println("processTorrentStatusChanges().disconnect: " + p.getKey());
                        disconnectPeer(p.getValue(), p.getKey(), "processTorrentStatusChanges()");
                    });
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

            synchronized(servedTorrents) {
                final TorrentView targetTorrent = servedTorrents.get(newPeer.getInfoHash());
                if(targetTorrent == null) {
                    continue;
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
                    handshakenConnections.values().stream().flatMap(
                            m -> m.keySet().stream()).collect(Collectors.toSet()) : messageRequestPeers;

            targetPeers.forEach(p -> {
                final SelectionKey selectionKey = handshakenConnections.get(p.getInfoHash()).get(p);
                if(selectionKey != null) {
                    try {
                        final ClientSession clientSession = (ClientSession)selectionKey.attachment();
                        clientSession.putOnWriteQueue(finalRequest);
                        writeToChannel(selectionKey);
                    } catch(final IOException ioe) {
                        //System.err.println("An error occurred while writing the peer message: " + ioe.toString());
                        disconnectPeer(selectionKey, p, "processPendingMessages(error: " + ioe.getMessage() + ")");
                    }
                }
            });

            if(finalRequest.getMessageType() == MessageType.KEEP_ALIVE) {
                lastKeepAliveSent = System.currentTimeMillis();
            }
        }
    }

    private void processPendingSelections() throws IOException {
        //TODO: Use KEEP_ALIVE_INTERVAL for selector timeout
        /*final long timeLeftToWaitForKeepAlive = KEEP_ALIVE_INTERVAL - (System.currentTimeMillis() - lastKeepAliveSent);
        final int keysSelected = selector.select(timeLeftToWaitForKeepAlive > 0? timeLeftToWaitForKeepAlive : 0);*/
        final int keysSelected = selector.select(5000); //5 seconds

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

                System.out.print("KEEP_ALIVE: Total? " + totalConnectionCount + ", indetermined? " + indeterminateConnections.values().stream().flatMap(
                        c -> c.entrySet().stream()).count() +
                        ", offline? " + offlinePeers.values().stream().flatMap(c -> c.stream()).count());

                for(final InfoHash infoHash : handshakenConnections.keySet()) {
                    System.out.println("\nKEEP_ALIVE: Handshaken connections = " + handshakenConnections.get(infoHash).size());
                }

                messageRequests.add(new PwpMessageRequest(MessageType.KEEP_ALIVE,
                        PwpMessageRequestFactory.buildKeepAliveMessage(), null));
            }
        }

        /* TODO: Stale connection checks below have a bug that causes invalid connection counting. FIX NEEDED! */
        //Disconnect all peers that haven't responded for a while
        final Set<Map.Entry<PeerView, SelectionKey>> staleHandshakenConnections = handshakenConnections.values().stream()
                .flatMap(m -> m.entrySet().stream()).filter(e -> {
                    final ClientSession session = (ClientSession) e.getValue().attachment();
                    return (System.currentTimeMillis() - session.getLastActivityTime()) > STALE_CONNECTION_THRESHOLD_TIME;
                }).collect(Collectors.toSet());
        staleHandshakenConnections.forEach(e -> {
            disconnectPeer(e.getValue(), e.getKey(), "processPendingSelections(stale handshaken)");
        });

        //Cancel pending connection attempts if they are taking too long
        final Set<Map.Entry<SelectionKey, PeerView>> staleIndeterminateConnections =
                indeterminateConnections.values().stream().flatMap(m -> m.entrySet().stream()).filter(e -> {
                    final ClientSession session = (ClientSession) e.getKey().attachment();
                    return session.getLastActivityTime() < System.currentTimeMillis() - 10000;
                }).collect(Collectors.toSet());
        staleIndeterminateConnections.stream().map(e -> {
            disconnectPeer(e.getKey(), e.getValue(), "processPendingSelections(stale indeterminate)");
            return e.getValue().getInfoHash();
        }).forEach(infoHash -> indeterminateConnections.remove(infoHash));
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
            //System.out.println("writeToChannel().disconnect: " + session.getPeerView());
            disconnectPeer(selectionKey, session.getPeerView(), "writeToChannel(" + ioe.getMessage() + ")");
        }
    }

    private void readFromChannel(final SelectionKey selectionKey) {
        final ClientSession session = (ClientSession)selectionKey.attachment();

        try {
            final Collection<PwpMessage> messages = session.read();
            if(!messages.isEmpty()) {
                final PeerView peerView = session.getPeerView();
                checkForHandshake(selectionKey, session, messages, peerView);

                final Map<PeerView, SelectionKey> targetPeers = handshakenConnections.get(peerView.getInfoHash());

                if(targetPeers == null || !targetPeers.containsKey(peerView)) {
                    //Ignore this peer, connection is not handshaken OR the target torrent is not served
                    //System.out.println("readFromChannel().disconnect not shaken: " + peerView);
                    //disconnectPeer(selectionKey, peerView, "readFromChannel(not_handhaken_connection)");
                    return;
                }

                checkForBitfield(selectionKey, messages, peerView);
                notifyMessagesReceived(messages, peerView);
            }
        }
        catch(final IOException | InvalidPeerMessageException e) {
            //System.err.println("readFromChannel().disconnect exception: " + session.getPeerView() + e);
            disconnectPeer(selectionKey, session.getPeerView(), "readFromChannel(exception: " + e.getMessage() + ")");
        }
    }

    private void checkForHandshake(final SelectionKey selectionKey, final ClientSession session,
                                   final Collection<PwpMessage> messages, final PeerView peerView) {
        final Optional<PwpMessage> potentialHandshake = messages.stream().filter(
                m -> m.getMessageType() == PwpMessage.MessageType.HANDSHAKE).findAny();
        if(potentialHandshake.isPresent()) {
            final PwpHandshakeMessage handshake = (PwpHandshakeMessage)potentialHandshake.get();
            final InfoHash infoHash = handshake.getInfoHash();

            synchronized(servedTorrents) {
                //Disconnect the peer if do we not serve this torrent or have reached torrent connections limit
                final int torrentConnectionCount = getTorrentConnectionCount(infoHash);
                final TorrentView targetTorrent = servedTorrents.get(infoHash);
                if(targetTorrent == null || !(targetTorrent.getStatus() == TorrentStatus.ACTIVE ||
                        targetTorrent.getStatus() == TorrentStatus.PAUSED) ||
                        torrentConnectionCount >= MAX_CONNECTIONS_PER_TORRENT) {
                    disconnectPeer(selectionKey, peerView, "checkForHandshake(not_served OR #torrent_connections ("
                            + torrentConnectionCount + ") >= " + MAX_CONNECTIONS_PER_TORRENT +")");
                    return;
                }
            }

            final PwpPeer peer = session.getPeer();
            peer.setInfoHash(infoHash);
            peerView.setClientId(handshake.getPeerId());

            final Map<SelectionKey, PeerView> incomingConnections = indeterminateConnections.get(null);
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
                peerQueue.remove(peer);
            }

            notifyPeerAdded(peerView);
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

            final BitSet bitSet = BitSet.valueOf(UnitConverter.reverseBits(payload));
            final BitsView bitsView = new BitsView(expectedPieceCount, bitSet);

            if(bitSet.length() > expectedPieceCount || (payload.length * Byte.SIZE < expectedPieceCount)) {
                //Disconnect this peer, invalid bitfield
                System.out.println("checkForBitfield().disconnect: " + peerView);
                disconnectPeer(selectionKey, peerView, "checkForBitfield(invalid_bitfield)");
                System.out.println("Invalid BITFIELD received from: " + peerView);
                return;
            }
            peerView.setPieces(bitsView);
        }
    }

    private void disconnectPeer(final SelectionKey selectionKey, final PeerView peerView, final String caller) {
        final SocketChannel channel = (SocketChannel)selectionKey.channel();
        final InfoHash peerInfoHash = peerView.getInfoHash();

        final Map<SelectionKey, PeerView> indeterminateConnectionsForTorrent = indeterminateConnections.get(peerInfoHash);
        if(indeterminateConnectionsForTorrent != null && (indeterminateConnectionsForTorrent.remove(selectionKey) != null)) {
            //--totalConnectionCount;
            /*System.out.println("DisconnectPeer(" + caller + "): peer = " + peerView.toString() + ", infoHash: "
                    + peerInfoHash + ", matching indeterminate connection");*/
        }

        final Map<PeerView, SelectionKey> handshakenConnectionsForTorrent = handshakenConnections.get(peerInfoHash);
        if(handshakenConnectionsForTorrent != null && (handshakenConnectionsForTorrent.remove(peerView) != null)) {
            //--totalConnectionCount;
            /*System.out.println("DisconnectPeer(" + caller + "): peer = " + peerView.toString() + ", infoHash: "
                    + peerInfoHash + ", matching handshaken connection");*/
        }

        --totalConnectionCount;
        closeChannel(channel);
        notifyConnectionClosed(peerView);

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
            int torrentConnectionCount = getTorrentConnectionCount(peerInfoHash);
            while (!targetOfflinePeers.isEmpty() && (totalConnectionCount < GLOBAL_CONNECTION_LIMIT) &&
                    torrentConnectionCount < MAX_CONNECTIONS_PER_TORRENT) {

                //System.out.println("disconnectPeers(): indeterminate connections? " + indeterminateConnectionCount);

                initConnection(targetOfflinePeers.remove(0));
                torrentConnectionCount = getTorrentConnectionCount(peerInfoHash);
            }
        }

        //Store to offline connections if we initiated this connection originally
        /*final ClientSession session = (ClientSession)selectionKey.attachment();
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
            //System.out.println("finalizeConnection().disconnect: " + peerView);
            disconnectPeer(selectionKey, peerView, "finalizeConnection(error: " + ioe.getMessage() + ")");
        }
    }

    private void acceptConnection(final SelectionKey selectionKey) {
        //Check if we can accept more connections or whether a limit has been reached
        final int incomingConnectionCount = indeterminateConnections.containsKey(null)?
                indeterminateConnections.get(null).size() : 0;

        /*System.out.println("acceptConnection(1): Incoming = " + incomingConnectionCount + ", total: "
                + totalConnectionCount + "\n");*/

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

            /*System.out.println("acceptConnection(2): Incoming = " + incomingConnectionCount + ", total: "
                    + totalConnectionCount + "\n");*/

            channel = serverSocketChannel.accept();
            channel.configureBlocking(false);

            final InetSocketAddress connectionAddress = (InetSocketAddress)channel.getRemoteAddress();
            final String remotePeerIp = connectionAddress.getAddress().getHostAddress();
            final int remotePeerPort = connectionAddress.getPort();

            //System.out.println("Remote connection: (" + remotePeerIp + ":" + remotePeerPort + ")");

            final PwpPeer peer = new PwpPeer(remotePeerIp, remotePeerPort, null);
            final ClientSession clientSession = new ClientSession(channel, peer, true);

            final SelectionKey channelKey = channel.register(selector, SelectionKey.OP_READ);
            channelKey.attach(clientSession);

            indeterminateConnections.putIfAbsent(null, new HashMap<>());
            indeterminateConnections.compute(null, (key, connections) -> {
                connections.put(channelKey, clientSession.getPeerView());
                return connections;
            });

            ++totalConnectionCount;
            /*System.out.println("acceptConnection(3): Incoming con added, total incoming = "
                    + indeterminateConnections.get(null).size() + ", totalCons = " + totalConnectionCount +
                    " [Accepted remote connection: ip: " + remotePeerIp + ", port " + remotePeerPort + "]");*/

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
        serverChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface("tun", listenPort));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        //System.out.println("Listening for incoming connection: " + serverChannel.toString());
    }

    private void initConnection(final PwpPeer peer) {
        SocketChannel peerChannel = null;

        //System.out.println("initConnection(): Increasing connection count (" + peer.getIp() + ":" + peer.getPort() + "), total = " + totalConnectionCount);

        try {
            peerChannel = SocketChannel.open();
            peerChannel.configureBlocking(false);

            setChannelOptions(peerChannel);

            final SelectionKey selectionKey = peerChannel.register(
                    selector, SelectionKey.OP_CONNECT, peer);
            final ClientSession session = new ClientSession(peerChannel, peer, false);
            selectionKey.attach(session);

            final PeerView peerView = session.getPeerView();

            indeterminateConnections.putIfAbsent(peer.getInfoHash(), new HashMap<>());
            indeterminateConnections.compute(peer.getInfoHash(), (key, connections) -> {
                connections.put(selectionKey, peerView);
                return connections;
            });

            peerChannel.bind(NetworkUtilities.getSocketAddressFromNetworkInterface("tun", 0));

            final boolean isConnected = peerChannel.connect(
                    new InetSocketAddress(peer.getIp(), peer.getPort()));

            if(isConnected) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
            ++totalConnectionCount;
            notifyPeerAdded(peerView);
        } catch(final IOException ioe) {
            //--totalConnectionCount;
            //System.out.println("initConnection(): Decreasing connection count (" + peer.getIp() + ":" + peer.getPort() + ") total = " + totalConnectionCount);

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