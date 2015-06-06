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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;

import org.junit.Before;
import org.junit.Test;
import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.codec.BinaryDecoderException;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeyNames;

public final class BinaryDecoderTest {
	
	private BinaryDecoder unitUnderTest;

	@Before
	public final void setup() {
		unitUnderTest = new BinaryDecoder();
	}

	@Test
	public final void testCopyInfoDictionaryBytes() throws Exception {
		final String infoDictionaryContents = "d6:lengthi4550373376e4:name13:file_name.doce";
		final String metaData = "d8:announce20:http://mytracker.now13:announce-listl18:http://onemore.nowe"
				+ "4:info" + infoDictionaryContents + "e";
		
		final BinaryEncodedDictionary dictionary = unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
		
		final MessageDigest messageDigest = MessageDigest.getInstance(BinaryDecoder.HASH_ALGORITHM);
		
		final byte[] expectedInfoHash = messageDigest.digest(infoDictionaryContents.getBytes());
		final byte[] extractedInfoHash = ((BinaryEncodedString)dictionary.get(BinaryEncodingKeyNames.KEY_INFO_HASH)).getBytes();
		
		assertArrayEquals(expectedInfoHash, extractedInfoHash);
	}

	@Test
	public final void testDecodeSimpleDictionary() throws Exception {
		final String metaData = "d5:key_17:value_15:key_27:value_2e";
		
		final BinaryEncodedDictionary contents = unitUnderTest
				.decode(new ByteArrayInputStream(metaData.getBytes()));
		
		assertEquals(2, contents.size());
		assertEquals("value_1", contents.get(new BinaryEncodedString("key_1".getBytes())).toString());
		assertEquals("value_2", contents.get(new BinaryEncodedString("key_2".getBytes())).toString());
	}

	@Test
	public final void testDecodeSimpleList() throws Exception {
		final String metaData = "d4:listl14:string_elementi42el13:one_more_listed3:key5:valueeee";
		
		final BinaryEncodedDictionary contents = unitUnderTest
				.decode(new ByteArrayInputStream(metaData.getBytes()));
		
		final BinaryEncodedList list = (BinaryEncodedList)contents.get(
				new BinaryEncodedString("list".getBytes()));
		final BinaryEncodedList insideList = (BinaryEncodedList)list.get(2);
		final BinaryEncodedDictionary insideDictionary = (BinaryEncodedDictionary)list.get(3);
		
		assertEquals(4, list.size());
		assertEquals(1, insideList.size());
		assertEquals("string_element", list.get(0).toString());
		assertEquals(42L, ((BinaryEncodedInteger)list.get(1)).getValue());
		assertEquals("one_more_list", ((BinaryEncodedString)insideList.get(0)).toString());
		assertEquals("value", insideDictionary.get(new BinaryEncodedString(
				"key".getBytes())).toString());
	}

	@Test
	public final void testDecodeSimpleByteString() throws Exception {
		final byte[] bytes = "d8:announce15:http://test.com6:pieces3:".getBytes();
		final byte[] rawString = { 76, 50, 11 };
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(bytes);
		baos.write(rawString);
		baos.write("e".getBytes());
		
		final BinaryEncodedDictionary contents = unitUnderTest.decode(
				new ByteArrayInputStream(baos.toByteArray()));
		
		final byte[] extractedRawString = ((BinaryEncodedString)(contents.get(
				new BinaryEncodedString("pieces".getBytes())))).getBytes();
		
		assertEquals(rawString.length, extractedRawString.length);
		
		for (int i = 0; i < extractedRawString.length; ++i) {
			assertEquals(rawString[i], extractedRawString[i]);
		}
		
		assertEquals("http://test.com", contents.get(new BinaryEncodedString(
				"announce".getBytes())).toString());
	}

	@Test
	public final void testDecodeInteger() throws Exception {
		final String metaData = "d8:integersli42ei-12ei0eee";
		
		final BinaryEncodedDictionary contents = unitUnderTest.decode(
				new ByteArrayInputStream(metaData.getBytes()));
		final BinaryEncodedList list = (BinaryEncodedList)contents.get(
				new BinaryEncodedString("integers".getBytes()));
		
		assertEquals(42L, ((BinaryEncodedInteger)list.get(0)).getValue());
		assertEquals(-12L, ((BinaryEncodedInteger)list.get(1)).getValue());
		assertEquals(0L, ((BinaryEncodedInteger)list.get(2)).getValue());
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testInvalidDictionaryKey() throws Exception {
		final String metaData = "dl7:elemente5:valuee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testMissingDictionaryEnd() throws Exception {
		final String metaData = "d4:listli3ee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testInvalidInteger() throws Exception {
		final String metaData = "d4:listli3reee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testMissingIntegerEnd() throws Exception {
		final String metaData = "d4:listli3l7:elementeee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testNonIntegerStringLength() throws Exception {
		final String metaData = "d3:keyz:valuee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test(expected = BinaryDecoderException.class)
	public final void testIncorrectStringLength() throws Exception {
		final String metaData = "d18:key5:valuee";
		unitUnderTest.decode(new ByteArrayInputStream(metaData.getBytes()));
	}

	@Test
	public final void testStringEncoding() throws Exception {
		final String metaData = "d8:encoding6:UTF-166:pieces6:blablae";
		
		final BinaryEncodedDictionary contents = unitUnderTest.decode(
				new ByteArrayInputStream(metaData.getBytes()));
		
		final BinaryEncodedString encodingValue = (BinaryEncodedString)(contents.get(
				new BinaryEncodedString("encoding".getBytes())));
		final BinaryEncodedString piecesValue = (BinaryEncodedString)(contents.get(
				new BinaryEncodedString("pieces".getBytes())));
		
		assertEquals("UTF-8", encodingValue.getEncoding());
		assertEquals("UTF-16", piecesValue.getEncoding());
	}
}
