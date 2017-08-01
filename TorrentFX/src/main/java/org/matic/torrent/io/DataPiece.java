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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A piece of data that is a part of a torrent. Its contents can be checked
 * for validity by using hash information found in the info dictionary.
 *
 * @author Vedran Matic
 */
public class DataPiece {

    private static final String DIGEST_ALGORITHM = "SHA-1";

    private final MessageDigest validatorDigest;

    private final byte[] pieceBytes;
    private final int pieceIndex;

    protected final TreeMap<Integer, DataBlock> queuedBlocks = new TreeMap<>();

    protected int digestedBlocksPointer = 0;

    public DataPiece(final int pieceLength, final int pieceIndex) {
        this(new byte[pieceLength], pieceIndex);
    }

    public DataPiece(final byte[] pieceBytes, final int pieceIndex) {
        this.pieceBytes = pieceBytes;
        this.pieceIndex = pieceIndex;

        try {
            validatorDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new RuntimeException("This should never happen: the platform doesn't support " + DIGEST_ALGORITHM);
        }
    }

    public boolean addBlock(final DataBlock block) {
        final byte[] blockData = block.getBlockData();
        final int pieceOffset = block.getPieceOffset();

        if(pieceOffset + blockData.length > this.getLength() || block.getPieceIndex() != pieceIndex) {
            return false;
        }

        System.arraycopy(blockData, 0, pieceBytes, block.getPieceOffset(), blockData.length);

        if(pieceOffset == digestedBlocksPointer) {
            validatorDigest.update(block.getBlockData());
            digestedBlocksPointer += blockData.length;

            Map.Entry<Integer, DataBlock> nextQueuedBlock;
            while((nextQueuedBlock = queuedBlocks.firstEntry()) != null) {
                final DataBlock queuedDataBlock = nextQueuedBlock.getValue();
                final byte[] queuedBlockBytes = queuedDataBlock.getBlockData();
                if(queuedDataBlock.getPieceOffset() == digestedBlocksPointer) {
                    validatorDigest.update(queuedBlockBytes);
                    final int queuedDataBlockLength = queuedBlockBytes.length;
                    System.arraycopy(queuedBlockBytes, 0, pieceBytes,
                            queuedDataBlock.getPieceOffset(), queuedBlockBytes.length);

                    queuedBlocks.remove(nextQueuedBlock.getKey());
                    digestedBlocksPointer += queuedDataBlockLength;
                }
                else {
                    break;
                }
            }
        }
        else {
            queuedBlocks.put(pieceOffset, block);
        }

        return true;
    }

    public Optional<DataBlock> getBlock(final int offset, final int blockLength) {
        if(offset + blockLength > this.getLength() || offset < 0 || offset > this.getLength()
                || blockLength < 1 || blockLength > this.getLength()) {
            return Optional.empty();
        }

        final byte[] blockData = new byte[blockLength];
        System.arraycopy(pieceBytes, offset, blockData, 0, blockLength);
        final DataBlock dataBlock = new DataBlock(blockData, pieceIndex, offset);
        return Optional.of(dataBlock);
    }

    public int getIndex() {
        return pieceIndex;
    }

    public int getLength() {
        return pieceBytes.length;
    }

    public boolean hasCompleted() {
        return pieceBytes.length == digestedBlocksPointer;
    }

    public boolean validate(final byte[] expectedPieceHash) {
        final byte[] pieceHash = validatorDigest.digest();
        return Arrays.equals(expectedPieceHash, pieceHash);
    }

    protected byte[] getPieceBytes() {
        return pieceBytes;
    }

    @Override
    public String toString() {
        return "DataPiece{" +
                "pieceIndex=" + pieceIndex +
                ", digestedBlocksPointer=" + digestedBlocksPointer +
                ", length=" + pieceBytes.length +
                ", queuedBlocks=" + queuedBlocks.keySet() +
                '}';
    }
}