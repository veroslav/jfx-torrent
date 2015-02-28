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

package org.matic.torrent.io.codec;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * A byte string, encoded as <length>:<contents>. The length is encoded in base 10,
 * but must be non-negative (zero is allowed); the contents are just the bytes that
 * make up the string.
 * 
 * @author vedran
 *
 */
public final class BinaryEncodedString implements BinaryEncodable {
			
	protected static final String ENCODING_UTF8 = "UTF-8";
	protected static final char SEPARATOR_TOKEN = ':';
	
	private final byte[] value;
	private String encoding;
	
	public BinaryEncodedString(final byte[] value) {
		this(value, BinaryEncodedString.ENCODING_UTF8);
	}

	public BinaryEncodedString(final byte[] value, final String encoding) {
		this.value = Arrays.copyOf(value, value.length);
		this.encoding = encoding;
	}
	
	public final byte[] getBytes() {
		return Arrays.copyOf(value, value.length);
	}
	
	public final String getEncoding() {
		return encoding;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((encoding == null) ? 0 : encoding.hashCode());
		result = prime * result + Arrays.hashCode(value);
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
		if (encoding == null) {
			if (other.encoding != null)
				return false;
		} else if (!encoding.equals(other.encoding))
			return false;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		try {
			return new String(value, encoding);
		} 
		catch (final UnsupportedEncodingException uee) {
			return null;
		}
	}	
}
