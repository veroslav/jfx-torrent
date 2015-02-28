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

import java.util.HashMap;
import java.util.Map;

/**
 * A dictionary encoded as d<contents>e. The elements of the dictionary are
 * encoded each key immediately followed by its value. All keys must be byte
 * strings and must appear in lexicographical order.
 * 
 * @author vedran
 *
 */
public final class BinaryEncodedDictionary implements BinaryEncodable {
	
	protected static final char BEGIN_TOKEN = 'd';
	protected static final char END_TOKEN = 'e';

	private final Map<BinaryEncodedString, BinaryEncodable> map;
	
	public BinaryEncodedDictionary() {
		map = new HashMap<>();
	}
	
	public final void put(final BinaryEncodedString key, final BinaryEncodable value) {
		map.put(key, value);
	}
	
	public final BinaryEncodable get(final BinaryEncodedString key) {
		return map.get(key);
	}
	
	public final int size() {
		return map.size();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
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
		BinaryEncodedDictionary other = (BinaryEncodedDictionary) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "BinaryEncodedDictionary [map=" + map + "]";
	}	
}
