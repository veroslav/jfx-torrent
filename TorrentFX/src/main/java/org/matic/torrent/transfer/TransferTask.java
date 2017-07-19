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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataBlock;
import org.matic.torrent.io.DataPiece;
import org.matic.torrent.io.FileIOWorker;
import org.matic.torrent.io.FileOperationResult;
import org.matic.torrent.io.DataPieceIdentifier;
import org.matic.torrent.io.TorrentFileIO;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.net.pwp.InvalidPeerMessageException;
import org.matic.torrent.net.pwp.PeerConnectionController;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PwpConnectionStateListener;
import org.matic.torrent.net.pwp.PwpMessage;
import org.matic.torrent.net.pwp.PwpMessageEvent;
import org.matic.torrent.net.pwp.PwpMessageFactory;
import org.matic.torrent.net.pwp.PwpMessageListener;
import org.matic.torrent.net.pwp.PwpMessageRequest;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.action.FilePriorityChangeEvent;
import org.matic.torrent.queue.action.FilePriorityChangeListener;
import org.matic.torrent.utils.UnitConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class manages download and upload of data for a torrent.
 *
 * @author Vedran Matic
 */
public final class TransferTask implements PwpMessageListener, PwpConnectionStateListener,
        FilePriorityChangeListener, Runnable {

    //4 interested and 1 optimistic peer
    private static final int MAX_UNCHOKED_PEERS = 5;
    private static final int MAX_RAREST_PIECES = 10;

    private static final int MAX_BLOCK_REQUESTS_FOR_PIECE = 10;
    private static final int REQUESTED_BLOCK_LENGTH = 16384;    //16 kB

    //Various choking algorithm timeouts
    private static final long OPTIMISTIC_ROTATION_INTERVAL = 30000; //30 sec
    private static final long CHOKING_ROTATION_INTERVAL = 10000;    //10 sec
    private static final long ANTI_SNUBBING_INTERVAL = 60000;       //1 minute

    //Peers we have unchoked and who are interested (except optimistic unchoke)
    // and to whom we are uploading and downloading from
    private final List<PeerView> downloaderPeers = new ArrayList<>();

    //Peers we are choking and who are interested and have unchoked us
    private final List<PeerView> downloaderCandidatePeers = new ArrayList<>();

    //Peers we are choking and which have unchoked us and are NOT interested but letting us
    // download. Here we also find the peers that have previously snubbed us
    private final List<PeerView> generousPeers = new ArrayList<>();

    //Peers we are choking and are choking us, no down-/upload is occurring
    private final List<PeerView> standbyPeers = new LinkedList<>();

    //Piece download/upload state tracking
    private final Map<PeerView, List<DataBlockIdentifier>> sentBlockRequests = new HashMap<>();
    private final Map<Integer, DataPiece> downloadingPieces = new HashMap<>();
    private final Map<Integer, DataBlock> uploadingBlocks = new HashMap<>();

    //Event queues
    private FilePriorityChangeEvent queuedFilePriorityChangeEvent = null;
    private final List<PeerConnectionStateChangeEvent> peerStateChangeEventQueue = new LinkedList<>();
    private final List<FileOperationResult> fileOperationResultQueue = new LinkedList<>();
    private final List<PwpMessageEvent> messageQueue = new LinkedList<>();

    private final ObjectProperty<TransferStatusChangeEvent> statusProperty = new SimpleObjectProperty();

    private final ExecutorService ioWorkerExecutor = Executors.newSingleThreadExecutor();
    private final FileIOWorker fileIOWorker;
    private Future<?> fileIOWorkerJob;

    private final int[] peerPieceAvailabilities;
    private final BitSet receivedPieces;

    private final PeerConnectionController connectionManager;
    private final QueuedTorrent torrent;

    /**
     * Create a new instance.
     *
     * @param torrent The torrent for which the data will be transferred
     * @param connectionManager For sending/receiving messages to/from remote peers
     * @param pieceCache The cache to use for data piece I/O
     */
    public TransferTask(final QueuedTorrent torrent, final PeerConnectionController connectionManager,
                        final DataPieceCache pieceCache) {
        this.connectionManager = connectionManager;
        this.torrent = torrent;

        final List<QueuedFileMetaData> fileMetaDatas = torrent.getMetaData().getFiles();
        final TreeMap<Long, TorrentFileIO> diskFileIOs = new TreeMap<>();

        for(final QueuedFileMetaData fileMetaData : fileMetaDatas) {
            diskFileIOs.put(fileMetaData.getOffset(), buildDiskFileIOs(fileMetaData));
        }

        final Consumer<FileOperationResult> pieceConsumer = this::fileOperationCompleted;
        this.fileIOWorker = new FileIOWorker(diskFileIOs, pieceCache, this.torrent.getMetaData(), pieceConsumer);

        final int pieceCount = torrent.getMetaData().getTotalPieces();
        peerPieceAvailabilities = new int[pieceCount];

        receivedPieces = torrent.getProgress().getObtainedPieces(pieceCount);
    }

    public void shutdown() {
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
            final InfoHash senderInfoHash = messageEvent.getPeerView().getInfoHash();
            final PwpMessage.MessageType messageType = messageEvent.getMessage().getMessageType();
            return torrent.getInfoHash().equals(senderInfoHash) &&
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
            return torrent.getInfoHash().equals(event.getPeerView().getInfoHash()) &&
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
            //TODO: Handle the I/O exception
        }
        else if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.READ) {
            handlePieceRead(fileOperationResult);
        } else if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.WRITE) {
            handlePieceWritten(fileOperationResult.getDataPiece().getIndex());
        }
    }

    private void applyChokingRotation(final boolean applyAntiSnubbing, final boolean applyOptimisticUnchoking) {

        /*System.out.println("\nCHOKING_ROTATION\n"
                + "\tdownloaders = " + downloaderPeers.size() + " [" + downloaderPeers + "]\n"
                + "\tchoked+interested+unchoked_us = " + downloaderCandidatePeers.size()
                + " [" + downloaderCandidatePeers + "]\n"
                + "\tchoked+not_interested+unchoked_us = " + generousPeers.size() + " [" + generousPeers + "]\n"
                + "\tstand_by_peers = " + standbyPeers.size());*/

        Optional<PeerView> peerToUnchoke = applyOptimisticUnchoking?
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

                /*System.out.println("Unchoking peer: " + peerToUnchoke.get().getIp() + ":"
                        + peerToUnchoke.get().getPort() + " because we only have " + downloaderPeers.size()
                        + " downloaders");*/

                unchokePeer(peerToUnchoke.get());
                peerToUnchoke = getRandomChokedPeerForUnchoking();
            } while(peerToUnchoke.isPresent() && downloaderPeers.size() < MAX_UNCHOKED_PEERS);
        }
        //We already have max peers unchoked, replace the one with the slowest upload rate
        else {
            final Optional<PeerView> slowestUploadingPeer = downloaderPeers.stream().reduce((first, second) ->
                    first.getAverageUploadRateSinceLastUnchoke() <
                            second.getAverageUploadRateSinceLastUnchoke()? first : second);
            if(slowestUploadingPeer.isPresent() && chokePeer(slowestUploadingPeer.get())) {

                /*System.out.println("Choked peer: " + slowestUploadingPeer.get().getIp() + ":"
                        + slowestUploadingPeer.get().getPort() + " because it has the slowest upload rate ["
                        + slowestUploadingPeer.get().getAverageUploadRateSinceLastUnchoke() + "]");*/

                connectionManager.send(new PwpMessageRequest(
                        PwpMessageFactory.getChokeMessage(), slowestUploadingPeer.get()));

                /*System.out.println("Unchoking peer: " + peerToUnchoke.get().getIp() + ":"
                        + peerToUnchoke.get().getPort() + " to replace the peer with the slowest upload rate");*/

                unchokePeer(peerToUnchoke.get());

                //TODO: Re-request choked peer's blocks from some other peer
            }
        }
    }

    private void applyAntiSnubbing() {

        //System.out.println("ANTI_SNUBBING_CHECK");

        final long currentTime = System.currentTimeMillis();

        final List<PeerView> snubbingPeers = downloaderPeers.stream().filter(peerView -> {
            final List<DataBlockIdentifier> peerBlockRequests = sentBlockRequests.get(peerView);
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

        //System.out.println("Choking " + snubbingPeers.size() + " peers because they have snubbed us");

        snubbingPeers.forEach(this::chokePeer);
        connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), snubbingPeers));

        //TODO: Re-request choked peer's blocks from some other peer
    }

    private Optional<PeerView> applyOptimisticUnchoking() {

        //System.out.println("OPTIMISTIC_UNCHOKING");

        final int randomUnchokingDistribution = ClientProperties.RANDOM_INSTANCE.nextInt(4);

        //3x more likely to unchoke a newly connected peer
        if(randomUnchokingDistribution < 3 && !standbyPeers.isEmpty()) {
            return Optional.of(standbyPeers.remove(standbyPeers.size()-1));
        }
        else {
            //Unchoke a random peer
            return getRandomChokedPeerForUnchoking();
        }
    }

    private Optional<PeerView> getRandomChokedPeerForUnchoking() {
        final List<PeerView> unchokeCandidates = new ArrayList<>();
        List<PeerView> chokedPeerQueue;

        //We prioritize peers in this order: interested, generous and then stand-by peers
        if(!downloaderCandidatePeers.isEmpty()) {
            unchokeCandidates.addAll(downloaderCandidatePeers);
            chokedPeerQueue = downloaderCandidatePeers;
        } else if(!generousPeers.isEmpty()) {
            unchokeCandidates.addAll(generousPeers);
            chokedPeerQueue = generousPeers;
        } else if(!standbyPeers.isEmpty()) {
            unchokeCandidates.addAll(standbyPeers);
            chokedPeerQueue = standbyPeers;
        } else {
            //There are no peers to choose from, simply return
            return Optional.empty();
        }

        final PeerView foundChokedPeer = unchokeCandidates.get(
                ClientProperties.RANDOM_INSTANCE.nextInt(unchokeCandidates.size()));

        //Remove the peer from its queue
        chokedPeerQueue.remove(foundChokedPeer);

        return Optional.of(foundChokedPeer);
    }

    private boolean chokePeer(final PeerView peer) {
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

    private void unchokePeer(final PeerView unchokedPeer) {
        unchokedPeer.setAreWeChoking(false);
        downloaderPeers.add(unchokedPeer);
        connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getUnchokeMessage(), unchokedPeer));
    }

    private void handlePieceWritten(final int pieceIndex) {

        //System.out.println("Wrote piece " + pieceIndex + " to disk and updated our stats");

        receivedPieces.set(pieceIndex);
        connectionManager.send(new PwpMessageRequest(
                PwpMessageFactory.buildHavePieceMessage(pieceIndex)));
    }

    private void handlePieceRead(final FileOperationResult fileOperationResult) {
        final DataPiece dataPiece = fileOperationResult.getDataPiece();
        final DataBlockIdentifier blockRequest = fileOperationResult.getBlockRequest();

        //TODO: Verify the DataPiece (SHA-1) before sending its block to the peer

        final DataBlock dataBlock = dataPiece.getBlock(
                blockRequest.getPieceOffset(), blockRequest.getBlockLength());

        connectionManager.send(new PwpMessageRequest(
                PwpMessageFactory.buildSendBlockMessage(dataBlock), fileOperationResult.getSender()));
        uploadingBlocks.put(blockRequest.getPieceIndex(), dataBlock);
    }

    private void handleFilePriorityChangeEvent(final FilePriorityChangeEvent filePriorityChangeEvent) {
        //TODO: Implement method
    }

    private void handlePeerStateChange(final PeerConnectionStateChangeEvent changeEvent) {
        final PeerView peer = changeEvent.getPeerView();

        boolean wasRemoved;

        if(!peer.areWeChoking()) {
            wasRemoved = downloaderPeers.remove(peer);

            //Unchoke a peer to replace the disconnected peer
            if(wasRemoved) {
                final Optional<PeerView> peerToUnchoke = getRandomChokedPeerForUnchoking();
                if(peerToUnchoke.isPresent()) {
                    unchokePeer(peerToUnchoke.get());
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
            //Update piece statistics, remove all piece counts for pieces that this peer had
            for (int i = 0; i < peerPieceAvailabilities.length; ++i) {
                if (peer.getHave(i)) {
                    --peerPieceAvailabilities[i];
                }
            }
        }
    }

    private void handlePeerMessage(final PwpMessageEvent messageEvent) {
        final PwpMessage message = messageEvent.getMessage();
        final PeerView peerView = messageEvent.getPeerView();

        switch(message.getMessageType()) {
            case HAVE:
                handleHaveMessage(message, peerView);
                break;
            case HANDSHAKE:
                handleHandshakeMessage(peerView);
                break;
            case BITFIELD:
                handleBitfieldMessage(peerView);
                break;
            case CHOKE:
                handleChokeMessage(peerView);
                break;
            case UNCHOKE:
                handleUnchokeMessage(peerView);
                break;
            case PIECE:
                handleBlockReceived(message, peerView);
                break;
            case REQUEST:
                handleBlockRequested(message, peerView);
                break;
            case CANCEL:
                handleCancelMessage(message, peerView);
                break;
            case INTERESTED:
                handleInterestedMessage(peerView);
                break;
            case NOT_INTERESTED:
                handleNotInterestedMessage(peerView);
                break;
        }
    }

    private void handleBitfieldMessage(final PeerView peerView) {
        if(standbyPeers.contains(peerView)) {
            for(int i = 0; i < torrent.getMetaData().getTotalPieces(); ++i) {
                if(peerView.getHave(i)) {
                    ++peerPieceAvailabilities[i];
                }
            }

            //TODO: Check whether this peer has anything we are INTERESTED in, remove below
            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getInterestedMessage(), peerView));
            peerView.setAreWeInterestedIn(true);
        }
    }

    private void handleHandshakeMessage(final PeerView peerView) {
        if(!standbyPeers.contains(peerView)) {
            standbyPeers.add(peerView);
        }
    }

    private void handleInterestedMessage(final PeerView peerView) {

        //System.out.println("INTERESTED from " + peerView.getIp() + ":" + peerView.getPort());

        if(peerView.isInterestedInUs()) {
            return;
        }

        peerView.setInterestedInUs(true);

        if(peerView.areWeChoking() && !peerView.isChokingUs()) {
            generousPeers.remove(peerView);
            downloaderCandidatePeers.add(peerView);
        }
    }

    private void handleNotInterestedMessage(final PeerView peerView) {

        //System.out.println("NOT_INTERESTED from " + peerView.getIp() + ":" + peerView.getPort());

        if(!peerView.isInterestedInUs()) {
            return;
        }

        peerView.setInterestedInUs(false);

        if(peerView.areWeChoking() && !peerView.isChokingUs()) {
            downloaderCandidatePeers.remove(peerView);
            generousPeers.add(peerView);
        }
    }

    private void handleCancelMessage(final PwpMessage message, final PeerView peerView) {

    }

    private void handleUnchokeMessage(final PeerView peerView) {

        //System.out.println("UNCHOKED by " + peerView.getIp() + ":" + peerView.getPort());

        if(!peerView.isChokingUs()) {
            return;
        }

        peerView.setChokingUs(false);

        //Check whether we are unchoked by a previous optimistic unchoke
        if(peerView.areWeChoking()) {
            standbyPeers.remove(peerView);
            if(downloaderPeers.size() < MAX_UNCHOKED_PEERS) {

                /*System.out.println("Unchoking peer: " + peerView.getIp() + ":" + peerView.getPort()
                        + " because it has unchoked us and there are only " + downloaderPeers.size() + " downloaders");*/

                unchokePeer(peerView);
            }
            else if(peerView.isInterestedInUs()){
                downloaderCandidatePeers.add(peerView);
            }
            else {
                generousPeers.add(peerView);
            }
        }

        //TODO: Request one of the rarest pieces from this peer, below is a simple test
        /*int pieceIndex = -1;
        for(int i = 0; i < torrent.getMetaData().getPieceLength(); ++i) {
            if(!downloadingPieces.containsKey(i) && !receivedPieces.get(i) && peerView.getHave(i)) {
                pieceIndex = i;
                break;
            }
        }

        if(pieceIndex != -1) {

            //System.out.println("Requesting piece " + pieceIndex + " from " + peerView.getIp() + ":" + peerView.getPort());

            final DataPiece requestedDataPiece = new DataPiece(new byte[torrent.getMetaData().getPieceLength()], pieceIndex);
            downloadingPieces.put(pieceIndex, requestedDataPiece);
            requestBlocks(requestedDataPiece, peerView);
        }*/
    }

    private void handleChokeMessage(final PeerView peerView) {

        //System.out.println("CHOKED by " + peerView.getIp() + ":" + peerView.getPort());

        if(peerView.isChokingUs()) {
            return;
        }

        peerView.setChokingUs(true);

        if(peerView.areWeChoking()) {
            if(peerView.isInterestedInUs()) {
                downloaderCandidatePeers.remove(peerView);
            }
            else {
                generousPeers.remove(peerView);
            }
        } else {
            downloaderPeers.remove(peerView);
            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), peerView));
            peerView.setAreWeChoking(true);

            //Replace the choked peer with another downloader
            final Optional<PeerView> peerToUnchoke = getRandomChokedPeerForUnchoking();
            if(peerToUnchoke.isPresent()) {
                unchokePeer(peerToUnchoke.get());
            }
        }

        standbyPeers.add(0, peerView);
    }

    private void handleBlockReceived(final PwpMessage message, final PeerView sender) {

        final DataBlock block;
        try {
            block = PwpMessageFactory.parseBlockReceivedMessage(message);
        }
        catch(final InvalidPeerMessageException ipme) {
            System.err.println(ipme.getMessage());
            return;
        }

        //START TEST
        //final String peerInfo = "[" + sender.getIp() + ":" + sender.getPort() + "]";
        //END TEST

        //Check whether we have requested this block from this peer
        final List<DataBlockIdentifier> blockRequests = sentBlockRequests.computeIfAbsent(
                sender, requestList -> new ArrayList());
        final Optional<DataBlockIdentifier> matchingRequest = blockRequests.stream().filter(request ->
                request.getBlockLength() == block.getBlockData().length
                        && request.getPieceIndex() == block.getPieceIndex()
                        && request.getPieceOffset() == block.getPieceOffset()
        ).findAny();
        if(!matchingRequest.isPresent()) {
            //We haven't requested this block
            //System.out.println("Didn't request this block: " + block + " from " + peerInfo);
            return;
        }

        /*System.out.println("Received " + block + " from " + peerInfo + " , correct_piece_length: "
                + torrent.getMetaData().getPieceLength());*/

        blockRequests.remove(matchingRequest.get());

        final int pieceIndex = block.getPieceIndex();
        final DataPiece dataPiece = downloadingPieces.get(pieceIndex);

        //System.out.println("Data piece for the received block from " + peerInfo + " is " + dataPiece.getIndex());

        if(dataPiece == null) {
            return;
        }

        final boolean blockAdded = dataPiece.addBlock(block);

        //System.out.println("Block received from " + peerInfo + " added to the pieces? " + blockAdded);

        if(!blockAdded) {
            return;
        }

        sender.updateBytesReceived(block.getBlockData().length);

        if(dataPiece.hasCompleted()) {
            //TODO: Fix ArrayIndexOutOfBoundsException when getting piece hash!
            final boolean validPiece = true; //dataPiece.validate(torrent.getMetaData().getPieceHash(pieceIndex));

            /*System.out.println("\nPIECE COMPLETED from " + peerInfo + " : " + dataPiece.getIndex()
                    + ", valid? " + validPiece + "\n");*/

            if(validPiece) {
                final DataPieceIdentifier dataPieceIdentifier = new DataPieceIdentifier(dataPiece, null, sender);
                fileIOWorker.writeDataPiece(dataPieceIdentifier);
            }

            downloadingPieces.remove(dataPiece);
            //TODO: Request a new piece, if any left
        }
        else {

            //System.out.println("Piece hasn't completed, requesting more blocks from " + peerInfo + "...");

            requestBlocks(dataPiece, sender);
        }
    }

    private void handleBlockRequested(final PwpMessage message, final PeerView requester) {
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

        //Read data only if we have this piece/block
        if(receivedPieces.get(blockRequest.getPieceIndex())) {
            fileIOWorker.readDataPiece(new DataPieceIdentifier(null, blockRequest, requester));
        }
    }

    private void requestBlocks(final DataPiece dataPiece, final PeerView receiver) {
        final List<DataBlockIdentifier> requestedBlocks = sentBlockRequests.computeIfAbsent(
                receiver, blocks -> new ArrayList<>());

        //START TEST
        //final String peerInfo = "[" + receiver.getIp() + ":" + receiver.getPort() + "]";
        //END TEST

        int pieceOffset;

        if(!requestedBlocks.isEmpty()) {

            //System.out.println("Pending block requests for " + peerInfo + ": " + requestedBlocks.size());

            final DataBlockIdentifier latestBlockRequest = requestedBlocks.get(requestedBlocks.size() - 1);
            pieceOffset = latestBlockRequest.getPieceOffset() + latestBlockRequest.getBlockLength();
        }
        else {
            pieceOffset = dataPiece.getBlockPointer();
        }

        //System.out.println("Piece offset for block request for " + peerInfo + " set to: " + pieceOffset);

        while(pieceOffset < dataPiece.getLength() && requestedBlocks.size() < MAX_BLOCK_REQUESTS_FOR_PIECE) {

            final int blockLength = pieceOffset + REQUESTED_BLOCK_LENGTH <= dataPiece.getLength()?
                    REQUESTED_BLOCK_LENGTH : dataPiece.getLength() - pieceOffset;

            final DataBlockIdentifier blockRequest = new DataBlockIdentifier(dataPiece.getIndex(), pieceOffset, blockLength);

            //System.out.println("Requesting block: " + blockRequest + " from " + peerInfo);

            final PwpMessage message = PwpMessageFactory.buildRequestMessage(blockRequest);
            connectionManager.send(new PwpMessageRequest(message, receiver));

            requestedBlocks.add(blockRequest);
            pieceOffset += blockLength;
        }
    }

    private void handleHaveMessage(final PwpMessage message, final PeerView peerView) {
        final int pieceIndex = UnitConverter.getInt(message.getPayload());
        ++peerPieceAvailabilities[pieceIndex];
        peerView.setHave(pieceIndex, true);
    }

    private TorrentFileIO buildDiskFileIOs(final QueuedFileMetaData fileMetaData) {
        final Path filePath = torrent.getProgress().getSavePath()
                .resolve(Paths.get(torrent.getMetaData().getName())).resolve(fileMetaData.getPath());

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
        //System.out.println(torrent.getInfoHash() + ": TransferTask.storeState()");

        connectionManager.removeConnectionListener(this);
        connectionManager.removeMessageListener(this);

        if(fileIOWorkerJob != null) {
            fileIOWorkerJob.cancel(true);
        }

        torrent.getProgress().storeObtainedPieces(receivedPieces);
    }

    private void restoreState() {
        //System.out.println(torrent.getInfoHash() + ": TransferTask.restoreState()");

        fileIOWorkerJob = ioWorkerExecutor.submit(fileIOWorker);

        connectionManager.addConnectionListener(this);
        connectionManager.addMessageListener(this);
    }
}