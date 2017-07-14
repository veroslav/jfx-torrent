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
package org.matic.torrent.io;

import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.transfer.DataBlockRequest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class FileIOWorker implements Runnable {

    private final List<DataPieceRequest> fileReaderQueue = new LinkedList<>();
    private final List<DataPiece> fileWriterQueue = new LinkedList<>();

    private final Consumer<DataPieceResponse> dataPieceConsumer;

    //Sorted on offsets within the torrent, 0 to torrent.length() - 1
    private final TreeMap<Long, TorrentDiskFileProxy> diskFileProxies;
    private final QueuedTorrentMetaData torrentMetaData;

    public FileIOWorker(final TreeMap<Long, TorrentDiskFileProxy> diskFileProxies,
                        final QueuedTorrentMetaData torrentMetaData,
                        final Consumer<DataPieceResponse> dataPieceConsumer) {
        this.diskFileProxies = diskFileProxies;
        this.torrentMetaData = torrentMetaData;
        this.dataPieceConsumer = dataPieceConsumer;
    }

    public void writeDataPiece(final DataPiece dataPiece) {
        synchronized(this) {
            fileWriterQueue.add(dataPiece);
            this.notifyAll();
        }
    }

    public void readDataPiece(final DataPieceRequest blockRequest) {
        synchronized(this) {
            fileReaderQueue.add(blockRequest);
            this.notifyAll();
        }
    }

    @Override
    public void run() {
        while(true) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                return;
            }

            DataPieceRequest dataPieceRequest = null;
            DataPiece dataPiece = null;

            synchronized(this) {
                while(fileWriterQueue.isEmpty() && fileReaderQueue.isEmpty()) {
                    try {
                        this.wait();
                    }
                    catch (final InterruptedException ie) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.interrupted();
                        }
                        return;
                    }
                }
                if(!fileWriterQueue.isEmpty()) {
                    dataPiece = fileWriterQueue.remove(0);
                }
                if(!fileReaderQueue.isEmpty()) {
                    dataPieceRequest = fileReaderQueue.remove(0);
                }
            }

            if(dataPiece != null) {
                handleWriteRequest(dataPiece);
            }
            if(dataPieceRequest != null) {
                handleReadRequest(dataPieceRequest);
            }
        }
    }

    private void handleReadRequest(final DataPieceRequest pieceRequest) {
        final int pieceLength = torrentMetaData.getPieceLength();
        final DataBlockRequest blockRequest = pieceRequest.getBlockRequest();
        final long pieceStart = pieceLength * blockRequest.getPieceIndex();
        final long fileProxyPieceOffset = diskFileProxies.floorKey(pieceStart);

        //TODO: Handle case when block/piece are spread across two or more files
        final TorrentDiskFileProxy fileProxy = diskFileProxies.get(fileProxyPieceOffset);
        try {
            final DataPiece dataPiece = fileProxy.retrievePiece(torrentMetaData.getInfoHash(),
                        blockRequest.getPieceIndex(), pieceLength);
            dataPieceConsumer.accept(new DataPieceResponse(dataPiece, pieceRequest.getRequester(), blockRequest));
        } catch (final IOException ioe) {
        }
    }

    private void handleWriteRequest(final DataPiece dataPiece) {

    }
}