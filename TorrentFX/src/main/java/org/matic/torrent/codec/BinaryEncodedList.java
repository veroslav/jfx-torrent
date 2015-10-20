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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A list of values is encoded as l<contents>e . The contents consist of
 * the bencoded elements of the list, in order, concatenated.
 * 
 * @author vedran
 *
 */
public final class BinaryEncodedList implements BinaryEncodable {
	
	protected static final char BEGIN_TOKEN = 'l';
	protected static final char END_TOKEN = 'e';
	
	private final List<BinaryEncodable> list;
	
	public BinaryEncodedList() {
		list = new ArrayList<>();
	}
	
	public final void add(final BinaryEncodable element) {
		list.add(element);
	}
	
	public final BinaryEncodable get(final int index) {
		return list.get(index);
	}
	
	public final int size() {
		return list.size();
	}
	
	public Stream<BinaryEncodable> stream() {
		return list.stream();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list == null) ? 0 : list.hashCode());
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
		BinaryEncodedList other = (BinaryEncodedList) obj;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return list.toString();
	}

	/**
	 * @see BinaryEncodable#toExportableValue()
	 */
	@Override
	public byte[] toExportableValue() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);		
		
		dos.write(BEGIN_TOKEN);		
		for(final BinaryEncodable element : list) {
			dos.write(element.toExportableValue());
		}
		dos.write(END_TOKEN);
		dos.flush();
		
		return baos.toByteArray();
	}	
}