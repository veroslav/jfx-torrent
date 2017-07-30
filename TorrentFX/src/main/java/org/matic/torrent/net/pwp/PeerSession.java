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

import org.matic.torrent.hash.InfoHash;

import java.util.BitSet;
import java.util.Objects;

/**
 * A bean that stores information about a peer's session while the client is connected to the peer.
 *
 * @author Vedran Matic
 */
public class PeerSession {

    private String clientId;

    private long bytesSentToUsSinceUnchoke = 0;
    private long unchokedByUsTime = 0;

    private int sentBlockRequests = 0;
    private int requestedBlocks = 0;

    private volatile boolean areWeInterestedIn = false;
    private volatile boolean isInterestedInUs = false;
    private volatile boolean areWeChoking = true;
    private volatile boolean isChokingUs = true;
    private volatile boolean isSnubbed = false;

    private BitSet pieces = new BitSet();
    private final boolean incoming;
    private final PwpPeer peer;

    public PeerSession(final PwpPeer peer, final boolean incoming) {
        this.peer = peer;
        this.incoming = incoming;
    }

    /**
     * Whether this session was initiated by a remote peer (incoming) or the client (outgoing).
     *
     * @return true if remote peer initiated this session, false otherwise
     */
    public boolean isIncoming() {
        return incoming;
    }

    public PwpPeer getPeer() {
        return peer;
    }

    public InfoHash getInfoHash() {
        return peer.getInfoHash();
    }

    public void setInfoHash(final InfoHash infoHash) {
        peer.setInfoHash(infoHash);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public int getSentBlockRequests() {
        return sentBlockRequests;
    }

    public void setSentBlockRequests(final int sentBlockRequests) {
        this.sentBlockRequests = sentBlockRequests;
    }

    public int getRequestedBlocks() {
        return requestedBlocks;
    }

    public void setRequestedBlocks(final int requestedBlocks) {
        this.requestedBlocks = requestedBlocks;
    }

    public boolean hasPiece(final int pieceIndex) {
        return pieces.get(pieceIndex);
    }

    public void setHasPiece(final int pieceIndex, final boolean have) {
        pieces.set(pieceIndex, have);
    }

    public BitSet getPieces() {
        return pieces;
    }

    public void setPieces(final BitSet pieces) {
        this.pieces = pieces;
    }

    public boolean isSeeder(final int pieceCount) {
        return pieces.cardinality() == pieceCount;
    }

    public boolean isSnubbed() {
        return isSnubbed;
    }

    public void setSnubbed(final boolean snubbed) {
        isSnubbed = snubbed;
    }

    public boolean isInterestedInUs() {
        return isInterestedInUs;
    }

    public void setInterestedInUs(final boolean isInterestedInUs) {
        this.isInterestedInUs = isInterestedInUs;
    }

    public boolean isChokingUs() {
        return isChokingUs;
    }

    public void setChokingUs(final boolean isChokingUs) {
        this.isChokingUs = isChokingUs;
    }

    public boolean areWeInterestedIn() {
        return areWeInterestedIn;
    }

    public void setAreWeInterestedIn(final boolean areWeInterestedIn) {
        this.areWeInterestedIn = areWeInterestedIn;
    }

    public boolean areWeChoking() {
        return areWeChoking;
    }

    public void setAreWeChoking(final boolean areWeChoking) {
        this.areWeChoking = areWeChoking;
        if(!areWeChoking) {
            unchokedByUsTime = System.currentTimeMillis();
            bytesSentToUsSinceUnchoke = 0;
        }
    }

    public void updateBytesReceived(final long byteCount) {
        bytesSentToUsSinceUnchoke += byteCount;
    }

    public double getAverageUploadRateSinceLastUnchoke() {
        return bytesSentToUsSinceUnchoke == 0? 0 :
                (double)bytesSentToUsSinceUnchoke / (System.currentTimeMillis() - unchokedByUsTime);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerSession that = (PeerSession) o;
        return Objects.equals(peer, that.peer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peer);
    }

    @Override
    public String toString() {
        final StringBuilder toStringBuilder = new StringBuilder();

        final InfoHash infoHash = peer.getInfoHash();

        toStringBuilder.append("[Hash = (");
        toStringBuilder.append(infoHash != null? infoHash.toString() : "no_handshake_yet");
        toStringBuilder.append(") ");
        toStringBuilder.append(peer.getIp());
        toStringBuilder.append(":");
        toStringBuilder.append(peer.getPort());
        toStringBuilder.append("]");

        return toStringBuilder.toString();
    }
}