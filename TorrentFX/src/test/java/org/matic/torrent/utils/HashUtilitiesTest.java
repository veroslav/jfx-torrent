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

package org.matic.torrent.utils;

import org.junit.Assert;
import org.junit.Test;
import org.matic.torrent.hash.HashUtilities;
import org.matic.torrent.peer.ClientProperties;

import javax.xml.bind.DatatypeConverter;

public final class HashUtilitiesTest {

	@Test
	public final void testConvertToHexValue() {		
		final byte[] inputBytes = "This is your message".getBytes(ClientProperties.STRING_ENCODING_CHARSET);
		final String expectedHexValue = "5468697320697320796F7572206D657373616765";
		
		Assert.assertEquals(expectedHexValue, DatatypeConverter.printHexBinary(inputBytes));
	}
	
	@Test
	public final void testValidHexNumberUpperCase() {
		final String hexNumber = "0123456789ABCDEF0123456789ABCDEF012345678";
		Assert.assertTrue(HashUtilities.isValidHexNumber(hexNumber)); 
	}
	
	@Test
	public final void testValidHexNumberMixedCase() {
		final String hexNumber = "0123456789AbCdEf0123456789aBcDeF012345678";
		Assert.assertTrue(HashUtilities.isValidHexNumber(hexNumber));
	}
	
	@Test
	public final void testInvalidHexNumberEmpty() {
		final String hexNumber = "";
		Assert.assertFalse(HashUtilities.isValidHexNumber(hexNumber));
	}
	
	@Test
	public final void testInvalidHexInvalidCharacter() {
		final String hexNumber = "0123456789AbCdEf0123456789aBcheF012345678";
		Assert.assertFalse(HashUtilities.isValidHexNumber(hexNumber));
	}
}