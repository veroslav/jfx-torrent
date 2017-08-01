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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.matic.torrent.net.pwp.PeerSession;
import org.matic.torrent.net.pwp.PwpPeer;

import java.util.Objects;

public final class PeerView {

    //Flags
    public static final String CLIENT_NOT_INTERESTED_AND_NOT_CHOKED_FLAG = "K";
    public static final String PEER_UNCHOKED_AND_NOT_INTERESTED_FLAG = "?";
    public static final String CLIENT_INTERESTED_AND_NOT_CHOKED_FLAG = "D";
    public static final String CLIENT_INTERESTED_AND_CHOKED_FLAG = "d";
    public static final String PEER_UNCHOKED_AND_INTERESTED_FLAG = "U";
    public static final String PEER_CHOKED_AND_INTERESTED_FLAG = "u";
    public static final String INCOMING_CONNECTION_FLAG = "I";
    public static final String PEER_SNUBBED = "S";

    private final StringProperty clientId = new SimpleStringProperty();
    private final StringProperty requests = new SimpleStringProperty();
    private final StringProperty flags = new SimpleStringProperty();
    private final StringProperty ip = new SimpleStringProperty();

    private final DoubleProperty percentDone = new SimpleDoubleProperty();
    private final LongProperty downSpeed = new SimpleLongProperty();
    private final DoubleProperty upSpeed = new SimpleDoubleProperty();

    private final LongProperty uploaded = new SimpleLongProperty();
    private final LongProperty downloaded = new SimpleLongProperty();
    private final DoubleProperty peerDownload = new SimpleDoubleProperty();

    private final IntegerProperty port = new SimpleIntegerProperty();

    private final PeerSession peerSession;
    private final int pieceCount;

    public PeerView(final PeerSession peerSession, final int pieceCount) {
        this.peerSession = peerSession;
        this.pieceCount = pieceCount;

        final PwpPeer peer = peerSession.getPeer();

        ip.set(peer.getIp());
        port.set(peer.getPort());
        clientId.set(peerSession.getClientId());
    }

    public PwpPeer getPeer() {
        return peerSession.getPeer();
    }

    public String getRequests() {
        return requests.get();
    }

    public StringProperty requestsProperty() {
        return requests;
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

    public IntegerProperty portProperty() {
        return port;
    }

    public int getPort() {
        return port.get();
    }

    public double getPercentDone() {
        return percentDone.get();
    }

    public DoubleProperty percentDoneProperty() {
        return percentDone;
    }

    public long getDownSpeed() {
        return downSpeed.get();
    }

    public LongProperty downSpeedProperty() {
        return downSpeed;
    }

    public double getUpSpeed() {
        return upSpeed.get();
    }

    public DoubleProperty upSpeedProperty() {
        return upSpeed;
    }

    public long getUploaded() {
        return uploaded.get();
    }

    public LongProperty uploadedProperty() {
        return uploaded;
    }

    public void setDownloaded(final long downloaded) {
        this.downloaded.set(downloaded);
    }

    public long getDownloaded() {
        return downloaded.get();
    }

    public LongProperty downloadedProperty() {
        return downloaded;
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

    public String getClientName() {
        return clientId.get();
    }

    public boolean isLogTraffic() {
        return peerSession.isLogTraffic();
    }

    public void setLogTraffic(final boolean logTraffic) {
        peerSession.setLogTraffic(logTraffic);
    }

    public void update() {
        downloaded.setValue(peerSession.getDownloadedBytes());
        uploaded.setValue(peerSession.getUploadedBytes());
        percentDone.set(updateAndGetPercentDone());
        flags.set(updateAndGetPeerFlags());
        requests.set(updateAndGetRequests());
    }

    private String updateAndGetRequests() {
        final StringBuilder reqBuilder = new StringBuilder();

        reqBuilder.append(peerSession.getRequestedBlocks());
        reqBuilder.append("|");
        reqBuilder.append(peerSession.getSentBlockRequests());

        return reqBuilder.toString();
    }

    private double updateAndGetPercentDone() {
        return (double)peerSession.getPieceCount()/ pieceCount * 100;
    }

    private String updateAndGetPeerFlags() {
        final StringBuilder flagsBuilder = new StringBuilder();

        if(peerSession.areWeInterestedIn()) {
            flagsBuilder.append(peerSession.isChokingUs()? CLIENT_INTERESTED_AND_CHOKED_FLAG
                    : CLIENT_INTERESTED_AND_NOT_CHOKED_FLAG);
            flagsBuilder.append(" ");
        }
        if(peerSession.isIncoming()) {
            flagsBuilder.append(INCOMING_CONNECTION_FLAG);
            flagsBuilder.append(" ");
        }
        if(!peerSession.isChokingUs() && !peerSession.areWeInterestedIn()) {
            flagsBuilder.append(CLIENT_NOT_INTERESTED_AND_NOT_CHOKED_FLAG);
            flagsBuilder.append(" ");
        }
        if(peerSession.isInterestedInUs()) {
            flagsBuilder.append(peerSession.areWeChoking()? PEER_CHOKED_AND_INTERESTED_FLAG
                    : PEER_UNCHOKED_AND_INTERESTED_FLAG);
            flagsBuilder.append(" ");
        }
        if(peerSession.isSnubbed()) {
            flagsBuilder.append(PEER_SNUBBED);
            flagsBuilder.append(" ");
        }
        if(!peerSession.isInterestedInUs() && !peerSession.areWeChoking()) {
            flagsBuilder.append(PEER_UNCHOKED_AND_NOT_INTERESTED_FLAG);
        }

        return flagsBuilder.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerView peerView = (PeerView) o;
        return Objects.equals(this.getPeer(), peerView.getPeer());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPeer());
    }

    @Override
    public String toString() {
        return getPeer().toString();
    }
}