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
*/

package org.matic.torrent.codec;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

/**
 * Immutable representation of a torrent's info dictionary SHA-1 hash.
 * The purpose of this class is to encapsulate the info hash bytes and
 * avoid passing and copying it when needed.
 * 
 * @author vedran
 *
 */
public final class InfoHash {
	
	private final String hexInfoHash;
	private final byte[] infoHash;

	public InfoHash(final byte[] infoHashBytes) {
		this.infoHash = Arrays.copyOf(infoHashBytes, infoHashBytes.length);
		hexInfoHash = DatatypeConverter.printHexBinary(infoHashBytes);
	}
	
	public InfoHash(final String hexInfoHash) {
		this.hexInfoHash = hexInfoHash;
		infoHash = DatatypeConverter.parseHexBinary(hexInfoHash);
	}
	
	public String getHexValue() {
		return hexInfoHash;
	}
	
	public byte[] getBytes() {
		return Arrays.copyOf(infoHash, infoHash.length);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hexInfoHash == null) ? 0 : hexInfoHash.hashCode());
		result = prime * result + Arrays.hashCode(infoHash);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InfoHash other = (InfoHash) obj;
		if (hexInfoHash == null) {
			if (other.hexInfoHash != null)
				return false;
		} else if (!hexInfoHash.equals(other.hexInfoHash))
			return false;
		if (!Arrays.equals(infoHash, other.infoHash))
			return false;
		return true;
	}
}
