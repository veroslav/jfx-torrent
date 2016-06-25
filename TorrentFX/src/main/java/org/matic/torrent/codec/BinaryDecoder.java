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

import org.matic.torrent.exception.BinaryDecoderException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

/**
 * A parser and decoder for binary encoded data types. Input can be read from 
 * a torrent meta file or directly from a (GZip compressed) stream. 
 * 
 * @author vedran
 *
 */
public final class BinaryDecoder {
	
	public static final String HASH_ALGORITHM = "SHA-1";
		
	private final MessageDigest messageDigest;
	
	public BinaryDecoder() {
		try {
			messageDigest = MessageDigest.getInstance(BinaryDecoder.HASH_ALGORITHM);
		} 
		catch (final NoSuchAlgorithmException nsae) {
			throw new RuntimeException("FATAL ERROR : SHA-1 hashing not supported.");
		}
	}
	
	public BinaryEncodedDictionary decode(final InputStream input)
			throws IOException, BinaryDecoderException {
		return decodeFromStream(new BufferedInputStream(input));
	}
	
	public BinaryEncodedDictionary decodeGzip(final InputStream input)
			throws IOException, BinaryDecoderException {
		return decodeFromStream(new BufferedInputStream(new GZIPInputStream(input)));
	}
	
	private BinaryEncodedDictionary decodeFromStream(final BufferedInputStream input) 
			throws IOException, BinaryDecoderException {
		final BinaryEncodedDictionary decodedDictionary = decodeDictionary(input, false);
		input.close();
		return decodedDictionary;
	}
	
	private BinaryEncodedDictionary decodeDictionary(final BufferedInputStream input, 
			final boolean copyDictionary) throws IOException, BinaryDecoderException {
		int currentByte = input.read();
		input.mark(Integer.MAX_VALUE);
		
		if(currentByte != BinaryEncodedDictionary.BEGIN_TOKEN) {
			throw new BinaryDecoderException("Attempted to decode an invalid dictionary.");
		}
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		final BinaryEncodedDictionary dictionary = new BinaryEncodedDictionary();
		currentByte = input.read();
		
		while(currentByte != BinaryEncodedDictionary.END_TOKEN) {
			input.reset();		
			final BinaryEncodedString key = decodeString(input, copyDictionary);		
			input.reset();
			
			BinaryEncodable value = null;
			
			if(BinaryEncodingKeys.KEY_INFO.equals(key)) {
				messageDigest.reset();
				value = decodeGenericType(input, true);
				final BinaryEncodedString infoHash = new BinaryEncodedString(messageDigest.digest());
				dictionary.put(BinaryEncodingKeys.KEY_INFO_HASH, infoHash);
			}
			else {
				value = decodeGenericType(input, copyDictionary);
			}
					
			dictionary.put(key, value);
			
			currentByte = input.read();
			if(currentByte == -1) {
				throw new BinaryDecoderException("Invalid dictionary: End of stream reached");
			}
		}
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		input.mark(Integer.MAX_VALUE);
		
		return dictionary;
	}
	
	private BinaryEncodable decodeGenericType(final BufferedInputStream input, final boolean copyDictionary) 
			throws IOException, BinaryDecoderException {
		input.mark(Integer.MAX_VALUE);
		
		BinaryEncodable element = null;		
		final char elementType = (char)input.read();
		
		switch(elementType) {
		case BinaryEncodedInteger.BEGIN_TOKEN:
			input.reset();
			element = decodeInteger(input, copyDictionary);
			break;
		case BinaryEncodedDictionary.BEGIN_TOKEN:
			input.reset();
			element = decodeDictionary(input, copyDictionary);
			break;
		case BinaryEncodedList.BEGIN_TOKEN:
			input.reset();
			element = decodeList(input, copyDictionary);
			break;
		default:
			input.reset();
			element = decodeString(input, copyDictionary);
		}
		
		return element;
	}
	
	private BinaryEncodedList decodeList(final BufferedInputStream input, final boolean copyDictionary) 
			throws IOException, BinaryDecoderException {
		final BinaryEncodedList list = new BinaryEncodedList();
		BinaryEncodable element = null;
		int currentByte = input.read();
		
		if(currentByte != BinaryEncodedList.BEGIN_TOKEN) {
			throw new BinaryDecoderException("Attempted to decode an invalid list.");
		}
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		input.mark(Integer.MAX_VALUE);
		
		currentByte = input.read();
		while(currentByte != BinaryEncodedList.END_TOKEN) {
			input.reset();
			element = decodeGenericType(input, copyDictionary);
			list.add(element);
			
			currentByte = input.read();
			if(currentByte == -1) {
				throw new BinaryDecoderException("Invalid list: End of stream reached");
			}
		}
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		input.mark(Integer.MAX_VALUE);
		
		return list;
	}
	
	private BinaryEncodedInteger decodeInteger(final BufferedInputStream input, final boolean copyDictionary) 
			throws IOException, BinaryDecoderException {
		final StringBuilder integerString = new StringBuilder();
		int currentByte = input.read();
		
		if(currentByte != BinaryEncodedInteger.BEGIN_TOKEN) {
			throw new BinaryDecoderException("Attempted to decode an invalid integer.");
		}
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		currentByte = input.read();
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		while(currentByte != BinaryEncodedInteger.END_TOKEN) {
			integerString.append((char)currentByte);
			currentByte = input.read();
		
			if(copyDictionary) {
				messageDigest.update((byte)currentByte);
			}
		
			if(currentByte == -1) {
				throw new BinaryDecoderException("Invalid integer: End of stream reached");
			}
		}
		
		BinaryEncodedInteger element = null;
		
		try {
			element = new BinaryEncodedInteger(Long.parseLong(integerString.toString()));
		}
		catch(final NumberFormatException nfe) {
			throw new BinaryDecoderException("Parsed value is not an integer: " +
					integerString);
		}
		
		input.mark(Integer.MAX_VALUE);
		
		return element;
	}
	
	private BinaryEncodedString decodeString(final BufferedInputStream input, boolean copyDictionary) 
			throws IOException, BinaryDecoderException {
		final StringBuilder length = new StringBuilder();
		
		int currentByte = input.read();
		
		if(copyDictionary) {
			messageDigest.update((byte)currentByte);
		}
		
		while(currentByte != BinaryEncodedString.SEPARATOR_TOKEN) {
			length.append((char)currentByte);
		
			currentByte = input.read();
			
			if(copyDictionary) {
				messageDigest.update((byte)currentByte);
			}
			if(currentByte == -1) {
				throw new BinaryDecoderException("Invalid string: End of stream reached");
			}
		}
		
		int lengthAsInt;
		try {
			lengthAsInt = Integer.parseInt(length.toString());
		}
		catch(final NumberFormatException nfe) {
			throw new BinaryDecoderException("Parsed string length is invalid: '"
					+ length.toString() + "'");
		}
		
		final byte[] rawValue = new byte[lengthAsInt];
		int currentPosition = 0;
		
		while(currentPosition < rawValue.length && (currentByte = input.read()) != -1) {
			rawValue[currentPosition++] = (byte)currentByte;
		}
		
		if(copyDictionary) {
			messageDigest.update(rawValue);
		}
		
		final BinaryEncodedString string = new BinaryEncodedString(rawValue);
		input.mark(Integer.MAX_VALUE);
		
		return string;
	}
}
