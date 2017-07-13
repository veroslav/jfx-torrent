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
import org.matic.torrent.io.TorrentDiskFileProxy;
import org.matic.torrent.io.TorrentFileIO;
import org.matic.torrent.net.pwp.PeerConnectionController;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PwpConnectionStateListener;
import org.matic.torrent.net.pwp.PwpMessage;
import org.matic.torrent.net.pwp.PwpMessageEvent;
import org.matic.torrent.net.pwp.PwpMessageListener;
import org.matic.torrent.net.pwp.PwpMessageRequest;
import org.matic.torrent.net.pwp.PwpMessageRequestFactory;
import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.action.FilePriorityChangeEvent;
import org.matic.torrent.queue.action.FilePriorityChangeListener;
import org.matic.torrent.utils.UnitConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This class manages download and upload of data in a torrent.
 *
 * @author Vedran Matic
 */
public final class TransferTask implements PwpMessageListener, PwpConnectionStateListener,
        FilePriorityChangeListener, Runnable {

    private static final int MAX_BLOCK_REQUESTS_FOR_PIECE = 10;
    private static final int REQUESTED_BLOCK_LENGTH = 16384; //16 kB

    private final Map<Integer, DataPiece> downloadingPieces = new HashMap<>();
    private final Map<Integer, DataBlock> uploadingBlocks = new HashMap<>();

    private final List<PeerView> unchockedPeersForDownload = new ArrayList<>();
    private final List<PeerView> unchockedPeersForUpload = new ArrayList<>();

    private final Map<Integer, List<DataBlockRequest>> sentBlockRequests = new HashMap<>();

    //Sorted on offsets within the torrent, 0 to torrent.length() - 1
    private TreeMap<Long, TorrentDiskFileProxy> diskFileProxies = new TreeMap<>();

    private FilePriorityChangeEvent filePriorityChangeEventsQueue = null;
    private final List<PeerConnectionStateChangeEvent> peerStateChangeEventQueue = new LinkedList<>();
    private final List<PwpMessageEvent> messageQueue = new LinkedList<>();

    private final ObjectProperty<TransferStatusChangeEvent> statusProperty = new SimpleObjectProperty();

    private final PeerConnectionController connectionManager;
    private final QueuedTorrent torrent;

    public TransferTask(final QueuedTorrent torrent, final PeerConnectionController connectionManager) {
        this.connectionManager = connectionManager;
        this.torrent = torrent;

        final List<QueuedFileMetaData> fileMetaDatas = torrent.getMetaData().getFiles();
        for(final QueuedFileMetaData fileMetaData : fileMetaDatas) {
            diskFileProxies.put(fileMetaData.getOffset(), buildDiskFileProxy(fileMetaData));
        }
    }

    public void addStatusChangeListener(final Consumer<TransferStatusChangeEvent> statusChangeHandler) {
        statusProperty.addListener((obs, oldV, newV) -> statusChangeHandler.accept(newV));
    }

    /**
     * @see {@link FilePriorityChangeListener#filePriorityChanged(FilePriorityChangeEvent)}
     */
    @Override
    public void filePriorityChanged(final FilePriorityChangeEvent changeEvent) {
        synchronized (this) {
            filePriorityChangeEventsQueue = changeEvent;
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
            return torrent.getInfoHash().equals(senderInfoHash) && (messageType == PwpMessage.MessageType.HAVE
                    || messageType == PwpMessage.MessageType.CHOKE
                    || messageType == PwpMessage.MessageType.UNCHOKE
                    || messageType == PwpMessage.MessageType.INTERESTED
                    || messageType == PwpMessage.MessageType.NOT_INTERESTED
                    || messageType == PwpMessage.MessageType.PIECE
                    || messageType == PwpMessage.MessageType.REQUEST);
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

    //TODO: Offload file I/O to another thread, don't handle it here
    @Override
    public void run() {
        restoreState();
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                storeState();
                return;
            }

            FilePriorityChangeEvent filePriorityChangeEvent = null;
            PeerConnectionStateChangeEvent peerEvent = null;
            PwpMessageEvent messageEvent = null;

            synchronized(this) {
                while(peerStateChangeEventQueue.isEmpty() && messageQueue.isEmpty()) {
                    try {
                        this.wait();
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
                if(filePriorityChangeEventsQueue != null) {
                    filePriorityChangeEvent = filePriorityChangeEventsQueue;
                    filePriorityChangeEventsQueue = null;
                }
            }

            if(peerEvent != null) {
                handlePeerStateChange(peerEvent);
            }
            if(messageEvent != null) {
                handlePeerMessage(messageEvent);
            }
            if(filePriorityChangeEvent != null) {
                handleFilePriorityChangeEvent(filePriorityChangeEvent);
            }
        }
    }

    private void handleFilePriorityChangeEvent(final FilePriorityChangeEvent filePriorityChangeEvent) {
        System.out.println("Priority set to " + filePriorityChangeEvent.getFilePriority()
                + " for file: " + filePriorityChangeEvent.getFilePath());
    }

    private void handlePeerStateChange(final PeerConnectionStateChangeEvent changeEvent) {
        //System.out.println("TransferTask: Received peer state change event = [" + changeEvent + "]");

        if(changeEvent.getEventType() == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED) {
            final PeerView peer = changeEvent.getPeerView();
            unchockedPeersForDownload.remove(peer);
            unchockedPeersForUpload.remove(peer);
        }
    }

    private void handlePeerMessage(final PwpMessageEvent messageEvent) {
        final PwpMessage message = messageEvent.getMessage();
        final PeerView peerView = messageEvent.getPeerView();
        switch(message.getMessageType()) {
            case HAVE:
                onHaveMessage(message, peerView);
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
            default:
                System.out.println("[" + torrent.getInfoHash() + "] Message from "
                        + messageEvent.getPeerView().getIp() + ": " + message.getMessageType());
        }
    }

    private void handleCancelMessage(final PwpMessage message, final PeerView peerView) {

    }

    private void handleUnchokeMessage(final PeerView peerView) {
        unchockedPeersForDownload.add(peerView);
    }

    private void handleChokeMessage(final PeerView peerView) {
        unchockedPeersForDownload.remove(peerView);
    }

    private void handleBlockReceived(final PwpMessage message, final PeerView sender) {
        final DataBlock block = PwpMessageRequestFactory.parseBlockReceivedMessage(message);

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
                handlePieceCompleted(dataPiece);
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

        final DataBlockRequest blockRequest = PwpMessageRequestFactory.parseBlockRequestedMessage(message);

        if(!unchockedPeersForUpload.contains(requester)) {
            return;
        }

        final int pieceLength = torrent.getMetaData().getPieceLength();
        final long pieceStart = pieceLength * blockRequest.getPieceIndex();
        final long fileProxyPieceOffset = diskFileProxies.floorKey(pieceStart);

        //TODO: Handle case when block/piece are spread across two or more files
        final TorrentDiskFileProxy fileProxy = diskFileProxies.get(fileProxyPieceOffset);
        try {
            final DataPiece dataPiece = fileProxy.retrievePiece(torrent.getInfoHash(),
                    blockRequest.getPieceIndex(), pieceLength);
            final DataBlock dataBlock = dataPiece.getBlock(
                    blockRequest.getPieceOffset(), blockRequest.getBlockLength());
            connectionManager.send(new PwpMessageRequest(
                    PwpMessageRequestFactory.buildSendBlockMessage(dataBlock), requester));
            uploadingBlocks.put(blockRequest.getPieceIndex(), dataBlock);
        }
        catch (final IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void requestBlocks(final DataPiece dataPiece, final PeerView receiver) {
        int pieceOffset = dataPiece.getBlockPointer();

        final List<DataBlockRequest> requestedBlocks = sentBlockRequests.computeIfAbsent(
                dataPiece.getIndex(), blocks -> new ArrayList<>());

        while(pieceOffset < dataPiece.getLength() && requestedBlocks.size() < MAX_BLOCK_REQUESTS_FOR_PIECE) {
            final int blockLength = pieceOffset + REQUESTED_BLOCK_LENGTH <= dataPiece.getLength()?
                    REQUESTED_BLOCK_LENGTH : dataPiece.getLength() - pieceOffset;

            final DataBlockRequest blockRequest = new DataBlockRequest(dataPiece.getIndex(), pieceOffset, blockLength);
            final PwpMessage message = PwpMessageRequestFactory.buildRequestMessage(blockRequest);
            connectionManager.send(new PwpMessageRequest(message, receiver));
            requestedBlocks.add(blockRequest);
            pieceOffset += blockLength;
        }
    }

    private void handlePieceCompleted(final DataPiece piece) {
        //TODO: Implement method
        connectionManager.send(new PwpMessageRequest(
                PwpMessageRequestFactory.buildHavePieceMessage(piece.getIndex())));
    }

    private void onHaveMessage(final PwpMessage message, final PeerView peerView) {
        final int pieceIndex = UnitConverter.getInt(message.getPayload());
        peerView.setHave(pieceIndex, true);
        //System.out.println(torrent.getInfoHash() + ": peer " + peerView.getIp() + " has piece with index: " + pieceIndex);
    }

    private TorrentDiskFileProxy buildDiskFileProxy(final QueuedFileMetaData fileMetaData) {
        final Path filePath = torrent.getProgress().getSavePath().resolve(fileMetaData.getPath());

        try {
            final TorrentFileIO fileIO = new TorrentFileIO(filePath, fileMetaData);
            return new TorrentDiskFileProxy(fileIO, null);
        } catch (final FileNotFoundException fnfe) {
            statusProperty.setValue(new TransferStatusChangeEvent(TransferStatusChangeEvent.EventType.ERROR,
                    fnfe.getMessage()));
            return null;
        }
    }

    private void storeState() {
        System.out.println(torrent.getInfoHash() + ": TransferTask.storeState()");

        connectionManager.removeConnectionListener(this);
        connectionManager.removeMessageListener(this);
    }

    private void restoreState() {
        System.out.println(torrent.getInfoHash() + ": TransferTask.restoreState()");

        connectionManager.addConnectionListener(this);
        connectionManager.addMessageListener(this);
    }
}