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
package org.matic.torrent.codec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * An integer encoded as i<number in base 10 notation>e. Leading zeros are not allowed.
 * Negative values are encoded by prefixing the number with a minus sign.
 * 
 * @author vedran
 *
 */
public final class BinaryEncodedInteger implements BinaryEncodable {
	
	protected static final char BEGIN_TOKEN = 'i';
	protected static final char END_TOKEN = 'e';

	private final long value;
	
	public BinaryEncodedInteger(final long value) {
		this.value = value;
	}
	
	public long getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
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
		BinaryEncodedInteger other = (BinaryEncodedInteger) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	/**
	 * @see BinaryEncodable#toExportableValue()
	 */
	@Override
	public byte[] toExportableValue() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);
		
		dos.write(BEGIN_TOKEN);
		dos.write(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
		dos.write(END_TOKEN);
		
		dos.flush();
		
		return baos.toByteArray();
	}
}