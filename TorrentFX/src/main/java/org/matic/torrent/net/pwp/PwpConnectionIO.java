/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015 Vedran Matic
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
 * A class for managing the I/O buffers for a remote peer connection.
 * 
 * @author vedran
 *
 */
public final class PwpConnectionIO {
	
	private static final String PROTOCOL_NAME = "BitTorrent protocol";
	private static final String STRING_ENCODING = "UTF-8";
	
	private static final byte PROTOCOL_NAME_LENGTH = (byte)PwpConnectionIO.PROTOCOL_NAME.length();
	private static final int MESSAGE_LENGTH_PREFIX_LENGTH = 4;
	private static final int HANDSHAKE_MESSAGE_LENGTH = 68;
	private static final int NO_PAYLOAD_MESSAGE_LENGTH = 5;
	private static final int RESERVED_BYTES_LENGTH = 8;
	private static final int INFO_HASH_LENGTH = 20;
	private static final int PEER_ID_LENGTH = 20;
	private static final int WRITER_BUFFER_SIZE = 1024;
	private static final int READER_BUFFER_SIZE = 1024;
	
	private final ByteBuffer readerBuffer;		
	private final ByteBuffer writerBuffer;
	
	//Leftover data, if any, left from a previous read on this session's connection
	private ByteBuffer backupReaderBuffer;
	
	public PwpConnectionIO() {
		readerBuffer = ByteBuffer.allocateDirect(PwpConnectionIO.READER_BUFFER_SIZE);
		writerBuffer = ByteBuffer.allocateDirect(PwpConnectionIO.WRITER_BUFFER_SIZE);
		backupReaderBuffer = null;
	}
	
	/**
	 * Write as much of input data as possible to a connection
	 * 
	 * @param connection Remote connection to write to
	 * @param data Data to be written
	 * @return Whether all contents of data have been written to the connection
	 * @throws IOException If an exception occurs while writing on the connection
	 */
	protected boolean write(final SocketChannel connection, final byte[] data) throws IOException {
		//TODO: Handle BufferOverflowException when putting byte[] in the writerBuffer
		writerBuffer.put(data);
		
		while(writerBuffer.position() > 0) {
		    writerBuffer.flip();
		    final int bytesWritten = connection.write(writerBuffer);
		    writerBuffer.compact();
		    
		    if(bytesWritten == 0) {
		    	//TODO: Store leftover data on the queue, to write when writerBuffer has room again
		    	return false;
		    }						    
		}
		
		return true;
	}

	/**
	 * Read as much as possible from a connection and parse the contents as a list of peer-2-peer messages
	 * 
	 * @param connection Remote connection to read from
	 * @return A list of parsed peer-wire-protocol messages
	 * @throws IOException If a string contained by a message can't be properly decoded or the connection is closed
	 */
	protected List<PwpMessage> read(final SocketChannel connection) throws IOException {
		final List<PwpMessage> messages = new ArrayList<>();
		
		int bytesRead = -1;
		while((bytesRead = connection.read(readerBuffer)) > 0) {
			messages.addAll(read(readerBuffer));
		}
		
		//Check whether the connection was closed or whether it is still active
		if(bytesRead == -1) {
			throw new IOException("Connection to peer was closed");
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
	private List<PwpMessage> read(final ByteBuffer buffer) throws UnsupportedEncodingException {
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
	
	private List<PwpMessage> parse(final ByteBuffer buffer) throws UnsupportedEncodingException {
		final List<PwpMessage> messages = new ArrayList<>();							
		
		while(buffer.remaining() >= PwpConnectionIO.NO_PAYLOAD_MESSAGE_LENGTH) {				
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
			
			try {
				final PwpMessage regularMessage = parseRegularMessage(buffer);				
				if(regularMessage == null) {
					buffer.clear();
					break;					
				}			
				messages.add(regularMessage);
			} catch (final InvalidPeerMessageException ipme) {
				//TODO: Log IP:PORT of the peer sending the invalid message + notify listeners
				System.err.println("Invalid message received from peer: " + ipme.getMessage());
			}						
		}
		
		if(backupReaderBuffer != null) {
			return messages;
		}				
		
		//Handle the case when exactly 4 bytes remain and they happen to belong to KEEP_ALIVE message
		if(buffer.remaining() == PwpConnectionIO.MESSAGE_LENGTH_PREFIX_LENGTH) {
			final int messageLength = buffer.duplicate().getInt();
			if(messageLength == 0) {
				messages.add(new PwpRegularMessage(PwpMessage.MessageType.KEEP_ALIVE, null));
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
			return new PwpRegularMessage(PwpMessage.MessageType.KEEP_ALIVE, null);
		}
		
		final byte messageId = buffer.get();		
		return parseMessageWithId(buffer, messageLength, messageId);
	}
	
	private PwpMessage parseMessageWithId(final ByteBuffer buffer, final int messageLength, final byte messageId) 
			throws InvalidPeerMessageException {
		//Check whether it is a message without payload
		if(messageId >= 0 && messageId < 4) {			
			return new PwpRegularMessage(PwpMessage.fromMessageId(messageId), null);
		}
		
		//Check whether there is enough data in buffer to completely parse the message
		if(buffer.remaining() < messageLength - 1) {
			//Backup remaining buffer data for the partial message
			final int backupBufferCapacity = messageLength + PwpConnectionIO.MESSAGE_LENGTH_PREFIX_LENGTH;			
			backupReaderBuffer = fromExistingBuffer(buffer, backupBufferCapacity);
			
			backupReaderBuffer.putInt(messageLength);
			backupReaderBuffer.put(messageId);
			backupReaderBuffer.put(buffer);
			return null;
		}
		
		//Parse message completely contained in the buffer		
		final byte[] messagePayload = new byte[messageLength - 1];
		buffer.get(messagePayload);
		return new PwpRegularMessage(PwpMessage.fromMessageId(messageId), messagePayload);
	}
	
	private PwpMessage parseHandshake(final ByteBuffer buffer) {		
		if(buffer.remaining() < PwpConnectionIO.HANDSHAKE_MESSAGE_LENGTH) {
			//Backup remaining buffer data for the partial HANDSHAKE message						
			backupReaderBuffer = fromExistingBuffer(buffer, PwpConnectionIO.HANDSHAKE_MESSAGE_LENGTH);			
			backupReaderBuffer.put(buffer);
			return null;
		}
		//Parse HANDSHAKE completely, skip 20 bytes [protocol_length + protocol_name] 
		buffer.position(buffer.position() + PwpConnectionIO.PROTOCOL_NAME_LENGTH + 1);				
		
		final byte[] reservedBytes = new byte[PwpConnectionIO.RESERVED_BYTES_LENGTH];
		final byte[] infoHash = new byte[PwpConnectionIO.INFO_HASH_LENGTH];
		final byte[] peerId = new byte[PwpConnectionIO.PEER_ID_LENGTH];
		
		buffer.get(reservedBytes);
		buffer.get(infoHash);
		buffer.get(peerId);
		 
		return new PwpHandshakeMessage(reservedBytes, infoHash, peerId);
	}
	
	private ByteBuffer fromExistingBuffer(final ByteBuffer existing, final int capacity) {
		return existing.isDirect()? ByteBuffer.allocateDirect(capacity) :
			ByteBuffer.allocate(capacity);
	}
	
	private boolean checkForHandshake(final ByteBuffer buffer) throws UnsupportedEncodingException {			
		if(buffer.get() != PwpConnectionIO.PROTOCOL_NAME_LENGTH) {
			return false;
		}
		
		final int availableBytesForPstr = buffer.remaining() >= PwpConnectionIO.PROTOCOL_NAME.length()?
				PwpConnectionIO.PROTOCOL_NAME.length() : buffer.remaining();
		final byte[] pstrBytes = new byte[availableBytesForPstr];
		buffer.get(pstrBytes);
		
		final String pstr = new String(pstrBytes, PwpConnectionIO.STRING_ENCODING);
		final int availableProtocolNameLength = availableBytesForPstr > PwpConnectionIO.PROTOCOL_NAME_LENGTH?
				PwpConnectionIO.PROTOCOL_NAME_LENGTH : availableBytesForPstr;
		
		return pstr.equals(PwpConnectionIO.PROTOCOL_NAME.substring(0, availableProtocolNameLength));
	}
}