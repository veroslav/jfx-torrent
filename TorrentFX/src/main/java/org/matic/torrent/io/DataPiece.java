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

/**
 * A piece of data that is a part of a torrent. Its contents can be checked
 * for validity by using hash information found in the info dictionary.
 *
 * @author Vedran Matic
 */
public final class DataPiece {

    private static final String DIGEST_ALGORITHM = "SHA-1";

    private final MessageDigest validatorDigest;

    private final byte[] pieceBytes;
    private final long fileOffset;
    private final int pieceIndex;

    private int blockPointer = 0;

    public DataPiece(final byte[] pieceBytes, final int pieceIndex, final long fileOffset) {
        this.pieceBytes = pieceBytes;
        this.fileOffset = fileOffset;
        this.pieceIndex = pieceIndex;

        try {
            validatorDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new RuntimeException("This should never happen: the platform doesn't support " + DIGEST_ALGORITHM);
        }
    }

    public boolean addBlock(final DataBlock block) {
        final byte[] blockData = block.getBlockData();
        if(block.getPieceOffset() == blockPointer) {
            validatorDigest.update(block.getBlockData());
            System.arraycopy(blockData, 0, pieceBytes, block.getPieceOffset(), blockData.length);
            blockPointer += blockData.length;
            return true;
        }
        return false;
    }

    public DataBlock getBlock(final int offset, final int blockLength) {
        final byte[] blockData = new byte[blockLength];
        System.arraycopy(pieceBytes, offset, blockData, 0, blockLength);
        final DataBlock dataBlock = new DataBlock(blockData, pieceIndex, offset);
        return dataBlock;
    }

    public int getIndex() {
        return pieceIndex;
    }

    public int getLength() {
        return pieceBytes.length;
    }

    public int getBlockPointer() {
        return blockPointer;
    }

    public boolean hasCompleted() {
        return pieceBytes.length == blockPointer;
    }

    public boolean validate(final byte[] expectedPieceHash) {
        final byte[] pieceHash = validatorDigest.digest();
        return Arrays.equals(expectedPieceHash, pieceHash);
    }

    public long getFileOffset() {
        return fileOffset;
    }

    protected byte[] getPieceBytes() {
        return pieceBytes;
    }
}