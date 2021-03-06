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

import java.util.Arrays;

/**
 * A message sent between the client and remote peers.
 * 
 * @author Vedran Matic
 *
 */
public class PwpMessage {
	
	public enum MessageType {
		KEEP_ALIVE, INTERESTED, NOT_INTERESTED, CHOKE, UNCHOKE, REQUEST, HAVE, PIECE,
		PORT, BITFIELD, HANDSHAKE, CANCEL
	}
	
	private static final MessageType[] MESSAGE_TYPE_MAPPINGS = {MessageType.CHOKE, MessageType.UNCHOKE,
		MessageType.INTERESTED, MessageType.NOT_INTERESTED, MessageType.HAVE, MessageType.BITFIELD,
		MessageType.REQUEST, MessageType.PIECE, MessageType.CANCEL, MessageType.PORT};

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    private final byte[] payload;

	private final MessageType messageType;
	
	public static MessageType fromMessageId(final int messageId) throws InvalidPeerMessageException {
		if(messageId < 0 || messageId >= PwpMessage.MESSAGE_TYPE_MAPPINGS.length) {
			throw new InvalidPeerMessageException("Message id: " + messageId + 
					" is not mappable to any known message types");
		}
		return PwpMessage.MESSAGE_TYPE_MAPPINGS[messageId];
	}

	public PwpMessage(final MessageType messageType) {
	    this(messageType, EMPTY_PAYLOAD);
    }
	
	public PwpMessage(final MessageType messageType, final byte[] payload) {
		this.payload = payload;
	    this.messageType = messageType;
	}

	public MessageType getMessageType() {
		return messageType;
	}

    public byte[] getPayload() {
        //TODO: System.arrayCopy on payload
        return payload;
    }

    @Override
    public String toString() {
        return "PwpMessage{" +
                "payload=" + Arrays.toString(payload) +
                ", messageType=" + messageType +
                '}';
    }
}