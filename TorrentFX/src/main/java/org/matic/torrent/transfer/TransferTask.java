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
import org.matic.torrent.io.ReadDataPieceRequest;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This class manages download and upload of data for a torrent.
 *
 * @author Vedran Matic
 */
public final class TransferTask implements PwpMessageListener, PwpConnectionStateListener,
        FilePriorityChangeListener, Runnable {

    //4 interested and 1 optimistic peer
    private static final int MAX_UNCHOKED_AND_INTERESTED_PEERS = 5;
    private static final int MAX_RAREST_PIECES = 10;

    private static final int MAX_BLOCK_REQUESTS_FOR_PIECE = 10;
    private static final int REQUESTED_BLOCK_LENGTH = 16384;    //16 kB

    //Various choking algorithm timeouts
    private static final long OPTIMISTIC_ROTATION_INTERVAL = 30000; //30 sec
    private static final long CHOKING_ROTATION_INTERVAL = 10000;    //10 sec
    private static final long ANTI_SNUBBING_INTERVAL = 60000;       //1 minute

    //Peers we have unchoked and who are interested
    private final List<PeerView> unchokedAndInterestedPeers = new ArrayList<>();

    //Peers we are choking and who are interested and have unchoked us
    private final List<PeerView> chokedAndInterestedPeers = new ArrayList<>();

    //Peers we are choking and which have unchoked us
    private final List<PeerView> unchokedUsPeers = new ArrayList<>();

    //Peers we are choking and are choking us
    private final List<PeerView> standbyPeers = new ArrayList<>();

    //Piece download/upload state tracking
    private final Map<PeerView, List<DataBlockRequest>> sentBlockRequests = new HashMap<>();
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
                        final DataPieceCache<InfoHash> pieceCache) {
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
        receivedPieces = new BitSet(pieceCount);
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
            final PeerView peerView = event.getPeerView();
            return torrent.getInfoHash().equals(event.getPeerView().getInfoHash()) &&
                    eventType == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED &&
                    (unchokedAndInterestedPeers.contains(peerView) ||
                    chokedAndInterestedPeers.contains(peerView) || standbyPeers.contains(peerView));
        };
    }

    @Override
    public void run() {
        restoreState();

        long lastChokingRotationTime = 0;
        long lastOptimisticUnchokeTime = 0;
        long lastAntiSnubbingCheckTime = 0;
        long timeLeftForChokingRotation;

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
                while((timeLeftForChokingRotation = getTimeLeftUntilChokingRotation(lastChokingRotationTime)) > 0
                        && eventQueuesEmpty()) {
                    try {
                        this.wait(timeLeftForChokingRotation);
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
                if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.READ) {
                    handlePieceRead(fileOperationResult);
                } else if(fileOperationResult.getOperationType() == FileOperationResult.OperationType.WRITE) {
                    handlePieceWritten(fileOperationResult.getDataPiece().getIndex());
                }
            }
            if(filePriorityChangeEvent != null) {
                handleFilePriorityChangeEvent(filePriorityChangeEvent);
            }

            if(timeLeftForChokingRotation <= 0) {
                final long currentTime = System.currentTimeMillis();

                if(getTimeLeftUntilAntiSnubbingCheck(lastAntiSnubbingCheckTime) <= 0) {
                    applyAntiSnubbingCheck();
                    lastAntiSnubbingCheckTime = currentTime;
                }

                applyChokingRotation();
                lastChokingRotationTime = currentTime;

                if(getTimeLeftUntilOptimisticUnchoking(lastOptimisticUnchokeTime) <= 0) {
                    applyOptimisticUnchoking();
                    lastOptimisticUnchokeTime = currentTime;
                }

                System.out.println("\nROTATION: downloaders = " + unchokedAndInterestedPeers.size()
                    + ", choked+interested = " + chokedAndInterestedPeers.size()
                    + ", have_unchoked_us = " + unchokedUsPeers.size()
                    + ", stand_by_peers = " + standbyPeers.size());
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

    private void applyAntiSnubbingCheck() {

        System.out.println("ANTI_SNUBBING_CHECK");

        /*final List<PeerView> snubbingPeers = unchokedAndInterestedPeers.stream().filter(peerView -> {
            final Long lastSentBlockTime = sentBlockRequests.get(peerView).stream().map(
                    DataBlockRequest::getCreationTime).min(Long::compareTo).get();
            return lastSentBlockTime != null && lastSentBlockTime > ANTI_SNUBBING_INTERVAL;
        }).collect(Collectors.toList());

        System.out.println("Removing " + snubbingPeers.size() + " due to anti-snubbing check");
        unchokedAndInterestedPeers.removeAll(snubbingPeers);

        chokedAndInterestedPeers.addAll(0, snubbingPeers);

        connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), snubbingPeers));
        snubbingPeers.forEach(peer -> peer.setAreWeChoking(true));*/

        //TODO: Re-request choked peer's blocks from some other peer
    }

    private void applyOptimisticUnchoking() {

        System.out.println("OPTIMISTIC_UNCHOKING");

        final int randomUnchokingDistribution = ClientProperties.RANDOM_INSTANCE.nextInt(4);

        if(randomUnchokingDistribution < 3) {

            System.out.println("Unchoking a newly connected peer");

            //3x more likely to unchoke a newly connected peer
            if(!standbyPeers.isEmpty()) {

                System.out.println("Stand-by peers is not empty");

                final PeerView optimisticPeer = standbyPeers.remove(standbyPeers.size()-1);

                //TODO: Replace "the slowest uploader" instead of random peer below
                if(unchokedAndInterestedPeers.size() == MAX_UNCHOKED_AND_INTERESTED_PEERS) {

                    System.out.println("Removing slowest downloader due to optimistic unchoking");

                    final PeerView slowestPeer = unchokedAndInterestedPeers.remove(
                            ClientProperties.RANDOM_INSTANCE.nextInt(unchokedAndInterestedPeers.size()));

                    //TODO: Place the slowestPeer into correct queue + send messages based on interest and choking
                    chokedAndInterestedPeers.add(slowestPeer);
                    connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getChokeMessage(), slowestPeer));
                    slowestPeer.setAreWeChoking(true);
                }

                //Add optimistically unchoked peer last in the queue
                unchokedAndInterestedPeers.add(optimisticPeer);
                connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getUnchokeMessage(), optimisticPeer));
                optimisticPeer.setAreWeChoking(false);
            }
        }
        else {

            System.out.println("Unchoking a random peer");

            //Unchoke a random peer
            while(unchokedAndInterestedPeers.size() < MAX_UNCHOKED_AND_INTERESTED_PEERS
                    && !chokedAndInterestedPeers.isEmpty()) {

                System.out.println("Unchoking inside while");

                final PeerView unchokedPeer = chokedAndInterestedPeers.remove(
                        ClientProperties.RANDOM_INSTANCE.nextInt(chokedAndInterestedPeers.size()));
                unchokedAndInterestedPeers.add(0, unchokedPeer);

                connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getUnchokeMessage(), unchokedPeer));
                connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getInterestedMessage(), unchokedPeer));

                unchokedPeer.setAreWeChoking(false);
                unchokedPeer.setAreWeInterestedIn(true);
            }
            //TODO: check unchokedUsPeers and standByPeers
        }
    }

    private void applyChokingRotation() {

        System.out.println("CHOKING_ROTATION");

    }

    private void handlePieceWritten(final int pieceIndex) {
        receivedPieces.set(pieceIndex);
        connectionManager.send(new PwpMessageRequest(
                PwpMessageFactory.buildHavePieceMessage(pieceIndex)));
    }

    private void handlePieceRead(final FileOperationResult fileOperationResult) {
        final DataPiece dataPiece = fileOperationResult.getDataPiece();
        final DataBlockRequest blockRequest = fileOperationResult.getBlockRequest();

        final DataBlock dataBlock = dataPiece.getBlock(
                blockRequest.getPieceOffset(), blockRequest.getBlockLength());
        connectionManager.send(new PwpMessageRequest(
                PwpMessageFactory.buildSendBlockMessage(dataBlock), fileOperationResult.getSender()));
        uploadingBlocks.put(blockRequest.getPieceIndex(), dataBlock);
    }

    private void handleFilePriorityChangeEvent(final FilePriorityChangeEvent filePriorityChangeEvent) {
        /*System.out.println("Priority set to " + filePriorityChangeEvent.getFilePriority()
                + " for file: " + filePriorityChangeEvent.getFilePath());*/
    }

    private void handlePeerStateChange(final PeerConnectionStateChangeEvent changeEvent) {
        final PeerView peer = changeEvent.getPeerView();

        chokedAndInterestedPeers.remove(peer);

        if(unchokedAndInterestedPeers.remove(peer)) {
            System.out.println("Removing downloader due to peer disconnection");
        }

        standbyPeers.remove(peer);

        //Update piece statistics, remove all piece counts for pieces that this peer had
        for(int i = 0; i < peerPieceAvailabilities.length; ++i) {
            if(peer.getHave(i)) {
                --peerPieceAvailabilities[i];
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

            //START DEBUG
            /*connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getInterestedMessage(), peerView));
            peerView.setAreWeInterestedIn(true);*/
            //END DEBUG
        }
    }

    private void handleHandshakeMessage(final PeerView peerView) {
        if(!standbyPeers.contains(peerView)) {
            standbyPeers.add(peerView);
        }
    }

    private void handleInterestedMessage(final PeerView peerView) {

        System.out.println("INTERESTED from " + peerView.getIp() + ":" + peerView.getPort());

        peerView.setInterestedInUs(true);
        if(standbyPeers.remove(peerView) && !chokedAndInterestedPeers.contains(peerView)) {
            chokedAndInterestedPeers.add(peerView);
        }
    }

    private void handleNotInterestedMessage(final PeerView peerView) {

        System.out.println("NOT_INTERESTED from " + peerView.getIp() + ":" + peerView.getPort());

        peerView.setInterestedInUs(false);
        if(unchokedAndInterestedPeers.remove(peerView)) {

            System.out.println("Removing peer due to it sending NOT_INTERESTED");

            unchokedUsPeers.add(peerView);
        }
        else if(chokedAndInterestedPeers.remove(peerView)) {
            unchokedUsPeers.add(peerView);
        }
    }

    private void handleCancelMessage(final PwpMessage message, final PeerView peerView) {

    }

    private void handleUnchokeMessage(final PeerView peerView) {

        System.out.println("UNCHOKED by " + peerView.getIp() + ":" + peerView.getPort());

        peerView.setChokingUs(false);
        if(!unchokedAndInterestedPeers.contains(peerView) && standbyPeers.remove(peerView)) {
            if(unchokedAndInterestedPeers.size() < MAX_UNCHOKED_AND_INTERESTED_PEERS) {
                unchokedAndInterestedPeers.add(0, peerView);
            }
            else {
                unchokedUsPeers.add(peerView);
            }

            connectionManager.send(new PwpMessageRequest(PwpMessageFactory.getUnchokeMessage(), peerView));
            peerView.setAreWeChoking(false);
        }
    }

    private void handleChokeMessage(final PeerView peerView) {

        System.out.println("CHOKED by " + peerView.getIp() + ":" + peerView.getPort());

        peerView.setChokingUs(true);
        if(unchokedAndInterestedPeers.remove(peerView) || unchokedUsPeers.remove(peerView)) {

            System.out.println("Might removed downloader due to it CHOKEing us");

            //TODO: Should we choke this peer here or in choking algorithm?
            standbyPeers.add(peerView);
        }
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

        final int pieceIndex = block.getPieceIndex();
        final DataPiece dataPiece = downloadingPieces.get(pieceIndex);

        if(dataPiece == null) {
            return;
        }

        final boolean blockAdded = dataPiece.addBlock(block);

        if(!blockAdded) {
            return;
        }

        if(dataPiece.hasCompleted()) {
            final boolean validPiece = dataPiece.validate(torrent.getMetaData().getPieceHash(pieceIndex));
            if(validPiece) {
                fileIOWorker.writeDataPiece(dataPiece);
            }
            downloadingPieces.remove(dataPiece);
            //TODO: Request a new piece, if any left
        }
        else {
            //TODO: Firstly check whether we are still UNCHOKEd by the sender
            requestBlocks(dataPiece, sender);
        }
    }

    private void handleBlockRequested(final PwpMessage message, final PeerView requester) {
        final DataBlockRequest blockRequest;
        try {
            blockRequest = PwpMessageFactory.parseBlockRequestedMessage(message);
        } catch (final InvalidPeerMessageException ipme) {
            System.err.println(ipme.getMessage());
            return;
        }

        if(!unchokedAndInterestedPeers.contains(requester)) {
            return;
        }

        fileIOWorker.readDataPiece(new ReadDataPieceRequest(blockRequest, requester));
    }

    private void requestBlocks(final DataPiece dataPiece, final PeerView receiver) {
        int pieceOffset = dataPiece.getBlockPointer();

        final List<DataBlockRequest> requestedBlocks = sentBlockRequests.computeIfAbsent(
                receiver, blocks -> new ArrayList<>());

        while(pieceOffset < dataPiece.getLength() && requestedBlocks.size() < MAX_BLOCK_REQUESTS_FOR_PIECE) {
            final int blockLength = pieceOffset + REQUESTED_BLOCK_LENGTH <= dataPiece.getLength()?
                    REQUESTED_BLOCK_LENGTH : dataPiece.getLength() - pieceOffset;

            final DataBlockRequest blockRequest = new DataBlockRequest(dataPiece.getIndex(), pieceOffset, blockLength);
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
            final TorrentFileIO fileIO = new TorrentFileIO(filePath, fileMetaData);
            return fileIO;
        } catch (final IOException ioe) {
            statusProperty.setValue(new TransferStatusChangeEvent(TransferStatusChangeEvent.EventType.ERROR,
                    ioe.getMessage()));
            return null;
        }
    }

    private void storeState() {
        System.out.println(torrent.getInfoHash() + ": TransferTask.storeState()");

        connectionManager.removeConnectionListener(this);
        connectionManager.removeMessageListener(this);

        if(fileIOWorkerJob != null) {
            fileIOWorkerJob.cancel(true);
        }
    }

    private void restoreState() {
        System.out.println(torrent.getInfoHash() + ": TransferTask.restoreState()");

        fileIOWorkerJob = ioWorkerExecutor.submit(fileIOWorker);

        connectionManager.addConnectionListener(this);
        connectionManager.addMessageListener(this);
    }
}