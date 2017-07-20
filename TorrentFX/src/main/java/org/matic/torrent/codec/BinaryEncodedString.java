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
package org.matic.torrent.codec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A byte string, encoded as <length>:<contents>. The length is encoded in base 10,
 * but must be non-negative (zero is allowed), while the String contents itself are
 * UTF-8 encoded.
 * 
 * @author Vedran Matic
 *
 */
public final class BinaryEncodedString implements BinaryEncodable, Comparable<BinaryEncodedString> {	
	protected static final char SEPARATOR_TOKEN = ':';
	
	private final String value;
	private final byte[] bytes;

	public BinaryEncodedString(final byte[] value) {
		this.bytes = Arrays.copyOf(value, value.length);
		this.value = new String(value, StandardCharsets.UTF_8);
	}
	
	public BinaryEncodedString(final String value) {
		this(value.getBytes(StandardCharsets.UTF_8));
	}
	
	public byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	public byte[] getBytes(final int offset, final int length) {
	    final byte[] bytesSegment = new byte[length];
	    System.arraycopy(bytes, offset, bytesSegment, 0, length);
	    return bytesSegment;
    }
	
	public String getValue() {
		return value.toString();
	}
	
	public int getLength() {
		return bytes.length;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		BinaryEncodedString other = (BinaryEncodedString) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int compareTo(final BinaryEncodedString other) {
		return value.compareTo(other.value);
	}

	/**
	 * @see BinaryEncodable#toExportableValue()
	 */
	@Override
	public byte[] toExportableValue() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);
		
		dos.write(String.valueOf(bytes.length).getBytes(StandardCharsets.UTF_8));
		dos.write(SEPARATOR_TOKEN);
		dos.write(bytes);		
		dos.flush();
		
		return baos.toByteArray();
	}	
}