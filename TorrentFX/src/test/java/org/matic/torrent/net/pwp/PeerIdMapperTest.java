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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public final class PeerIdMapperTest {
	
	private final byte[] emptyBytes = new byte[12];

	@Test
	public void testParseKnownAzureusPeerId() throws Exception {
		final byte[] peerId = {'-', 'A', 'Z','3', '1', '0', '6', '-'};
		final String mappedPeerId = PeerIdMapper.mapId(buildCompletePeerId(peerId));
		Assert.assertEquals("Azureus 3.1.0.6", mappedPeerId);
	}
	
	@Test
	public void testParseUnknownAzureusPeerId() throws Exception {
		final byte[] peerId = {'-', 'X', 'Y','3', '1', '0', '6', '-'};
		final String mappedPeerId = PeerIdMapper.mapId(buildCompletePeerId(peerId));
		Assert.assertEquals("unknown client", mappedPeerId);
	}
	
	@Test
	public void testParseInvalidAzureusPeerId() throws Exception {
		final byte[] peerId = {'+', 'U', 'T','3', '1', '0', '6', '.'};
		final String mappedPeerId = PeerIdMapper.mapId(buildCompletePeerId(peerId));
		Assert.assertEquals("unknown client", mappedPeerId);
	}
	
	@Test
	public void testParseMinimumVersionLengthAzureusPeerId() throws Exception {
		final byte[] peerId = {'-', 'D', 'E','3', '0', '0', '0', '-'};
		final String mappedPeerId = PeerIdMapper.mapId(buildCompletePeerId(peerId));
		Assert.assertEquals("Deluge 3", mappedPeerId);
	}
	
	private byte[] buildCompletePeerId(final byte[] peerId) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(peerId);
		baos.write(emptyBytes);
		return baos.toByteArray();
	}
}