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

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.transfer.DataBlockRequest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class FileIOWorker implements Runnable {

    private final List<ReadDataPieceRequest> fileReaderQueue = new LinkedList<>();
    private final List<DataPiece> fileWriterQueue = new LinkedList<>();

    private final Consumer<FileOperationResult> dataPieceConsumer;
    private final DataPieceCache<InfoHash> pieceCache;

    //Sorted on offsets within the torrent, 0 to torrent.length() - 1
    private final TreeMap<Long, TorrentFileIO> diskFileIOs;
    private final QueuedTorrentMetaData torrentMetaData;

    public FileIOWorker(final TreeMap<Long, TorrentFileIO> diskFileIOs,
                        final DataPieceCache<InfoHash> pieceCache,
                        final QueuedTorrentMetaData torrentMetaData,
                        final Consumer<FileOperationResult> dataPieceConsumer) {
        this.diskFileIOs = diskFileIOs;
        this.torrentMetaData = torrentMetaData;
        this.pieceCache = pieceCache;
        this.dataPieceConsumer = dataPieceConsumer;
    }

    public void writeDataPiece(final DataPiece dataPiece) {
        synchronized(this) {
            fileWriterQueue.add(dataPiece);
            this.notifyAll();
        }
    }

    public void readDataPiece(final ReadDataPieceRequest blockRequest) {
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

            ReadDataPieceRequest readDataPieceRequest = null;
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
                    readDataPieceRequest = fileReaderQueue.remove(0);
                }
            }

            if(dataPiece != null) {
                handleWriteRequest(dataPiece);
            }
            if(readDataPieceRequest != null) {
                handleReadRequest(readDataPieceRequest);
            }
        }
    }

    private void handleReadRequest(final ReadDataPieceRequest pieceRequest) {
        final int pieceLength = torrentMetaData.getPieceLength();
        final DataBlockRequest blockRequest = pieceRequest.getBlockRequest();
        final long pieceStart = pieceLength * blockRequest.getPieceIndex();

        final long firstFileBeginPosition = diskFileIOs.floorKey(pieceStart);
        final long lastFileBeginPosition = diskFileIOs.floorKey(pieceStart + pieceLength);

        //Check whether the piece has been cached (faster)
        final InfoHash infoHash = torrentMetaData.getInfoHash();
        final DataPiece cachedPiece = pieceCache.getItem(infoHash);
        if(cachedPiece != null) {
            dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                    cachedPiece, pieceRequest.getRequester(), blockRequest));
        }

        //Retrieve piece data from the disk (slower)
        final int pieceIndex = pieceRequest.getBlockRequest().getPieceIndex();

        final byte[] pieceBytes = new byte[pieceLength];
        int pieceBytesPosition = 0;

        //Read bytes from all files that the piece spans across
        for(Long currentFilePosition = firstFileBeginPosition;
            currentFilePosition != null && currentFilePosition <= lastFileBeginPosition;
            currentFilePosition = diskFileIOs.higherKey(currentFilePosition)) {

            final TorrentFileIO fileIO = diskFileIOs.get(currentFilePosition);
            final byte[] pieceBytesFromFile;
            try {
                pieceBytesFromFile = fileIO.readPieceFromDisk(pieceLength, pieceIndex);
                System.arraycopy(pieceBytesFromFile, 0, pieceBytes, pieceBytesPosition, pieceBytesFromFile.length);
                pieceBytesPosition += pieceBytesFromFile.length;
            } catch (final IOException ioe) {
                ioe.printStackTrace();
                return;
            }
        }

        final DataPiece dataPiece = new DataPiece(pieceBytes, pieceIndex);
        pieceCache.addItem(infoHash, dataPiece);
        dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                dataPiece, pieceRequest.getRequester(), blockRequest));
    }

    private void handleWriteRequest(final DataPiece dataPiece) {
        final int pieceLength = torrentMetaData.getPieceLength();

        if(dataPiece.getLength() != pieceLength) {
            //TODO: And throw a new InvalidPeerMessageException
            System.err.println("Invalid piece length: expected " + pieceLength
                    + " but was " + dataPiece.getLength());
            return;
        }

        final int pieceIndex = dataPiece.getIndex();
        final long pieceStart = dataPiece.getLength() * pieceIndex;

        final long firstFileBeginPosition = diskFileIOs.floorKey(pieceStart);
        final long lastFileBeginPosition = diskFileIOs.floorKey(pieceStart + pieceLength);

        //Write bytes to all files that the piece spans across
        for(Long currentFilePosition = firstFileBeginPosition;
            currentFilePosition != null && currentFilePosition <= lastFileBeginPosition;
            currentFilePosition = diskFileIOs.higherKey(currentFilePosition)) {

            final TorrentFileIO fileIO = diskFileIOs.get(currentFilePosition);

            try {
                //TODO: Check whether write failed (written bytes == 0) and handle it
                fileIO.writePieceToDisk(dataPiece);
            } catch (final IOException ioe) {
                ioe.printStackTrace();
                return;
            }
        }

        pieceCache.addItem(torrentMetaData.getInfoHash(), dataPiece);
        dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.WRITE,
                dataPiece, null, null));
    }
}