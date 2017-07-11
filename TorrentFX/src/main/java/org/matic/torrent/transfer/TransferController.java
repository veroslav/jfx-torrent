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

import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PwpConnectionStateListener;
import org.matic.torrent.net.pwp.PwpMessage;
import org.matic.torrent.net.pwp.PwpMessageEvent;
import org.matic.torrent.net.pwp.PwpMessageListener;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.utils.UnitConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class manages download and upload of data in a torrent.
 *
 * @author Vedran Matic
 */
public final class TransferController implements PwpMessageListener, PwpConnectionStateListener, Runnable {

    private final List<PeerConnectionStateChangeEvent> peerStateChangeEventQueue = new ArrayList<>();
    private final List<PwpMessageEvent> messageQueue = new ArrayList<>();

    private final List<PeerView> peers = new ArrayList<>();

    private final QueuedTorrent torrent;

    public TransferController(final QueuedTorrent torrent) {
        this.torrent = torrent;
    }

    /**
     * @see {@link PwpMessageListener#onMessageReceived(PwpMessageEvent)}
     */
    @Override
    public void onMessageReceived(final PwpMessageEvent event) {
        synchronized(peers) {
            messageQueue.add(event);
            peers.notifyAll();
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
                    || messageType == PwpMessage.MessageType.HANDSHAKE
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
        synchronized(peers) {
            peerStateChangeEventQueue.add(event);
            peers.notifyAll();
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
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                storeState();
                return;
            }

            PeerConnectionStateChangeEvent peerEvent = null;
            PwpMessageEvent messageEvent = null;

            synchronized(peers) {
                while(peerStateChangeEventQueue.isEmpty() && messageQueue.isEmpty()) {
                    try {
                        peers.wait();
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
            }

            if(peerEvent != null) {
                handlePeerStateChange(peerEvent);
            }
            if(messageEvent != null) {
                handlePeerMessage(messageEvent);
            }
        }
    }

    private void handlePeerStateChange(final PeerConnectionStateChangeEvent changeEvent) {
        //System.out.println("TransferController: Received peer state change event = [" + changeEvent + "]");

        if(changeEvent.getEventType() == PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.DISCONNECTED) {
            peers.remove(changeEvent.getPeerView());
        }
    }

    private void handlePeerMessage(final PwpMessageEvent messageEvent) {
        final PwpMessage message = messageEvent.getMessage();
        switch(message.getMessageType()) {
            case HANDSHAKE:
                onHandshakeMessage(messageEvent.getPeerView());
                break;
            case HAVE:
                onHaveMessage(message, messageEvent.getPeerView());
                break;
            default:
                System.out.println("[" + torrent.getInfoHash() + "] Message from "
                        + messageEvent.getPeerView().getIp() + ": " + message.getMessageType());
        }
    }

    private void onHandshakeMessage(final PeerView peerView) {
        if(!peers.contains(peerView)) {
            System.out.println("[" + torrent.getInfoHash() + "] Message from " + peerView.getIp() + ": HANDSHAKE");
            peers.add(peerView);
        }
    }

    private void onHaveMessage(final PwpMessage message, final PeerView peerView) {
        final int pieceIndex = UnitConverter.getInt(message.getPayload());
        peerView.setHave(pieceIndex, true);
        System.out.println(torrent.getInfoHash() + ": peer " + peerView.getIp() + " has piece with index: " + pieceIndex);
    }

    private void storeState() {
        System.out.println(torrent.getInfoHash() + ": TransferController.storeState()");
    }

    private void restoreState() {
        System.out.println(torrent.getInfoHash() + ": TransferController.restoreState()");
    }
}