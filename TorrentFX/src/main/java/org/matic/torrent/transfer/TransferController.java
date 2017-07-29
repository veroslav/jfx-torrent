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
package org.matic.torrent.transfer;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.matic.torrent.client.ClientProperties;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataBlock;
import org.matic.torrent.io.DataPiece;
import org.matic.torrent.io.FileIOWorker;
import org.matic.torrent.io.FileOperationResult;
import org.matic.torrent.io.ReadDataPieceRequest;
import org.matic.torrent.io.TorrentFileIO;
import org.matic.torrent.io.WriteDataPieceRequest;
import org.matic.torrent.io.cache.CachedDataPieceIdentifier;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.net.pwp.InvalidPeerMessageException;
import org.matic.torrent.net.pwp.PeerConnectionController;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PeerSession;
import org.matic.torrent.net.pwp.PwpConnectionStateListener;
import org.matic.torrent.net.pwp.PwpMessage;
import org.matic.torrent.net.pwp.PwpMessageEvent;
import org.matic.torrent.net.pwp.PwpMessageFactory;
import org.matic.torrent.net.pwp.PwpMessageListener;
import org.matic.torrent.net.pwp.PwpMessageRequest;
import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.action.FilePriorityChangeEvent;
import org.matic.torrent.queue.action.FilePriorityChangeListener;
import org.matic.torrent.transfer.strategy.PieceSelectionStrategy;
import org.matic.torrent.transfer.strategy.RarestFirstPieceSelectionStrategy;
import org.matic.torrent.utils.UnitConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class manages download and upload of data for a torrentView.
 *
 * @author Vedran Matic
 */
public final class TransferController implements PwpMessageListener, PwpConnectionStateListener,
        FilePriorityChangeListener, Runnable {

    //14 interested and 1 optimistic client
    private static final int MAX_UNCHOKED_PEERS = 15;
    private static final int MAX_RAREST_PIECES = 10;

    private static final int MAX_BLOCK_REQUESTS_PER_PEER = 19;
    private static final int REQUESTED_BLOCK_LENGTH = 16384;    //16 kB

    //Various choking algorithm timeouts
    private static final long OPTIMISTIC_ROTATION_INTERVAL = 30000; //30 sec
    private static final long CHOKING_ROTATION_INTERVAL = 10000;    //10 sec
    private static final long ANTI_SNUBBING_INTERVAL = 60000;       //1 minute

    //Peers we have unchoked and who are interested (except optimistic unchoke)
    // and to whom we are uploading and downloading from
    private final List<PeerSession> downloaderPeers = new ArrayList<>();

    //Peers we are choking and who are interested and have unchoked us
    private final List<PeerSession> downloaderCandidatePeers = new ArrayList<>();

    //Peers we are choking and which have unchoked us and are NOT interested but letting us
    // download. Here we also find the peers that have previously snubbed us
    private final List<PeerSession> generousPeers = new ArrayList<>();

    //Peers we are choking and are choking us, no down-/upload is occurring
    private final List<PeerSession> standbyPeers = new LinkedList<>();

    //Piece download/upload state tracking
    private final Map<PeerSession, List<DataBlockIdentifier>> sentBlockRequests = new HashMap<>();
    private final Map<PeerSession, Set<DataBlockIdentifier>> uploadingBlocks = new HashMap<>();

    //Pieces for which the download was interrupted, i.e. peer disconnecting, snubbing etc
    private final Map<Integer, List<DataBlockIdentifier>> downloadedInterruptedPieces = new HashMap<>();

    //Event queues
    private FilePriorityChangeEvent queuedFilePriorityChangeEvent = null;
    private final List<PeerConnectionStateChangeEvent> peerStateChangeEventQueue = new LinkedList<>();
    private final List<FileOperationResult> fileOperationResultQueue = new LinkedList<>();
    private final List<PwpMessageEvent> messageQueue = new LinkedList<>();

    private final ObjectProperty<TransferStatusChangeEvent> statusProperty = new SimpleObjectProperty();

    private final ExecutorService ioWorkerExecutor = Executors.newSingleThreadExecutor();
    private final FileIOWorker fileIOWorker;
    private Future<?> fileIOWorkerJob;

    private final PieceSelectionStrategy pieceSelectionStrategy;
    private final BitSet receivedPieces;

    private final PeerConnectionController connectionManager;
    private final TorrentView torrentView;

    private final LongProperty totalDownloadedBytes = new SimpleLongProperty(0);
    private final IntegerProperty hashFailures = new SimpleIntegerProperty(0);
    private final LongProperty wastedBytes = new SimpleLongProperty(0);

    /**
     * Create a new instance.
     *
     * @param torrentView The view of the torrentView for which the data will be transferred
     * @param connectionManager For sending/receiving messages to/from remote peers
     * @param pieceCache The cache to use for data piece I/O
     */
    public TransferController(final TorrentView torrentView, final PeerConnectionController connectionManager,
                              final DataPieceCache pieceCache) {
        this.connectionManager = connectionManager;
        this.torrentView = torrentView;

        final List<QueuedFileMetaData> fileMetaDatas = this.torrentView.getMetaData().getFiles();
        final TreeMap<Long, TorrentFileIO> diskFileIOs = new TreeMap<>();

        for(final QueuedFileMetaData fileMetaData : fileMetaDatas) {
            diskFileIOs.put(fileMetaData.getOffset(), buildDiskFileIOs(fileMetaData));
        }

        final Consumer<FileOperationResult> pieceConsumer = this::fileOperationCompleted;
        this.fileIOWorker = new FileIOWorker(diskFileIOs, pieceCache, this.torrentView.getMetaData(), pieceConsumer);

        final int pieceCount = this.torrentView.getMetaData().getTotalPieces();
        receivedPieces = this.torrentView.getProgress().getObtainedPieces(pieceCount);

        pieceSelectionStrategy = new RarestFirstPieceSelectionStrategy(MAX_RAREST_PIECES,
                this.torrentView.getMetaData().getPieceLength(), receivedPieces);

        torrentView.setHavePieces(receivedPieces.toByteArray());
        torrentView.hashFailuresProperty().bindBidirectional(hashFailures);
        torrentView.downloadedBytesProperty().bind(totalDownloadedBytes);
        torrentView.wastedBytesProperty().bind(wastedBytes);
    }

    public void shutdown() {
        torrentView.downloadedBytesProperty().unbind();
        torrentView.hashFailuresProperty().unbind();
        torrentView.wastedBytesProperty().unbind();

        ioWorkerExecutor.shutdownNow();
    }

    public void fileOperationCompleted(final FileOperationResult fileOperationResult) {
        synchronized (this) {
            fileOperationResultQueue.add(fileOperationResult);
            this.notifyAll();
        }
    }

    /**
     * Notify listeners when a transfer change has occurred. An example of this can be a transfer completion,
     * an error during file I/O and similar.
     *
     * @param statusChangeHandler Handler of the status change
     */
    public void addStatusChangeListener(final Consumer<TransferStatusChangeEvent> statusChangeHandler) {
        statusProperty.addListener((obs, oldV, newV) -> statusChangeHandler.accept(newV));
    }

    /**
     * @see {@link FilePriorityChangeListener#filePriorityChanged(FilePriorityChangeEvent)}
     */
    @Override
    public void filePriorityChanged(final FilePriorityChangeEvent changeEvent) {
        synchronized (this) {
            queuedFilePriorityChangeEvent = changeEvent;
            this.notifyAll();
        }
    }

    /**
     * @see {@link PwpMessageListener#onMessageReceived(PwpMessageEvent)}
     */
    @Override
    public void onMessageReceived(final PwpMessageEvent event) {
        synchronized(this) {
            messageQueue.add(event);
            this.notifyAll();
        }
    }

    /**
     * @see {@link PwpMessageListener#getPeerMessageAcceptanceFilter()}
     */
    @Override
    public Predicate<PwpMessageEvent> getPeerMessageAcceptanceFilter() {
        return messageEvent -> {
            final InfoHash senderInfoHash = messageEvent.getPeerSession().getInfoHash();
            final PwpMessage.MessageType messageType = messageEvent.getMessage().getMessageType();
            return torrentView.getInfoHash().equals(senderInfoHash) &&
                    (messageType == PwpMessage.MessageType.HAVE
                            || messageType == PwpMessage.MessageType.CHOKE
                            || messageType == PwpMessage.MessageType.UNCHOKE
                            || messageType == PwpMessage.MessageType.INTERESTED
                            || messageType == PwpMessage.MessageType.NOT_INTERESTED
                            || messageType == PwpMessage.MessageType.PIECE
                            || messageType == PwpMessage.MessageType.REQUEST
                            || messageType == PwpMessage.MessageType.CANCEL
                            || messageType == PwpMessage.MessageType.HANDSHAKE
                            || messageType == PwpMessage.MessageType.BITFIELD);
        };
    }

    /**
     * @see {@link PwpConnectionStateListener#peerConnectionStateChanged(PeerConnectionStateChangeEvent)}
     */
    @Override
    public void peerConnectionStateChanged(final PeerConnectionStateChangeEvent event) {
        synchronized(this) {
            peerStateChangeEventQueue.add(event);
            this.notifyAll();
        }
    }

    /**
     * @see {@link PwpConnectionStateListener#getPeerStateChangeAcceptanceFilter()}
     */
    @Override
    public Predicate<PeerConnectionStateChangeEvent> getPeerStateChangeAcceptanceFilter() {
        return event -> {
            final PeerConnectionStateChangeEvent.PeerLifeCycleChangeType eventType = event.getEventType();
            return torrentView.getInfoHash().equals(event.getPeerSession().getInfoHash()) &&
                    eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED;
        };
    }

    @Override
    public void run() {
        restoreState();

        long lastChokingRotationTime = 0;
        long lastOptimisticUnchokeTime = System.currentTimeMillis();
        long lastAntiSnubbingCheckTime = System.currentTimeMillis();
        long timeLeftUntilChokingRotation;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                storeState();
                return;
            }

            FilePriorityChangeEvent filePriorityChangeEvent = null;
            PeerConnectionStateChangeEvent peerEvent = null;
            FileOperationResult fileOperationResult = null;
            PwpMessageEvent messageEvent = null;

            synchronized(this) {
                while((timeLeftUntilChokingRotation = getTimeLeftUntilChokingRotation(lastChokingRotationTime)) > 0
                        && eventQueuesEmpty()) {
                    try {
                        this.wait(timeLeftUntilChokingRotation);
                    } catch (final InterruptedException ie) {
                        Thread.interrupted();
                        storeState();
                        return;
                    }
                }

                if(!peerStateChangeEventQueue.isEmpty()) {
                    peerEvent = peerStateChangeEventQueue.remove(0);
                }
                if(!messageQueue.isEmpty()) {
                    messageEvent = messageQueue.remove(0);
                }
                if(!fileOperationResultQueue.isEmpty()) {
                    fileOperationResult = fileOperationResultQueue.remove(0);
                }
                if(queuedFilePriorityChangeEvent != null) {
                    filePriorityChangeEvent = queuedFilePriorityChangeEvent;
                    queuedFilePriorityChangeEvent = null;
                }
            }

            if(peerEvent != null) {
                handlePeerStateChange(peerEvent);
            }
            if(messageEvent != null) {
                handlePeerMessage(messageEvent);
            }
            if(fileOperationResult != null) {
                handleFileOperationCompleted(fileOperationResult);
            }
            if(filePriorityChangeEvent != null) {
                handleFilePriorityChangeEvent(filePriorityChangeEvent);
            }

            if(timeLeftUntilChokingRotation <= 0) {
                final boolean applyAntiSnubbing = getTimeLeftUntilAntiSnubbingCheck(lastAntiSnubbingCheckTime) <= 0;
                final boolean applyOptimisticUnchoking =
                        getTimeLeftUntilOptimisticUnchoking(lastOptimisticUnchokeTime) <= 0;
                final long currentTime = System.currentTimeMillis();

                applyChokingRotation(applyAntiSnubbing, applyOptimisticUnchoking);
                lastChokingRotationTime = currentTime;

                if(applyAntiSnubbing) {
                    lastAntiSnubbingCheckTime = currentTime;
                }

                if(applyOptimisticUnchoking) {
                    lastOptimisticUnchokeTime = currentTime;
                }
            }
        }
    }

    private boolean eventQueuesEmpty() {
        return peerStateChangeEventQueue.isEmpty() && messageQueue.isEmpty()
                && fileOperationResultQueue.isEmpty() && queuedFilePriorityChangeEvent == null;
    }

    private long getTimeLeftUntilAntiSnubbingCheck(final long lastAntiSnubbingCheckTime) {
        return ANTI_SNUBBING_INTERVAL - (System.currentTimeMillis() - lastAntiSnubbingCheckTime);
    }

    private long getTimeLeftUntilOptimisticUnchoking(final long lastOptimisticUnchokingTime) {
        return OPTIMISTIC_ROTATION_INTERVAL - (System.currentTimeMillis() - lastOptimisticUnchokingTime);
    }

    private long getTimeLeftUntilChokingRotation(final long lastChokingRotationTime) {
        return CHOKING_ROTATION_INTERVAL - (System.currentTimeMillis() - lastChokingRotationTime);
    }

    private void handleFileOperationCompleted(final FileOperationResult fileOperationResult) {
        //Check whether the file operation was successful
        final Optional<IOException> fileOperationError = fileOperationResult.getErrorCause();

        if(fileOperationError.isPresent()) {

            //TODO: Handle the I/O exception (re-download the piece if OperationType == WRITE)

            totalDownloadedBytes.setValue(totalDownloadedBytes.get() - fileOperationResult.getDataPiece().getLength());

            if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.READ) {
                final PeerSession sender = fileOperationResult.getSender();
                uploadingBlocks.computeIfAbsent(sender, peer -> new HashSet<>()).remove(fileOperationResult.getBlockRequest());
                sender.setSentBlockRequests(uploadingBlocks.get(sender).size());
            }
            pieceSelectionStrategy.pieceFailure(fileOperationResult.getDataPiece().getIndex());
        }
        else if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.READ) {
            handlePieceRead(fileOperationResult);
        }
        else if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.WRITE) {
            handlePieceWritten(fileOperationResult);
        }
    }

    private void applyChokingRotation(final boolean applyAntiSnubbing, final boolean applyOptimisticUnchoking) {

        //System.out.println("\nCHOKING_ROTATION\n\tDownloaders: " + downloaderPeers);

        Optional<PeerSession> peerToUnchoke = applyOptimisticUnchoking?
                applyOptimisticUnchoking() : getRandomChokedPeerForUnchoking();
        if(!peerToUnchoke.isPresent()) {
            return;
        }

        //Check for any peers that have snubbed us and choke them
        if(applyAntiSnubbing) {
            applyAntiSnubbing();
        }

        //If we haven't yet reached the limit for max unchoked peers, just unchoke as many as possible
        if(downloaderPeers.size() < MAX_UNCHOKED_PEERS) {
            do {
                unchokePeer(peerToUnchoke.get());
                peerToUnchoke = getRandomChokedPeerForUnchoking();
            } while(peerToUnchoke.isPresent() && downloaderPeers.size() < MAX_UNCHOKED_PEERS);
        }
        //We already have max peers unchoked, replace the one with the slowest upload rate
        else {
            final Optional<PeerSession> slowestUploadingPeer = downloaderPeers.stream().reduce((first, second) ->
                    first.getAverageUploadRateSinceLastUnchoke() <
                            second.getAverageUploadRateSinceLastUnchoke()? first : second);
            if(slowestUploadingPeer.isPresent()) {
                final PeerSession slowestPeer = slowestUploadingPeer.get();

                //START TEST
                /*if(System.currentTimeMillis() - slowestPeer.getUnchokedByUsTime() > ANTI_SNUBBING_INTERVAL
                    && chokePeer(slowestPeer)) {*/

                    /*System.out.println("[CHOKER]: choked slowest client: " + slowestPeer + " ["
                            + slowestPeer.getAverageUploadRateSinceLastUnchoke() + " b/s]");*/

                    saveInterruptedDownloadState(slowestPeer);

                    connectionManager.send(new PwpMessageRequest(
                            PwpMessageFactory.getChokeMessage(), slowestPeer));
                    unchokePeer(peerToUnchoke.get());
                //}
                //END TEST
            }
        }
    }

    private void applyAntiSnubbing() {

        //System.out.println("ANTI_SNUBBING_CHECK");

        final long currentTime = System.currentTimeMillis();

        final List<PeerSession> snubbingPeers = downloaderPeers.stream().filter(peerSession -> {
            final List<DataBlockIdentifier> peerBlockRequests = sentBlockRequests.get(peerSession);
            if(peerBlockRequests == null || peerBlockRequests.isEmpty()) {
                return false;
            }

            final Optional<Long> lastSentBlockTime = peerBlockRequests.stream().map(
                    DataBlockIdentifier::getTimeRequested).min(Long::compareTo);

            return lastSentBlockTime.isPresent() &&
                    ((currentTime - lastSentBlockTime.get()) > ANTI_SNUBBING_INTERVAL);
        }).collect(Collectors.toList());

        if(snubbingPeers.isEmpty()) {
            return;
        }

        //System.out.println("Choking " + snubbingPeers + " because they have snubbed us");

        snubbingPeers.forEach(peer -> {
            chokePeer(peer);
            peer.setSnubbed(true);
            saveInterruptedDownloadState(peer);
        });
        connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), snubbingPeers));
    }

    private Optional<PeerSession> applyOptimisticUnchoking() {

        //System.out.println("OPTIMISTIC_UNCHOKING");

        final int randomUnchokingDistribution = ClientProperties.RANDOM_INSTANCE.nextInt(4);

        //3x more likely to unchoke a newly connected peer
        final int pieceCount = torrentView.getMetaData().getTotalPieces();
        final Optional<PeerSession> standByPeer;

        if(randomUnchokingDistribution < 3 && (standByPeer = standbyPeers.stream().filter(
                peer -> !peer.isSeeder(pieceCount)).findFirst()).isPresent()) {

            //TODO: Prefer stand-by peers who are INTERESTED in us

            final PeerSession foundPeer = standByPeer.get();
            standbyPeers.remove(foundPeer);
            return standByPeer;
        }
        else {
            //Unchoke a random peer
            return getRandomChokedPeerForUnchoking();
        }
    }

    private Optional<PeerSession> getRandomChokedPeerForUnchoking() {
        final List<PeerSession> unchokeCandidates = new ArrayList<>();
        List<PeerSession> chokedPeerQueue;

        //We prioritize peers in this order: interested, generous and then stand-by peers
        final int pieceCount = torrentView.getMetaData().getTotalPieces();

        if(!downloaderCandidatePeers.isEmpty() &&
            unchokeCandidates.addAll(downloaderCandidatePeers.stream().filter(
                    peer -> !peer.isSeeder(pieceCount)).collect(Collectors.toList()))) {
            chokedPeerQueue = downloaderCandidatePeers;
        } else if(!generousPeers.isEmpty() &&
            unchokeCandidates.addAll(generousPeers.stream().filter(
                    peer -> !peer.isSeeder(pieceCount)).collect(Collectors.toList()))) {
            chokedPeerQueue = generousPeers;
        } else if(!standbyPeers.isEmpty() &&
            unchokeCandidates.addAll(standbyPeers.stream().filter(
                    peer -> !peer.isSeeder(pieceCount)).collect(Collectors.toList()))) {
            chokedPeerQueue = standbyPeers;
        } else {
            //There are no peers to choose from, simply return
            return Optional.empty();
        }

        final Optional<PeerSession> interestedPeer = unchokeCandidates.stream().filter(
                peer -> peer.isInterestedInUs()).findFirst();

        final PeerSession foundChokedPeer = interestedPeer.isPresent()? interestedPeer.get()
                : unchokeCandidates.get(ClientProperties.RANDOM_INSTANCE.nextInt(unchokeCandidates.size()));

        //Remove the peer from its queue
        chokedPeerQueue.remove(foundChokedPeer);

        return Optional.of(foundChokedPeer);
    }

    private boolean chokePeer(final PeerSession peer) {
        if(!downloaderPeers.remove(peer)) {
            return false;
        }

        peer.setAreWeChoking(true);

        if(peer.isChokingUs()) {
            standbyPeers.add(0, peer);
        }
        else {
            if(peer.isInterestedInUs()) {
                downloaderCandidatePeers.add(peer);
            }
            else {
                generousPeers.add(peer);
            }
        }
        return true;
    }

    private void unchokePeer(final PeerSession unchokedPeer) {
        unchokedPeer.setAreWeChoking(false);
        unchokedPeer.setSnubbed(false);
        downloaderPeers.add(unchokedPeer);
        connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getUnchokeMessage(), unchokedPeer));
    }

    private Set<Integer> saveInterruptedDownloadState(final PeerSession targetPeer) {
        final Set<Integer> inProgressPieceDownloads = new HashSet<>();
        final List<DataBlockIdentifier> peerBlockRequests = sentBlockRequests.remove(targetPeer);
        if(peerBlockRequests != null && !peerBlockRequests.isEmpty()) {
            final Map<Integer, List<DataBlockIdentifier>> requestedPieces = peerBlockRequests.stream()
                    .collect(Collectors.groupingBy(DataBlockIdentifier::getPieceIndex));

            inProgressPieceDownloads.addAll(requestedPieces.keySet());
            downloadedInterruptedPieces.putAll(requestedPieces);

            targetPeer.setSentBlockRequests(peerBlockRequests.size());
        }
        return inProgressPieceDownloads;
    }

    private void handlePieceWritten(final FileOperationResult fileOperationResult) {
        final int pieceIndex = fileOperationResult.getDataPiece().getIndex();

        //Send HAVE message only to non-seeder peers
        final List<PeerSession> receiverPeers = new ArrayList<>();
        final int totalPieceCount = torrentView.getTotalPieces();

        receiverPeers.addAll(downloaderCandidatePeers);
        receiverPeers.addAll(downloaderPeers.stream().filter(peer ->
                peer.isSeeder(totalPieceCount)).collect(Collectors.toList()));
        receiverPeers.addAll(generousPeers.stream().filter(peer ->
                peer.isSeeder(totalPieceCount)).collect(Collectors.toList()));
        receiverPeers.addAll(standbyPeers.stream().filter(peer ->
                peer.isSeeder(totalPieceCount)).collect(Collectors.toList()));

        if(!receiverPeers.isEmpty()) {
            connectionManager.send(new PwpMessageRequest(
                    PwpMessageFactory.buildHavePieceMessage(pieceIndex), receiverPeers));
        }
    }

    private void handlePieceRead(final FileOperationResult fileOperationResult) {
        final DataPiece dataPiece = fileOperationResult.getDataPiece();
        final DataBlockIdentifier blockRequest = fileOperationResult.getBlockRequest();

        //TODO: Verify the DataPiece (SHA-1) before sending its block to the peer?

        final DataBlock dataBlock = dataPiece.getBlock(
                blockRequest.getPieceOffset(), blockRequest.getBlockLength()).get();

        final PeerSession sender = fileOperationResult.getSender();
        connectionManager.send(new PwpMessageRequest(
                PwpMessageFactory.buildSendBlockMessage(dataBlock), sender));

        //System.out.println("Sent block " + blockRequest + " to " + fileOperationResult.getSender());
        uploadingBlocks.computeIfAbsent(sender, peer -> new HashSet<>()).remove(blockRequest);
        sender.setSentBlockRequests(uploadingBlocks.get(sender).size());
    }

    private void handleFilePriorityChangeEvent(final FilePriorityChangeEvent filePriorityChangeEvent) {
        //TODO: Implement method
    }

    private void handlePeerStateChange(final PeerConnectionStateChangeEvent changeEvent) {
        final PeerSession peer = changeEvent.getPeerSession();

        boolean wasRemoved;

        if(!peer.areWeChoking()) {
            wasRemoved = downloaderPeers.remove(peer);

            //Unchoke a peer to replace the disconnected peer
            if(wasRemoved) {
                if(!downloaderCandidatePeers.isEmpty()) {
                    unchokePeer(downloaderCandidatePeers.remove(0));
                }
                else {
                    final Optional<PeerSession> peerToUnchoke = getRandomChokedPeerForUnchoking();
                    if (peerToUnchoke.isPresent()) {
                        unchokePeer(peerToUnchoke.get());
                    }
                }
            }
        }
        else if(peer.isInterestedInUs()) {
            wasRemoved = generousPeers.remove(peer);
        }
        else {
            wasRemoved = standbyPeers.remove(peer);
        }

        if(wasRemoved) {
            //Update piece statistics, remove all piece counts for pieces
            // that this peer had and store its download state
            final Set<Integer> interruptedPieceDownloadsFromPeer = saveInterruptedDownloadState(peer);
            interruptedPieceDownloadsFromPeer.forEach(index -> pieceSelectionStrategy.pieceFailure(index));
            pieceSelectionStrategy.peerLost(peer.getPieces());
        }
    }

    private void handlePeerMessage(final PwpMessageEvent messageEvent) {
        final PwpMessage message = messageEvent.getMessage();
        final PeerSession peerSession = messageEvent.getPeerSession();

        switch(message.getMessageType()) {
            case HAVE:
                handleHaveMessage(message, peerSession);
                break;
            case HANDSHAKE:
                handleHandshakeMessage(peerSession);
                break;
            case BITFIELD:
                handleBitfieldMessage(message, peerSession);
                break;
            case CHOKE:
                handleChokeMessage(peerSession);
                break;
            case UNCHOKE:
                handleUnchokeMessage(peerSession);
                break;
            case PIECE:
                handleBlockReceived(message, peerSession);
                break;
            case REQUEST:
                handleBlockRequested(message, peerSession);
                break;
            case CANCEL:
                handleCancelMessage(message, peerSession);
                break;
            case INTERESTED:
                handleInterestedMessage(peerSession);
                break;
            case NOT_INTERESTED:
                handleNotInterestedMessage(peerSession);
                break;
        }
    }

    private void handleBitfieldMessage(final PwpMessage message, final PeerSession peerSession) {
        if(standbyPeers.contains(peerSession)) {
            //Check whether this peer has anything we are interested in

            final BitSet parsedBitField = PwpMessageFactory.parseBitfieldMessage(message);
            peerSession.setPieces(parsedBitField);

            pieceSelectionStrategy.peerGained(parsedBitField);

            checkIfInterestingAndShowInterest(peerSession);

            /*if(interestedSent) {
                System.out.println("[BITFIELD] Sent interest to " + peerView + "? " + interestedSent);
            }*/
        }
    }

    private void handleHandshakeMessage(final PeerSession peerSession) {
        if(!standbyPeers.contains(peerSession)) {
            standbyPeers.add(peerSession);

            //Send a BITFIELD message to this peer if we have any pieces to share
            if(receivedPieces.cardinality() > 0) {
                connectionManager.send(new PwpMessageRequest(PwpMessageFactory.buildBitfieldMessage(
                        receivedPieces, torrentView.getMetaData().getTotalPieces()), peerSession));
            }
        }
    }

    private void handleInterestedMessage(final PeerSession peerSession) {
        if(peerSession.isInterestedInUs()) {
            return;
        }

        peerSession.setInterestedInUs(true);

        if(peerSession.areWeChoking()) {
            if(downloaderPeers.size() < MAX_UNCHOKED_PEERS) {
                unchokePeer(peerSession);
            }
            else {
                generousPeers.remove(peerSession);
                standbyPeers.remove(peerSession);
                downloaderCandidatePeers.add(peerSession);
            }
        }
    }

    private void handleNotInterestedMessage(final PeerSession peerSession) {
        if(!peerSession.isInterestedInUs()) {
            return;
        }

        peerSession.setInterestedInUs(false);

        if(peerSession.areWeChoking() && !peerSession.isChokingUs()) {
            downloaderCandidatePeers.remove(peerSession);
            generousPeers.add(peerSession);
        }
    }

    private void handleCancelMessage(final PwpMessage message, final PeerSession peerSession) {
        //TODO: Implement method
    }

    private void handleUnchokeMessage(final PeerSession peerSession) {
        if(!peerSession.isChokingUs()) {

            //System.out.println("[UNCHOKE] Ignored unchoke from " + peerSession + " because we are already unchoked");

            return;
        }

        peerSession.setChokingUs(false);

        //Check whether we are unchoked by a previous optimistic unchoke
        if(peerSession.areWeChoking()) {
            standbyPeers.remove(peerSession);

            if(peerSession.isInterestedInUs()){
                if(downloaderPeers.size() < MAX_UNCHOKED_PEERS) {
                    unchokePeer(peerSession);
                }
                else {
                    downloaderCandidatePeers.add(peerSession);
                }
            }
            else {
                generousPeers.add(peerSession);
            }
        }

        if(peerSession.areWeInterestedIn()) {
            //Request one of the rarest pieces from this client
            requestPiece(peerSession);
        }
    }

    private void handleChokeMessage(final PeerSession peerSession) {
        if(peerSession.isChokingUs()) {
            return;
        }

        peerSession.setChokingUs(true);

        if(peerSession.areWeChoking()) {
            if(peerSession.isInterestedInUs()) {
                downloaderCandidatePeers.remove(peerSession);
            }
            else {
                generousPeers.remove(peerSession);
            }
        } else {
            downloaderPeers.remove(peerSession);
            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), peerSession));
            peerSession.setAreWeChoking(true);

            //Replace the choked peer with another downloader candidate
            if(!downloaderCandidatePeers.isEmpty()) {
                unchokePeer(downloaderCandidatePeers.remove(0));
            } else {
                final Optional<PeerSession> peerToUnchoke = getRandomChokedPeerForUnchoking();
                if(peerToUnchoke.isPresent()) {
                    unchokePeer(peerToUnchoke.get());
                }
            }
        }

        //System.out.println("Saving interrupted state for peer that choked us: " + peerSession);
        saveInterruptedDownloadState(peerSession);
        standbyPeers.add(0, peerSession);
    }

    private boolean checkIfInterestingAndShowInterest(final PeerSession peerSession) {
        final BitSet peerPieces = peerSession.getPieces().get(0, torrentView.getMetaData().getTotalPieces());
        peerPieces.andNot(receivedPieces);
        final boolean peerHasNotReceivedPieces = !peerPieces.isEmpty();

        if(!peerHasNotReceivedPieces) {
            //We already have all of this client's pieces

            //System.out.println("Already have all of the pieces that " + peerSession + " has.");

            return false;
        }

        //The client has pieces that we haven't yet got. Check whether we have pending requests for those
        final boolean peerHasPiecesNotYetRequested = pieceSelectionStrategy.anyPiecesNotYetRequested(peerPieces);

        if(peerHasPiecesNotYetRequested) {
            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getInterestedMessage(), peerSession));
            peerSession.setAreWeInterestedIn(true);

            return true;
        }

        //System.out.println("All of the pieces that " + peerSession + " has are already requested.");

        return false;
    }

    private void handleBlockReceived(final PwpMessage message, final PeerSession sender) {

        final DataBlock block;
        try {
            block = PwpMessageFactory.parseBlockReceivedMessage(message);
        }
        catch(final InvalidPeerMessageException ipme) {
            System.err.println(ipme.getMessage());
            return;
        }

        final int blockLength = block.getBlockData().length;

        sender.updateBytesReceived(blockLength);
        //sender.setDownSpeed(sender.getAverageUploadRateSinceLastUnchoke());

        //Check whether we have requested this block from this peer
        final List<DataBlockIdentifier> blockRequests = sentBlockRequests.computeIfAbsent(
                sender, requestList -> new ArrayList());
        final Optional<DataBlockIdentifier> matchingRequest = blockRequests.stream().filter(request ->
                request.getBlockLength() == blockLength &&
                        request.getPieceIndex() == block.getPieceIndex()
                        && request.getPieceOffset() == block.getPieceOffset()
        ).findAny();

        //TODO: Enable when the storage of the state of downloading blocks has been implemented
        /*if(!matchingRequest.isPresent()) {
            //We haven't requested this block
            System.out.println("WASTED: Didn't request this block: " + block + " from " + sender
                + ", requested blocks are: " + blockRequests);
            wastedBytes.set(wastedBytes.get() + blockLength);
            return;
        }*/

        if(matchingRequest.isPresent()) {
            blockRequests.remove(matchingRequest.get());
            sender.setRequestedBlocks(blockRequests.size());
        }

        final int pieceIndex = block.getPieceIndex();
        final DataPiece dataPiece = pieceSelectionStrategy.getRequestedPiece(pieceIndex);
        if(dataPiece == null) {
            return;
        }

        final boolean blockAdded = dataPiece.addBlock(block);
        if(!blockAdded) {
            System.out.println("WASTED: Received blocked was out of order.");
            wastedBytes.set(wastedBytes.get() + blockLength);
            return;
        }

        totalDownloadedBytes.set(totalDownloadedBytes.get() + blockLength);

        if(dataPiece.hasCompleted()) {
            final boolean validPiece = dataPiece.validate(torrentView.getMetaData().getPieceHash(pieceIndex));

            System.out.println("\nPIECE COMPLETED from " + sender + " : " + dataPiece.getIndex());

            if(validPiece) {
                pieceSelectionStrategy.pieceObtained(pieceIndex);
                torrentView.setHavePiece(pieceIndex);

                //Request a new piece from this peer if we are unchoked
                if(!sender.isChokingUs()) {
                    requestPiece(sender);
                }

                final CachedDataPieceIdentifier cachedDataPieceIdentifier =
                        new CachedDataPieceIdentifier(dataPiece.getIndex(), sender.getInfoHash());
                fileIOWorker.writeDataPiece(new WriteDataPieceRequest(cachedDataPieceIdentifier, dataPiece, sender));
            }
            else {
                hashFailures.set(hashFailures.get() + 1);
                totalDownloadedBytes.set(totalDownloadedBytes.get() - dataPiece.getLength());

                pieceSelectionStrategy.pieceFailure(pieceIndex);
            }
        }
        else {
            requestBlocks(dataPiece, sender);

            //TODO: Must check whether it is time to request next piece blocks!
            /*if(allRequestedBlocksForPeer.size() < MAX_BLOCK_REQUESTS_PER_PEER) {
                //System.out.println("Need to request new piece from " + receiver);

                requestPiece(receiver);
            }*/
        }
    }

    private void handleBlockRequested(final PwpMessage message, final PeerSession requester) {
        //Only send the block if the requester is unchoked
        if(requester.areWeChoking()) {
            return;
        }

        final DataBlockIdentifier blockRequest;
        try {
            blockRequest = PwpMessageFactory.parseBlockRequestedMessage(message);
        } catch (final InvalidPeerMessageException ipme) {
            System.err.println(ipme.getMessage());
            return;
        }

        /*System.out.println("\nREQUEST from " + requester + ": " + blockRequest
                + ", do we have the piece? " + receivedPieces.get(blockRequest.getPieceIndex()) + "\n");*/

        //Don't allow requests for blocks that are longer than 16 kB
        final int blockLength = blockRequest.getBlockLength();
        if(blockLength > REQUESTED_BLOCK_LENGTH) {
            System.out.println("Block request denied, too big: " + blockLength);
            return;
        }

        //Read data only if we have this piece/block
        final int pieceIndex = blockRequest.getPieceIndex();
        if(receivedPieces.get(pieceIndex)) {
            final ReadDataPieceRequest readRequest = new ReadDataPieceRequest(new CachedDataPieceIdentifier(
                    pieceIndex, requester.getInfoHash()), blockRequest, requester);

            fileIOWorker.readDataPiece(readRequest);
        }
    }

    private void requestPiece(final PeerSession peerSession) {
        //First check whether there are any interrupted piece downloads that this peer has
        final Optional<Integer> interruptedPiece = downloadedInterruptedPieces.keySet().stream().filter(
                pieceIndex -> peerSession.hasPiece(pieceIndex)).findFirst();

        final Optional<Integer> nextPieceCandidate = interruptedPiece.isPresent()? interruptedPiece
                : pieceSelectionStrategy.selectNext(peerSession.getPieces());

        if(nextPieceCandidate.isPresent()) {
            final int pieceIndex = nextPieceCandidate.get();

            //Check whether we are going to request blocks for an interrupted piece download
            final List<DataBlockIdentifier> interruptedBlockRequests = downloadedInterruptedPieces.remove(pieceIndex);
            if(interruptedBlockRequests != null && !interruptedBlockRequests.isEmpty()) {
                sentBlockRequests.computeIfAbsent(peerSession, blocks -> new ArrayList<>()).addAll(interruptedBlockRequests);
                peerSession.setRequestedBlocks(sentBlockRequests.get(peerSession).size());
            }

            final DataPiece requestedDataPiece = new DataPiece(
                    torrentView.getMetaData().getPieceLength(), pieceIndex);

            if(pieceSelectionStrategy.pieceRequested(pieceIndex, requestedDataPiece)) {
                requestBlocks(requestedDataPiece, peerSession);
            }
        }
    }

    private void requestBlocks(final DataPiece dataPiece, final PeerSession receiver) {
        //Collect all previously sent block requests for this piece

        final List<DataBlockIdentifier> allRequestedBlocksForPeer = sentBlockRequests.computeIfAbsent(
                receiver, blocks -> new ArrayList<>());

        final List<DataBlockIdentifier> blockRequestsForPiece = allRequestedBlocksForPeer.stream().filter(
                block -> block.getPieceIndex() == dataPiece.getIndex()).collect(Collectors.toList());

        int pieceOffset = 0;
        if(!blockRequestsForPiece.isEmpty()) {
            final DataBlockIdentifier lastBlockRequested = blockRequestsForPiece.get(blockRequestsForPiece.size() - 1);
            pieceOffset = lastBlockRequested.getPieceOffset() + lastBlockRequested.getBlockLength();
        }

        final List<PwpMessage> blockRequestMessages = new ArrayList<>();

        while(allRequestedBlocksForPeer.size() < MAX_BLOCK_REQUESTS_PER_PEER && pieceOffset < dataPiece.getLength()) {
            //Check if there are unrequested blocks left for this piece
            final boolean isLastBlock = pieceOffset + REQUESTED_BLOCK_LENGTH >= dataPiece.getLength();
            final int blockLength = isLastBlock? dataPiece.getLength() - pieceOffset : REQUESTED_BLOCK_LENGTH;
            final DataBlockIdentifier blockRequest = new DataBlockIdentifier(dataPiece.getIndex(),
                    pieceOffset, blockLength);

            final PwpMessage message = PwpMessageFactory.buildRequestMessage(blockRequest);
            blockRequestMessages.add(message);
            allRequestedBlocksForPeer.add(blockRequest);
            pieceOffset += blockLength;

            receiver.setRequestedBlocks(allRequestedBlocksForPeer.size());
        }

        if(!blockRequestMessages.isEmpty()) {
            connectionManager.send(new PwpMessageRequest(blockRequestMessages, receiver, PwpMessage.MessageType.REQUEST));
        }
    }

    private void handleHaveMessage(final PwpMessage message, final PeerSession peerSession) {
        final int pieceIndex = UnitConverter.getInt(message.getPayload());

        if(pieceIndex < 0 || pieceIndex >= torrentView.getTotalPieces()) {
            System.err.println("[HAVE] Invalid piece index: " + pieceIndex + " from " + peerSession);
            return;
        }

        pieceSelectionStrategy.occurrenceIncreased(pieceIndex);
        peerSession.setHasPiece(pieceIndex, true);

        //If we don't yet have this piece, let's show some interest
        if(!receivedPieces.get(pieceIndex) && pieceSelectionStrategy.getRequestedPiece(pieceIndex) == null
                && !peerSession.areWeInterestedIn()) {

            //System.out.println("[HAVE] Showing interest in " + peerView + " because of piece " + pieceIndex);

            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getInterestedMessage(), peerSession));
            peerSession.setAreWeInterestedIn(true);
        }
    }

    private TorrentFileIO buildDiskFileIOs(final QueuedFileMetaData fileMetaData) {
        final Path filePath = torrentView.getProgress().getSavePath()
                .resolve(Paths.get(torrentView.getMetaData().getName())).resolve(fileMetaData.getPath());

        try {
            if(!filePath.toFile().exists()) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
            }

            final TorrentFileIO fileIO = new TorrentFileIO(filePath, fileMetaData);
            return fileIO;
        } catch (final IOException ioe) {
            statusProperty.setValue(new TransferStatusChangeEvent(TransferStatusChangeEvent.EventType.ERROR,
                    ioe.getMessage()));
            return null;
        }
    }

    private void storeState() {
        connectionManager.removeConnectionListener(this);
        connectionManager.removeMessageListener(this);

        if(fileIOWorkerJob != null) {
            fileIOWorkerJob.cancel(true);
        }

        torrentView.getProgress().storeObtainedPieces(receivedPieces);
    }

    private void restoreState() {
        fileIOWorkerJob = ioWorkerExecutor.submit(fileIOWorker);

        connectionManager.addConnectionListener(this);
        connectionManager.addMessageListener(this);
    }
}