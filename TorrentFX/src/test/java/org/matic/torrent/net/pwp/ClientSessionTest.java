/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.net.pwp.PwpMessage.MessageType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class ClientSessionTest {

	private final String protocolName = "BitTorrent protocol";
    private final PwpPeer peer = new PwpPeer("127.0.0.1", 44444, new InfoHash("1".getBytes(StandardCharsets.UTF_8)));
    private final byte[] peerId = {'-', 'D', 'E', '5', '4', '3', '2', '-', 2, 1, 0, 9, 8, 7, 6, 5, 4, 3, 2, 1};
	
	//Parse empty buffer
	@Test
	public void testEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(0);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 0, 0));
	}
	
	//Parse empty buffer with non-zero capacity
	@Test
	public void testEmptyBufferWithNonZeroCapacity() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(10);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 10, 10));
	}
	
	//Parse invalid regular message of correct length
	@Test(expected = InvalidPeerMessageException.class)
	public void testInvalidRegularMessage() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(5);
		
		final byte[] message = {0, 0, 0, 1, 12};		
		buffer.put(message);
		
		unitUnderTest.read(buffer);
	}
	
	//Parse fully contained keep_alive message, buffer empty afterwards
	@Test
	public void testKeepAliveFullyContainedEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(4);
		
		final byte[] message = {0, 0, 0, 0};		
		buffer.put(message);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.KEEP_ALIVE);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 4, 4));
	}
	
	//Parse fully contained keep_alive message, buffer contains more data afterwards
	@Test
	public void testKeepAliveFullyContainedNonEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(10);
		
		final byte[] message = {0, 0, 0, 0, 0, 0};		
		buffer.put(message);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.KEEP_ALIVE);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 2, 10, 8));		
	}
	
	//Parse partially contained keep_alive message, spread over two buffer reads
	@Test
	public void testKeepAlivePartiallyContainedTwoBufferReads() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(10);
		
		buffer.put(new byte[] {0});		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 1, 10, 9));
		
		buffer.put(new byte[] {0, 0, 0});
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.KEEP_ALIVE);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 10, 10));
	}
	
	//Parse partially contained keep_alive message, spread over three buffer reads
	@Test
	public void testKeepAlivePartiallyContainedThreeBufferReads() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(10);
		
		buffer.put(new byte[] {0, 0});		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 2, 10, 8));
		
		buffer.put(new byte[] {0});
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 3, 10, 7));
		
		buffer.put(new byte[] {0});
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.KEEP_ALIVE);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 10, 10));
	}
	
	//Parse fully contained regular message, buffer empty afterwards
	@Test
	public void testRegularMessageFullyContainedEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(9);
		
		//HAVE_MESSAGE
		buffer.put(new byte[] {0, 0, 0, 5, 4, 1, 2, 3, 4});		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(1, messages.size());				
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HAVE);
		
		final PwpRegularMessage result = (PwpRegularMessage)messages.get(0);
		final byte[] expectedPayload = {1, 2, 3, 4};
		
		Assert.assertTrue(Arrays.equals(expectedPayload, result.getPayload()));
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 9, 9));
	}
	
	//Parse fully contained regular message, buffer contains more data afterwards
	@Test
	public void testRegularMessageFullyContainedNonEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(13);
		
		//HAVE_MESSAGE
		buffer.put(new byte[] {0, 0, 0, 5, 4, 1, 2, 3, 4, 0, 0, 1, 3});		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(1, messages.size());				
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HAVE);
		
		final PwpRegularMessage result = (PwpRegularMessage)messages.get(0);
		final byte[] expectedPayload = {1, 2, 3, 4};
		
		Assert.assertTrue(Arrays.equals(expectedPayload, result.getPayload()));
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 4, 13, 9));
	}
	
	//Parse partially contained regular message, spread over two buffer reads, backupBuffer used
	@Test
	public void testRegularMessagePartiallyContainedTwoReadsWithBackupBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(9);
		final byte[] bytesToBackup = new byte[] {0, 0, 0, 5, 4};
		
		//HAVE_MESSAGE
		buffer.put(bytesToBackup);		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 9, 9));
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 5, 9, 4));
		
		//Verify that bytes stored in backupBuffer are == {0, 0, 0, 5, 4}
		final byte[] actualBackup = new byte[bytesToBackup.length];
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		copyOfBackup.get(actualBackup);
		Assert.assertTrue(Arrays.equals(bytesToBackup, actualBackup));
		
		buffer.put(new byte[] {1, 2, 3, 4});
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());				
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HAVE);
		
		final PwpRegularMessage result = (PwpRegularMessage)messages.get(0);
		final byte[] expectedPayload = {1, 2, 3, 4};
		
		Assert.assertTrue(Arrays.equals(expectedPayload, result.getPayload()));
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 9, 9));
	}
	
	//Parse partially contained regular message, spread over three buffer reads, backupBuffer used
	@Test
	public void testRegularMessagePartiallyContainedThreeReadsWithBackupBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(20);
		
		//REQUEST message length and id
		buffer.putInt(13);
		buffer.put((byte)6);		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 5, 17, 12));
		
		//Put index
		buffer.putInt(423);
		//Put begin
		buffer.putInt(19);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 13, 17, 4));
		
		//Verify contents of the backup buffer
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		Assert.assertEquals(13, copyOfBackup.getInt());
		Assert.assertEquals(6, copyOfBackup.get());
		Assert.assertEquals(423, copyOfBackup.getInt());
		Assert.assertEquals(19, copyOfBackup.getInt());
		
		//Put length
		buffer.putInt(32);
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());				
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.REQUEST);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));		
	}
	
	//Parse fully contained handshake message, empty buffer afterwards
	@Test
	public void testHandshakeFullyContainedEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(100);		
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};
		
		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		buffer.put(peerId);
	
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		
		final PwpHandshakeMessage actualMessage = (PwpHandshakeMessage)messages.get(0);
		
		//Validate message contents
		Assert.assertArrayEquals(infoHash, actualMessage.getInfoHash().getBytes());
        Assert.assertEquals("Deluge 5.4.3.2", actualMessage.getPeerId());
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 100, 100));
	}
	
	//Parse fully contained handshake message, buffer contains more data afterwards
	@Test
	public void testHandshakeFullyContainedNonEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(100);		
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};
		final byte[] leftBytes = {1, 1, 1};
		
		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		buffer.put(peerId);
		buffer.put(leftBytes);
	
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		
		final PwpHandshakeMessage actualMessage = (PwpHandshakeMessage)messages.get(0);
		
		//Validate message contents
		Assert.assertTrue(Arrays.equals(infoHash, actualMessage.getInfoHash().getBytes()));
		Assert.assertEquals("Deluge 5.4.3.2", actualMessage.getPeerId());
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 3, 100, 97));
	}
	
	//Parse partially contained handshake message, spread over two buffer reads, no payload
	@Test
	public void testHandshakePartiallyContainedTwoReadsNoPayload() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(100);
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};

		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 20, 68, 48));
		Assert.assertTrue(verifyBufferState(buffer, 0, 100, 100));
		
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		
		Assert.assertEquals(19, copyOfBackup.get());
		
		final byte[] actualProtocolName = new byte[protocolName.length()];
		copyOfBackup.get(actualProtocolName);

		Assert.assertEquals(protocolName, new String(actualProtocolName, "UTF-8"));
		
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		buffer.put(peerId);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		
		final PwpHandshakeMessage actualMessage = (PwpHandshakeMessage)messages.get(0);
		
		//Validate message contents
		Assert.assertTrue(Arrays.equals(infoHash, actualMessage.getInfoHash().getBytes()));
		Assert.assertEquals("Deluge 5.4.3.2", actualMessage.getPeerId());
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 100, 100));
	}
	
	//Parse partially contained handshake message, spread over two buffer reads, partial payload
	@Test
	public void testHandshakePartiallyContainedTwoReadsPartialPayload() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(100);
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};

		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 48, 68, 20));
		Assert.assertTrue(verifyBufferState(buffer, 0, 100, 100));
		
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		
		Assert.assertEquals(19, copyOfBackup.get());
		
		final byte[] actualProtocolName = new byte[protocolName.length()];
		copyOfBackup.get(actualProtocolName);
		Assert.assertEquals(protocolName, new String(actualProtocolName, "UTF-8"));
		
		final byte[] backupOfReservedBytes = new byte[8];
		copyOfBackup.get(backupOfReservedBytes);
		Assert.assertTrue(Arrays.equals(backupOfReservedBytes, reservedBytes));
		
		final byte[] backupOfInfoHash = new byte[20];
		copyOfBackup.get(backupOfInfoHash);
		Assert.assertTrue(Arrays.equals(backupOfInfoHash, infoHash));
				
		buffer.put(peerId);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		
		final PwpHandshakeMessage actualMessage = (PwpHandshakeMessage)messages.get(0);

		//Validate message contents
		Assert.assertTrue(Arrays.equals(infoHash, actualMessage.getInfoHash().getBytes()));
		Assert.assertEquals("Deluge 5.4.3.2", actualMessage.getPeerId());
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 100, 100));
	}
	
	//Parse fully contained handshake + bitfield + have, buffer empty afterwards
	@Test
	public void testHandshakeBitfieldHaveFullyContainedEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(120);
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};

		//Put HANDSHAKE message
		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		buffer.put(peerId);
		
		//Put BITFIELD message (length(bitfield) == 32) 
		buffer.putInt(9);
		buffer.put((byte)5);
		buffer.putLong(777);
		
		//Put HAVE message
		buffer.putInt(5);
		buffer.put((byte)4);
		buffer.putInt(1976);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(3, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		Assert.assertTrue(messages.get(1).getMessageType() == MessageType.BITFIELD);
		Assert.assertTrue(messages.get(2).getMessageType() == MessageType.HAVE);
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 120, 120));
		
		final PwpHandshakeMessage actualHandshakeMessage = (PwpHandshakeMessage)messages.get(0);
		
		//Validate HANDSHAKE message contents
		Assert.assertTrue(Arrays.equals(infoHash, actualHandshakeMessage.getInfoHash().getBytes()));
		Assert.assertEquals("Deluge 5.4.3.2", actualHandshakeMessage.getPeerId());
		
		final PwpRegularMessage actualBitfieldMessage = (PwpRegularMessage)messages.get(1);
		
		//Validate BITFIELD message contents
		Assert.assertEquals(777L, ByteBuffer.wrap(actualBitfieldMessage.getPayload()).getLong());
		
		final PwpRegularMessage actualHaveMessage = (PwpRegularMessage)messages.get(2);
		
		//Validate HAVE message contents
		Assert.assertEquals(1976, ByteBuffer.wrap(actualHaveMessage.getPayload()).getInt());
	}
	
	//Parse fully contained handshake + bitfield + have, as three separate reads
	@Test
	public void testHandshakeBitfieldHavePartiallyContainedThreeReads() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(120);
		
		final byte[] reservedBytes = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};						
		final byte[] infoHash = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
				7, 8, 9, 0};

		//Put HANDSHAKE message
		buffer.put((byte)protocolName.length());
		buffer.put(protocolName.getBytes("UTF-8"));
		buffer.put(reservedBytes);
		buffer.put(infoHash);
		buffer.put(peerId);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HANDSHAKE);
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 120, 120));
		
		final PwpHandshakeMessage actualHandshakeMessage = (PwpHandshakeMessage)messages.get(0);
		
		//Validate HANDSHAKE message contents
		Assert.assertTrue(Arrays.equals(infoHash, actualHandshakeMessage.getInfoHash().getBytes()));
		Assert.assertEquals("Deluge 5.4.3.2", actualHandshakeMessage.getPeerId());
		
		//Put BITFIELD message (length(bitfield) == 32) 
		buffer.putInt(9);
		buffer.put((byte)5);
		buffer.putLong(777);
		
		messages.clear();
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.BITFIELD);
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 120, 120));
		
		final PwpRegularMessage actualBitfieldMessage = (PwpRegularMessage)messages.get(0);
		
		//Validate BITFIELD message contents
		Assert.assertEquals(777L, ByteBuffer.wrap(actualBitfieldMessage.getPayload()).getLong());
		
		//Put HAVE message
		buffer.putInt(5);
		buffer.put((byte)4);
		buffer.putInt(1976);
		
		messages.clear();
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.HAVE);
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 120, 120));
		
		final PwpRegularMessage actualHaveMessage = (PwpRegularMessage)messages.get(0);
		
		//Validate HAVE message contents
		Assert.assertEquals(1976, ByteBuffer.wrap(actualHaveMessage.getPayload()).getInt());
	}
	
	//Parse valid message(s), mixed with invalid message(s)
	@Test(expected = InvalidPeerMessageException.class)
	public void testMixedValidAndInvalidMessages() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(50);
		
		//Put PIECE message (length(block)) == 4 bytes
		buffer.putInt(13);
		buffer.put((byte)7);
		buffer.putInt(3981);
		buffer.putInt(0);
		buffer.putInt(999);
		
		//Put an invalid message
		final byte[] invalidMessage = {0, 0, 0, 1, 12};		
		buffer.put(invalidMessage);
		
		//Put CANCEL message		
		buffer.putInt(13);
		buffer.put((byte)8);
		buffer.putInt(3981);
		buffer.putInt(0);
		buffer.putInt(999);
		
		unitUnderTest.read(buffer);
	}
	
	//Parse fully contained regular messages, last 4 bytes consist of keep_alive message, buffer empty afterwards
	@Test
	public void testRegularMessagesFullyContainedLastKeepAliveEmptyBuffer() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(9);
		
		//Put CHOKE message
		buffer.putInt(1);
		buffer.put((byte)0);		
		
		//Put KEEP_ALIVE message
		buffer.putInt(0);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertEquals(2, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.CHOKE);
		Assert.assertTrue(messages.get(1).getMessageType() == MessageType.KEEP_ALIVE);
		
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 9, 9));
	}
	
	//Parse partially contained bitfield message spread over two buffer reads
	@Test
	public void testBitfieldPartiallyContainedTwoReads() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(20);
		
		//Put BITFIELD message (length(bitfield) == 32) 
		buffer.putInt(9);
		buffer.put((byte)5);
		buffer.putInt(777);
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 9, 13, 4));
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		
		Assert.assertEquals(9, copyOfBackup.getInt());
		Assert.assertEquals(5, copyOfBackup.get());
		Assert.assertEquals(777, copyOfBackup.getInt());
		
		buffer.putInt(908);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.BITFIELD);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		
		final PwpRegularMessage actualMessage = (PwpRegularMessage)messages.get(0);		
		final ByteBuffer payload = ByteBuffer.wrap(actualMessage.getPayload());		
		
		//Verify BITFIELD message contents
		Assert.assertEquals(777, payload.getInt());
		Assert.assertEquals(908, payload.getInt());
	}
	
	//Parse partially contained bitfield message spread over three buffer reads
	@Test
	public void testBitfieldPartiallyContainedThreeReads() throws Exception {
		final ClientSession unitUnderTest = new ClientSession(null, peer);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(20);
		
		//Put partial BITFIELD message (length(bitfield) == 32) 
		buffer.putInt(9);		
		
		final List<PwpMessage> messages = unitUnderTest.read(buffer);
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 4, 20, 16));
		
		buffer.put((byte)5);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertTrue(messages.isEmpty());
		Assert.assertNotNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		Assert.assertTrue(verifyBufferState(unitUnderTest.backupReaderBuffer, 5, 13, 8));
		
		final ByteBuffer copyOfBackup = unitUnderTest.backupReaderBuffer.duplicate();
		copyOfBackup.flip();
		
		Assert.assertEquals(9, copyOfBackup.getInt());
		Assert.assertEquals(5, copyOfBackup.get());
		
		buffer.putInt(777);
		buffer.putInt(908);
		
		messages.addAll(unitUnderTest.read(buffer));
		
		Assert.assertEquals(1, messages.size());
		Assert.assertTrue(messages.get(0).getMessageType() == MessageType.BITFIELD);
		Assert.assertNull(unitUnderTest.backupReaderBuffer);
		Assert.assertTrue(verifyBufferState(buffer, 0, 20, 20));
		
		final PwpRegularMessage actualMessage = (PwpRegularMessage)messages.get(0);		
		final ByteBuffer payload = ByteBuffer.wrap(actualMessage.getPayload());		
		
		//Verify BITFIELD message contents
		Assert.assertEquals(777, payload.getInt());
		Assert.assertEquals(908, payload.getInt());
	}
	
	private boolean verifyBufferState(final ByteBuffer buffer, final int expectedPosition, final int expectedLimit,
			final int expectedRemaining) {
		return buffer.position() == expectedPosition && buffer.limit() == expectedLimit &&
				buffer.remaining() == expectedRemaining;
	}
}