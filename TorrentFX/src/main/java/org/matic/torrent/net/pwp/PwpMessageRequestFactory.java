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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataBlock;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.transfer.DataBlockRequest;

/**
 * A factory class and a parser for messages sent between peers.
 *
 * @author Vedran Matic
 *
 */
public final class PwpMessageRequestFactory {

    private static final byte[] KEEP_ALIVE_MESSAGE = new byte[] {0, 0, 0, 0};

    /**
     * Create raw bytes representing a KEEP_ALIVE message
     *
     * @return KEEP_ALIVE message as raw bytes
     */
    public static byte[] buildKeepAliveMessage() {
        return PwpMessageRequestFactory.KEEP_ALIVE_MESSAGE;
    }

    /**
     * Create raw bytes representing a HANDSHAKE message
     *
     * @param infoHash The info hash for which to build the handshake
     * @return HANDSHAKE message as raw bytes
     */
    public static byte[] buildHandshakeMessage(final InfoHash infoHash) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(70);
        try {
            baos.write(PeerSession.PROTOCOL_NAME_LENGTH);
            baos.write(PeerSession.PROTOCOL_NAME.getBytes(StandardCharsets.UTF_8));
            baos.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            baos.write(infoHash.getBytes());
            baos.write(ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8));
        }
        catch(final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    //TODO: Implement parse methods below
    public static PwpMessage buildSendBlockMessage(final DataBlock dataBlock) {
        return null;
    }

    public static PwpMessage buildRequestMessage(final DataBlockRequest dataBlockRequest) {
        return null;
    }

    public static PwpMessage buildHavePieceMessage(final int pieceIndex) {
        return null;
    }

    public static DataBlockRequest parseBlockRequestedMessage(final PwpMessage message) {
        return null;
    }

    public static DataBlock parseBlockReceivedMessage(final PwpMessage message) {
        return null;
    }
}