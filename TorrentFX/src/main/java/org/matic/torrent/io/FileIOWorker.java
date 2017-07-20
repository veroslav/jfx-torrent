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

import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.io.cache.DataPieceCache;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.transfer.DataBlockIdentifier;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class FileIOWorker implements Runnable {

    private final List<DataPieceIdentifier> fileReaderQueue = new LinkedList<>();
    private final List<DataPieceIdentifier> fileWriterQueue = new LinkedList<>();

    private final Consumer<FileOperationResult> dataPieceConsumer;
    private final DataPieceCache pieceCache;

    //Sorted on offsets within the torrent, 0 to torrent.length() - 1
    private final TreeMap<Long, TorrentFileIO> diskFileIOs;
    private final QueuedTorrentMetaData torrentMetaData;

    public FileIOWorker(final TreeMap<Long, TorrentFileIO> diskFileIOs,
                        final DataPieceCache pieceCache,
                        final QueuedTorrentMetaData torrentMetaData,
                        final Consumer<FileOperationResult> dataPieceConsumer) {
        this.diskFileIOs = diskFileIOs;
        this.torrentMetaData = torrentMetaData;
        this.pieceCache = pieceCache;
        this.dataPieceConsumer = dataPieceConsumer;
    }

    public void writeDataPiece(final DataPieceIdentifier dataPieceIdentifier) {
        synchronized(this) {
            fileWriterQueue.add(dataPieceIdentifier);
            this.notifyAll();
        }
    }

    public void readDataPiece(final DataPieceIdentifier dataPieceIdentifier) {
        synchronized(this) {
            fileReaderQueue.add(dataPieceIdentifier);
            this.notifyAll();
        }
    }

    @Override
    public void run() {
        while(true) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                cleanup();

                return;
            }

            DataPieceIdentifier dataPieceToRead = null;
            DataPieceIdentifier dataPieceToWrite = null;

            synchronized(this) {
                while(fileWriterQueue.isEmpty() && fileReaderQueue.isEmpty()) {
                    try {
                        this.wait();
                    }
                    catch (final InterruptedException ie) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.interrupted();
                        }

                        cleanup();
                        return;
                    }
                }

                if(!fileWriterQueue.isEmpty()) {
                    dataPieceToWrite = fileWriterQueue.remove(0);
                }
                if(!fileReaderQueue.isEmpty()) {
                    dataPieceToRead = fileReaderQueue.remove(0);
                }
            }

            if(dataPieceToWrite != null) {
                handleWriteRequest(dataPieceToWrite);
            }
            if(dataPieceToRead != null) {
                handleReadRequest(dataPieceToRead);
            }
        }
    }

    //Need to close all of the file accessors on exit (RandomAccessFile.close())
    private void cleanup() {
        diskFileIOs.values().forEach(TorrentFileIO::cleanup);
    }

    private void handleReadRequest(final DataPieceIdentifier dataPieceIdentifier) {
        final int pieceLength = torrentMetaData.getPieceLength();
        final DataBlockIdentifier blockIdentifier = dataPieceIdentifier.getBlockIdentifier().get();
        final PeerView requester = dataPieceIdentifier.getTargetPeer();

        final int pieceIndex = blockIdentifier.getPieceIndex();
        final long pieceStart = pieceLength * pieceIndex;

        final long firstFileBeginPosition = diskFileIOs.floorKey(pieceStart);
        final long lastFileBeginPosition = diskFileIOs.floorKey(pieceStart + pieceLength);

        //Check whether the piece has been cached (faster)
        final Optional<DataPiece> cachedPiece = pieceCache.get(dataPieceIdentifier);
        if(cachedPiece.isPresent()) {
            dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                    cachedPiece.get(), requester, blockIdentifier, null));
        }

        //Retrieve piece data from the disk (slower)
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

                if(pieceBytesFromFile.length != pieceLength) {
                    dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                            null, requester, blockIdentifier,
                            new IOException("Read piece data length missmatch: expected " + pieceLength
                                    + " but got " + pieceBytesFromFile + " bytes")));
                    return;
                }

                System.arraycopy(pieceBytesFromFile, 0, pieceBytes, pieceBytesPosition, pieceBytesFromFile.length);
                pieceBytesPosition += pieceBytesFromFile.length;
            } catch (final IOException ioe) {
                dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                        null, requester, blockIdentifier, ioe));
                return;
            }
        }

        final DataPiece dataPiece = new DataPiece(pieceBytes, pieceIndex);

        //TODO: Re-enable after fixing the cache implementation
        //pieceCache.put(dataPieceIdentifier, dataPiece);

        dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.READ,
                dataPiece, requester, blockIdentifier, null));
    }

    private void handleWriteRequest(final DataPieceIdentifier dataPieceIdentifier) {
        final DataPiece dataPiece = dataPieceIdentifier.getDataPiece().get();
        final int expectedPieceLength = torrentMetaData.getPieceLength();
        final int pieceLength = dataPiece.getLength();

        if(pieceLength != expectedPieceLength) {
            //TODO: And throw a new InvalidPeerMessageException
            System.err.println("Invalid piece length: expected " + expectedPieceLength
                    + " but was " + pieceLength);
            return;
        }

        final int pieceIndex = dataPiece.getIndex();
        final long pieceStart = pieceLength * pieceIndex;

        final long firstFileBeginPosition = diskFileIOs.floorKey(pieceStart);
        final long lastFileBeginPosition = diskFileIOs.floorKey(pieceStart + expectedPieceLength);

        //Write bytes to all files that the piece spans across
        for(Long currentFilePosition = firstFileBeginPosition;
            currentFilePosition != null && currentFilePosition <= lastFileBeginPosition;
            currentFilePosition = diskFileIOs.higherKey(currentFilePosition)) {

            final TorrentFileIO fileIO = diskFileIOs.get(currentFilePosition);

            try {
                final int writtenBytes = fileIO.writePieceToDisk(dataPiece);
                if(writtenBytes == 0) {
                    dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.WRITE,
                            dataPiece, null, null, new IOException("No bytes were written for " + dataPiece)));
                    return;
                }
            } catch (final IOException ioe) {
                dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.WRITE,
                        dataPiece, null, null, ioe));
                return;
            }
        }

        //TODO: Re-enable after fixing the cache implementation
        //pieceCache.put(dataPieceIdentifier, dataPiece);

        dataPieceConsumer.accept(new FileOperationResult(FileOperationResult.OperationType.WRITE,
                dataPiece, null, null, null));
    }
}