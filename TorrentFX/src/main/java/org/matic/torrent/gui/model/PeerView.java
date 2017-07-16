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
package org.matic.torrent.gui.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.pwp.PwpPeer;

import java.util.Objects;

public final class PeerView {

    private final PwpPeer peer;

    private final StringProperty clientId = new SimpleStringProperty();
    private final StringProperty requests = new SimpleStringProperty();
    private final StringProperty flags = new SimpleStringProperty();
    private final StringProperty ip = new SimpleStringProperty();

    private final DoubleProperty percentDone = new SimpleDoubleProperty();
    private final DoubleProperty downSpeed = new SimpleDoubleProperty();
    private final DoubleProperty upSpeed = new SimpleDoubleProperty();

    private final DoubleProperty uploaded = new SimpleDoubleProperty();
    private final DoubleProperty downloaded = new SimpleDoubleProperty();
    private final DoubleProperty peerDownload = new SimpleDoubleProperty();

    private BitsView pieces = new BitsView(0);

    private boolean isChokingUs = true;
    private boolean isInterestedInUs = false;

    private boolean areWeChoking = true;
    private boolean areWeInterestedIn = false;

    public PeerView(final PwpPeer peer) {
        this.peer = peer;
        ip.setValue(peer.getIp());
    }

    public boolean isChokingUs() {
        return isChokingUs;
    }

    public void setChokingUs(final boolean isChokingUs) {
        this.isChokingUs = isChokingUs;
    }

    public boolean isInterestedInUs() {
        return isInterestedInUs;
    }

    public void setInterestedInUs(final boolean isInterestedInUs) {
        this.isInterestedInUs = isInterestedInUs;
    }

    public boolean isAreWeChoking() {
        return areWeChoking;
    }

    public void setAreWeChoking(final boolean areWeChoking) {
        this.areWeChoking = areWeChoking;
    }

    public boolean isAreWeInterestedIn() {
        return areWeInterestedIn;
    }

    public void setAreWeInterestedIn(final boolean areWeInterestedIn) {
        this.areWeInterestedIn = areWeInterestedIn;
    }

    public PwpPeer getPeer() {
        return peer;
    }

    public boolean getHave(final int pieceIndex) {
        return pieces.getHave(pieceIndex);
    }

    public void setHave(final int pieceIndex, final boolean have) {
        pieces.setHave(pieceIndex, have);
        percentDone.set((double)pieces.getHavePiecesCount() / pieces.getTotalPieces() * 100);
    }

    public void setPieces(final BitsView pieces) {
        this.pieces = pieces;
        percentDone.set((double)pieces.getHavePiecesCount() / pieces.getTotalPieces() * 100);
    }

    public int getPort() {
        return peer.getPort();
    }

    public InfoHash getInfoHash() {
        return peer.getInfoHash();
    }

    public void setInfoHash(final InfoHash infoHash) {
        peer.setInfoHash(infoHash);
    }

    public void setRequests(final String requests) {
        this.requests.set(requests);
    }

    public String getRequests() {
        return requests.get();
    }

    public StringProperty requestsProperty() {
        return requests;
    }

    public void setFlags(final String flags) {
        this.flags.set(flags);
    }

    public String getFlags() {
        return flags.get();
    }

    public StringProperty flagsProperty() {
        return flags;
    }

    public String getIp() {
        return ip.get();
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public void setPercentDone(final double percentDone) {
        this.percentDone.set(percentDone);
    }

    public double getPercentDone() {
        return percentDone.get();
    }

    public DoubleProperty percentDoneProperty() {
        return percentDone;
    }

    public void setDownSpeed(final double downSpeed) {
        this.downSpeed.set(downSpeed);
    }

    public double getDownSpeed() {
        return downSpeed.get();
    }

    public DoubleProperty downSpeedProperty() {
        return downSpeed;
    }

    public void setUpSpeed(final double upSpeed) {
        this.upSpeed.set(upSpeed);
    }

    public double getUpSpeed() {
        return upSpeed.get();
    }

    public DoubleProperty upSpeedProperty() {
        return upSpeed;
    }

    public void setUploaded(final double uploaded) {
        this.uploaded.set(uploaded);
    }

    public double getUploaded() {
        return uploaded.get();
    }

    public DoubleProperty uploadedProperty() {
        return uploaded;
    }

    public void setDownloaded(final double downloaded) {
        this.downloaded.set(downloaded);
    }

    public double getDownloaded() {
        return downloaded.get();
    }

    public DoubleProperty downloadedProperty() {
        return downloaded;
    }

    public void setPeerDownload(final double peerDownload) {
        this.peerDownload.set(peerDownload);
    }

    public double getPeerDownload() {
        return peerDownload.get();
    }

    public DoubleProperty peerDownloadProperty() {
        return peerDownload;
    }

    public StringProperty clientIdProperty() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId.set(clientId);
    }

    public String getClientName() {
        return clientId.get();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerView peerView = (PeerView) o;
        return Objects.equals(peer, peerView.peer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peer);
    }

    @Override
    public String toString() {
        return "PeerView{" +
                "peer=" + peer +
                '}';
    }
}