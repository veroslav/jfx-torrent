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

import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

public final class UnitConverterTest {

	@Test
	public final void testFormatByteCount() {
		final long unit = 1024;
		final long byteCount = 678;
		final long kiloByteCount = byteCount * unit;
		final long megaByteCount = kiloByteCount * unit;
		final long gigaByteCount = megaByteCount * unit;
		
		Assert.assertEquals("678 B", UnitConverter.formatByteCount(byteCount));
		Assert.assertTrue(UnitConverter.formatByteCount(kiloByteCount).matches("678[\\.|,]0 KiB"));
		Assert.assertTrue(UnitConverter.formatByteCount(megaByteCount).matches("678[\\.|,]0 MiB"));
		Assert.assertTrue(UnitConverter.formatByteCount(gigaByteCount).matches("678[\\.|,]0 GiB"));
	}
	
	@Test
	public final void testFormatTime() {
		final long timeToConvert = 969695100000L; 
		final String expectedConvertedTime = "2000-09-23 07:45:00";
		
		Assert.assertEquals(expectedConvertedTime, UnitConverter.formatMillisToDate(timeToConvert,
				TimeZone.getTimeZone(UnitConverter.UTC_TIMEZONE)));
	}
}