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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.net.pwp.PeerConnectionController;
import org.matic.torrent.net.pwp.PeerConnectionStateChangeEvent;
import org.matic.torrent.net.pwp.PeerSession;
import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.QueuedTorrentMetaData;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class TransferControllerTest {

    private final PeerConnectionController connectionControllerMock = EasyMock.createMock(PeerConnectionController.class);
    private final DataPieceCache dataPieceCacheMock = EasyMock.createMock(DataPieceCache.class);
    private final TorrentView torrentViewMock = EasyMock.createMock(TorrentView.class);

    private final QueuedTorrentMetaData metaDataMock = EasyMock.createMock(QueuedTorrentMetaData.class);
    private final PeerSession peerSessionMock = EasyMock.createMock(PeerSession.class);

    private final List<QueuedFileMetaData> fileMetaDatas = new ArrayList<>();

    private TransferController unitUnderTest;

    @Before
    public void setup() {
        EasyMock.reset(connectionControllerMock, dataPieceCacheMock, torrentViewMock, peerSessionMock,
                metaDataMock);
        fileMetaDatas.clear();
    }

    @Ignore
    @Test
    public void testPeerConnectedAndHasPiecesNotRequestedNorObtained() {
        final PeerConnectionStateChangeEvent connectionEvent = new PeerConnectionStateChangeEvent(peerSessionMock,
                PeerConnectionStateChangeEvent.PeerLifeCycleChangeType.CONNECTED, null);

        addFileMetadatas(true);
        setupCommonExpectations();
        replayMocks();

        unitUnderTest = new TransferController(
                torrentViewMock, connectionControllerMock, dataPieceCacheMock);

        unitUnderTest.handlePeerStateChange(connectionEvent);

        verifyMocks();
    }

    private void addFileMetadatas(final boolean singleFile) {
        fileMetaDatas.add(new QueuedFileMetaData(Paths.get(""), 42, 0));
        if(!singleFile) {
            fileMetaDatas.add(new QueuedFileMetaData(Paths.get(""), 38, 42));
        }
    }

    private void setupCommonExpectations() {
        EasyMock.expect(torrentViewMock.getMetaData()).andReturn(metaDataMock);
        EasyMock.expect(metaDataMock.getFiles()).andReturn(fileMetaDatas);
    }

    private void replayMocks() {
        EasyMock.replay(connectionControllerMock, dataPieceCacheMock, torrentViewMock, peerSessionMock,
                metaDataMock);
    }

    private void verifyMocks() {
        EasyMock.verify(connectionControllerMock, dataPieceCacheMock, torrentViewMock, peerSessionMock,
                metaDataMock);
    }
}