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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.DataBlock;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.transfer.DataBlockIdentifier;

/**
 * A factory class and a parser for messages sent between the client and remote peers.
 *
 * @author Vedran Matic
 *
 */
public final class PwpMessageFactory {

    private static final byte[] PROTOCOL_NAME_BYTES = PeerSession.PROTOCOL_NAME.getBytes(StandardCharsets.UTF_8);
    private static final byte[] PEER_ID_BYTES = ClientProperties.PEER_ID.getBytes(StandardCharsets.UTF_8);

    private static final byte[] KEEP_ALIVE_MESSAGE_BYTES = new byte[] {0, 0, 0, 0};
    private static final byte[] RESERVED_BYTES = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};

    private static final PwpMessage INTERESTED_MESSAGE = PwpMessageFactory.buildInterestedMessage();

    private static final PwpMessage UNCHOKE_MESSAGE = PwpMessageFactory.buildUnchokeMessage();
    private static final PwpMessage CHOKE_MESSAGE = PwpMessageFactory.buildChokeMessage();

    /**
     * Create raw bytes representing a KEEP_ALIVE message.
     *
     * @return KEEP_ALIVE message as raw bytes
     */
    public static byte[] buildKeepAliveMessage() {
        return PwpMessageFactory.KEEP_ALIVE_MESSAGE_BYTES;
    }

    /**
     * Create raw bytes representing a HANDSHAKE message.
     *
     * @param infoHash The info hash for which to build the handshake
     * @return HANDSHAKE message as raw bytes
     */
    public static byte[] buildHandshakeMessage(final InfoHash infoHash) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(70);
        try {
            baos.write(PeerSession.PROTOCOL_NAME_LENGTH);
            baos.write(PROTOCOL_NAME_BYTES);
            baos.write(RESERVED_BYTES);
            baos.write(infoHash.getBytes());
            baos.write(PEER_ID_BYTES);
            baos.flush();
        }
        catch(final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    public static PwpMessage getUnchokeMessage() {
        return UNCHOKE_MESSAGE;
    }

    public static PwpMessage getChokeMessage() {
        return CHOKE_MESSAGE;
    }

    public static PwpMessage getInterestedMessage() {
        return INTERESTED_MESSAGE;
    }

    /**
     * Create a PIECE message. The PIECE message has the following format:
     *
     * [msg_length=int(9+block_data_length)][msg_id=byte(7)][piece_index=int][block_begin_offset=int][block_data]
     *
     * @param dataBlock Requested data block
     * @return The constructed PIECE message
     */
    public static PwpMessage buildSendBlockMessage(final DataBlock dataBlock) {
        //[(0, 0, 1, 3), (7), (0, 0, 0, 1), (0, 0, 0, 1), (msg_length - 9)]
        //  msg_length  msg_id  index          begin        block_data
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            final byte[] blockBytes = dataBlock.getBlockData();

            dos.writeInt(9 + blockBytes.length);        //Message length (9 + block_length)
            dos.writeByte(7);                           //Message id
            dos.writeInt(dataBlock.getPieceIndex());    //Piece index
            dos.writeInt(dataBlock.getPieceOffset());   //Block offset within the piece
            dos.write(blockBytes);
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.PIECE, baos.toByteArray());
        }
        catch(final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }

    /**
     * Create a REQUEST message. The REQUEST message has the following format:
     *
     * [msg_length=int(13)][msg_id=byte(6)][piece_index=int][block_begin_offset=int][block_length=int]
     *
     * @param dataBlockIdentifier Data block request
     * @return The constructed REQUEST message
     */
    public static PwpMessage buildRequestMessage(final DataBlockIdentifier dataBlockIdentifier) {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(13);                               //Message length
            dos.writeByte(6);                               //Message id
            dos.writeInt(dataBlockIdentifier.getPieceIndex()); //Piece index
            dos.writeInt(dataBlockIdentifier.getPieceOffset());//Block offset within the piece
            dos.writeInt(dataBlockIdentifier.getBlockLength());//Requested block's length
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.REQUEST, baos.toByteArray());
        }
        catch(final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }

    /**
     * Create a HAVE message. The HAVE message has the following format:
     *
     * [message_length=int(5)][msg_id=byte(4)][piece_index=int]
     *
     * @param pieceIndex Piece index
     * @return The constructed HAVE message
     */
    public static PwpMessage buildHavePieceMessage(final int pieceIndex) {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(5);            //Message length
            dos.writeByte(4);           //Message id
            dos.writeInt(pieceIndex);   //Piece index
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.HAVE, baos.toByteArray());
        }
        catch (final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }

    /**
     * Parse a REQUEST message. The REQUEST message has the following format:
     *
     * [msg_length=int(13)][msg_id=byte(6)][piece_index=int][block_begin_offset=int][block_length=int]
     *
     * @param message Message to parse
     * @return Resulting data block request
     * @throws InvalidPeerMessageException If the message has invalid format
     */
    public static DataBlockIdentifier parseBlockRequestedMessage(final PwpMessage message)
            throws InvalidPeerMessageException {

        try(final ByteArrayInputStream bais = new ByteArrayInputStream(message.getPayload());
            final DataInputStream dis = new DataInputStream(bais)) {

            final int pieceIndex = dis.readInt();   //Piece index
            final int pieceOffset = dis.readInt();  //Block offset within the piece
            final int blockLength = dis.readInt();  //Requested block's length

            return new DataBlockIdentifier(pieceIndex, pieceOffset, blockLength);
        }
        catch(final IOException ioe) {
            throw new InvalidPeerMessageException("Invalid REQUEST message: " + message, ioe);
        }
    }

    /**
     * Parse a PIECE message. The PIECE message has the following format:
     *
     * [msg_length=int(9+block_data_length)][msg_id=byte(7)][piece_index=int][block_begin_offset=int][block_data]
     *
     * @param message Message to parse
     * @return Resulting data block
     * @throws InvalidPeerMessageException If the message has invalid format
     */
    public static DataBlock parseBlockReceivedMessage(final PwpMessage message)
            throws InvalidPeerMessageException {

        final byte[] messagePayload = message.getPayload();
        try(final ByteArrayInputStream bais = new ByteArrayInputStream(messagePayload);
            final DataInputStream dis = new DataInputStream(bais)) {

            final int pieceIndex = dis.readInt();       //Piece index
            final int pieceOffset = dis.readInt();      //Block offset within the piece

            final byte[] blockData = new byte[messagePayload.length - 8];
            dis.read(blockData);                        //Block data bytes

            return new DataBlock(blockData, pieceIndex, pieceOffset);
        }
        catch(final IOException ioe) {
            throw new InvalidPeerMessageException("Invalid PIECE message: " + message, ioe);
        }
    }

    private static PwpMessage buildChokeMessage() {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(1);            //Message length
            dos.writeByte(0);           //Message id
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.CHOKE, baos.toByteArray());
        }
        catch (final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }

    private static PwpMessage buildUnchokeMessage() {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(1);            //Message length
            dos.writeByte(1);           //Message id
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.UNCHOKE, baos.toByteArray());
        }
        catch (final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }

    private static PwpMessage buildInterestedMessage() {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(1);            //Message length
            dos.writeByte(2);           //Message id
            dos.flush();

            return new PwpMessage(PwpMessage.MessageType.INTERESTED, baos.toByteArray());
        }
        catch (final IOException ioe) {
            //This can't happen for ByteArrayOutputStream
            return null;
        }
    }
}