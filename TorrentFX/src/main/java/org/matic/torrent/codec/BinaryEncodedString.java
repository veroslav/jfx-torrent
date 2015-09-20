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

package org.matic.torrent.codec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A byte string, encoded as <length>:<contents>. The length is encoded in base 10,
 * but must be non-negative (zero is allowed), while the String contents itself are
 * UTF-8 encoded.
 * 
 * @author vedran
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
	
	public final byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}
	
	public final String getValue() {
		return value.toString();
	}
	
	public final int getLength() {
		return value.length();
	}
	
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
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
	public final String toString() {
		return value;
	}

	@Override
	public final int compareTo(final BinaryEncodedString other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toExportableValue() {
		final StringBuilder result = new StringBuilder();
		result.append(value.length());
		result.append(SEPARATOR_TOKEN);
		result.append(value);
		
		return result.toString();
	}	
}
