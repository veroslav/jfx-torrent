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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that keeps track of remote peer connection's writes and reads.
 * 
 * @author Vedran Matic
 *
 */
public final class ConnectionSession {
	
	protected static final String PROTOCOL_NAME = "BitTorrent protocol";
	private static final String STRING_ENCODING = "UTF-8";
	
	protected static final byte PROTOCOL_NAME_LENGTH = (byte) ConnectionSession.PROTOCOL_NAME.length();
	private static final int MESSAGE_LENGTH_PREFIX_LENGTH = 4;
	private static final int HANDSHAKE_MESSAGE_LENGTH = 68;
	private static final int NO_PAYLOAD_MESSAGE_LENGTH = 5;
	private static final int RESERVED_BYTES_LENGTH = 8;
	private static final int INFO_HASH_LENGTH = 20;
	private static final int PEER_ID_LENGTH = 20;

	//Leftover data, if any, left from a previous read on this session's connection
	protected ByteBuffer backupReaderBuffer = null;

    private static final int INPUT_BUFFER_SIZE = 20 * 1024; //32 * 1024;
    private static final int OUTPUT_BUFFER_SIZE = 20 * 1024;   //32 * 1024;

    private final ByteBuffer inputBuffer = ByteBuffer.allocateDirect(INPUT_BUFFER_SIZE);
    private final ThreadLocal<ByteBuffer> outputBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(OUTPUT_BUFFER_SIZE));

    private final List<PwpMessage> messageWriteQueue = new ArrayList<>();

    private final SocketChannel channel;
    private final PeerSession peerSession;
    private final boolean incoming;

    private long lastActivityTime = System.currentTimeMillis();

	public ConnectionSession(final SocketChannel channel, final PeerSession peerSession, final boolean incoming) {
        this.channel = channel;
        this.peerSession = peerSession;
        this.incoming = incoming;
	}

    /**
     * Whether this session was initiated by a remote peer (incoming) or the client (outgoing).
     *
     * @return true if remote peer initiated this session, false otherwise
     */
	protected boolean isIncoming() {
	    return incoming;
    }

    protected PeerSession getPeerSession() {
        return peerSession;
    }

    protected long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Write as much of input data as possible to a connection

     * @return Whether all contents of data have been written to the connection
     * @throws IOException If an exception occurs while writing on the connection
     */
    protected boolean flushWriteQueue() throws IOException {
         while(!messageWriteQueue.isEmpty()) {
            final PwpMessage message = messageWriteQueue.get(0);
            final ByteBuffer localOutputBuffer = outputBuffer.get();

            //If buffer is empty, we process a new message, otherwise we write buffered data
            if(localOutputBuffer.position() == 0) {
                localOutputBuffer.put(message.getPayload());
            }

            localOutputBuffer.flip();
            final int bytesWritten = channel.write(localOutputBuffer);
            localOutputBuffer.compact();

            if(bytesWritten == 0) {
                return false;
            }

            messageWriteQueue.remove(0);
        }
        return true;
    }

    protected void putOnWriteQueue(final PwpMessageRequest messageRequest) throws IOException {
        messageWriteQueue.addAll(messageRequest.getMessages());
    }

	/**
	 * Read as much as possible from a connection and parse the contents as a list of peer-2-peer messages
	 *
	 * @return A list of parsed peer-wire-protocol messages
	 * @throws IOException If a string contained by a message can't be properly decoded or the connection is closed
     * @throws InvalidPeerMessageException if the message is of unknown format
	 */
	protected List<PwpMessage> read() throws IOException, InvalidPeerMessageException {
		final List<PwpMessage> messages = new ArrayList<>();
        lastActivityTime = System.currentTimeMillis();
		
		int bytesRead;
		while((bytesRead = channel.read(inputBuffer)) > 0) {
			messages.addAll(read(inputBuffer));
		}

		//Check whether the connection was closed or whether it is still active
		if(bytesRead == -1) {
			throw new IOException("Connection to peer was closed: " + channel.toString());
		}

		return messages;
	}
	
	/**
	 * Read as much as possible from a buffer and parse the contents as a list of peer-2-peer messages
	 * 
	 * @param buffer Buffer to read from
	 * @return A list of parsed peer-wire-protocol messages
	 * @throws UnsupportedEncodingException If a string contained by a message can't be properly decoded
	 */
	protected List<PwpMessage> read(final ByteBuffer buffer) throws IOException, InvalidPeerMessageException {
		final List<PwpMessage> messages = new ArrayList<>();
		
		buffer.flip();
		
		if(backupReaderBuffer != null) {			
			//Read as much as possible from buffer, until backupBuffer is full				
			while(buffer.remaining() > 0 && backupReaderBuffer.remaining() > 0) {
				backupReaderBuffer.put(buffer.get());
			}
			
			if(backupReaderBuffer.remaining() > 0) {
				buffer.clear();
				return messages;
			}
			
			backupReaderBuffer.flip();
			messages.addAll(parse(backupReaderBuffer));
            backupReaderBuffer = null;
			
			buffer.compact();
			buffer.flip();
		}
				
		messages.addAll(parse(buffer));
		return messages;
	}
	
	private List<PwpMessage> parse(final ByteBuffer buffer) throws IOException, InvalidPeerMessageException {
		final List<PwpMessage> messages = new ArrayList<>();							
		
		while(buffer.remaining() >= ConnectionSession.NO_PAYLOAD_MESSAGE_LENGTH) {
			final boolean isHandshake = checkForHandshake(buffer.duplicate());
			
			if(isHandshake) {
				final PwpMessage handshakeMessage = parseHandshake(buffer);				
				if(handshakeMessage == null) {					
					buffer.clear();
					break;
				}
				messages.add(handshakeMessage);
				continue;
			}

            final PwpMessage regularMessage = parseRegularMessage(buffer);
            if(regularMessage == null) {
                buffer.clear();
                break;
            }
            messages.add(regularMessage);
		}
		
		if(backupReaderBuffer != null) {
			return messages;
		}				
		
		//Handle the case when exactly 4 bytes remain and they happen to belong to KEEP_ALIVE message
		if(buffer.remaining() == ConnectionSession.MESSAGE_LENGTH_PREFIX_LENGTH) {
			final int messageLength = buffer.duplicate().getInt();
			if(messageLength == 0) {
				messages.add(new PwpMessage(PwpMessage.MessageType.KEEP_ALIVE));
				buffer.clear();				
				
				return messages;
			}			
		}
		
		buffer.compact();					
		return messages;
	}
	
	private PwpMessage parseRegularMessage(final ByteBuffer buffer) throws InvalidPeerMessageException {
		final int messageLength = buffer.getInt();		
		
		//First check whether we've got a KEEP_ALIVE message
		if(messageLength == 0) {
			return new PwpMessage(PwpMessage.MessageType.KEEP_ALIVE);
		}
		
		final byte messageId = buffer.get();

        //If the message is not BITFIELD, nor PIECE and is too long, it might be obfuscated
        if(messageId != 5 && messageId != 7 && messageLength > 13) {
            throw new InvalidPeerMessageException("Possibly obfuscated message data from: " + peerSession
                + " Incoming? " + isIncoming());
        }

		return parseMessageWithId(buffer, messageLength, messageId);
	}
	
	private PwpMessage parseMessageWithId(final ByteBuffer buffer, final int messageLength, final byte messageId) 
			throws InvalidPeerMessageException {
        if(messageLength < 1) {
            throw new InvalidPeerMessageException("Invalid message: messageId: " + messageId +
                    ", messageLength = " + messageLength + ", CAUSE: " + peerSession
                    + " Incoming? " + isIncoming());
        }

		//Check whether it is a message without payload
		if(messageId >= 0 && messageId < 4) {			
			return new PwpMessage(PwpMessage.fromMessageId(messageId));
		}

		//Check whether there is enough data in buffer to completely parse the message
		if(buffer.remaining() < messageLength - 1) {
			//Backup remaining buffer data for the partial message
			final int backupBufferCapacity = messageLength + ConnectionSession.MESSAGE_LENGTH_PREFIX_LENGTH;
			backupReaderBuffer = fromExistingBuffer(backupBufferCapacity);
			
			backupReaderBuffer.putInt(messageLength);
			backupReaderBuffer.put(messageId);
			backupReaderBuffer.put(buffer);
			return null;
		}
		
		//Parse message completely contained in the buffer
		final byte[] messagePayload = new byte[messageLength - 1];
		buffer.get(messagePayload);

		return new PwpMessage(PwpMessage.fromMessageId(messageId), messagePayload);
	}
	
	private PwpMessage parseHandshake(final ByteBuffer buffer) {		
		if(buffer.remaining() < ConnectionSession.HANDSHAKE_MESSAGE_LENGTH) {
			//Backup remaining buffer data for the partial HANDSHAKE message						
			backupReaderBuffer = fromExistingBuffer(ConnectionSession.HANDSHAKE_MESSAGE_LENGTH);
			backupReaderBuffer.put(buffer);
			return null;
		}
		//Parse HANDSHAKE completely, skip 20 bytes [protocol_length + protocol_name] 
		buffer.position(buffer.position() + ConnectionSession.PROTOCOL_NAME_LENGTH + 1);
		
		final byte[] reservedBytes = new byte[ConnectionSession.RESERVED_BYTES_LENGTH];
		final byte[] infoHash = new byte[ConnectionSession.INFO_HASH_LENGTH];
		final byte[] peerId = new byte[ConnectionSession.PEER_ID_LENGTH];
		
		buffer.get(reservedBytes);
		buffer.get(infoHash);
		buffer.get(peerId);
		 
		return new PwpHandshakeMessage(reservedBytes, infoHash, peerId);
	}
	
	private ByteBuffer fromExistingBuffer(final int capacity) {
		return ByteBuffer.allocate(capacity);
	}
	
	private boolean checkForHandshake(final ByteBuffer buffer) throws UnsupportedEncodingException {			
		if(buffer.get() != ConnectionSession.PROTOCOL_NAME_LENGTH) {
			return false;
		}
		
		final int availableBytesForPstr = buffer.remaining() >= ConnectionSession.PROTOCOL_NAME.length()?
				ConnectionSession.PROTOCOL_NAME.length() : buffer.remaining();
		final byte[] pstrBytes = new byte[availableBytesForPstr];
		buffer.get(pstrBytes);
		
		final String pstr = new String(pstrBytes, ConnectionSession.STRING_ENCODING);
		final int availableProtocolNameLength = availableBytesForPstr > ConnectionSession.PROTOCOL_NAME_LENGTH?
				ConnectionSession.PROTOCOL_NAME_LENGTH : availableBytesForPstr;
		
		return pstr.equals(ConnectionSession.PROTOCOL_NAME.substring(0, availableProtocolNameLength));
	}
}