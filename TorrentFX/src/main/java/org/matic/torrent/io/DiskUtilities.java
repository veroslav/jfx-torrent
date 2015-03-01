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

package org.matic.torrent.io;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiskUtilities {

	/**
	 * Calculate available space for a disk/partition containing the
	 * specified path on that disk/partition.
	 * 
	 * @param pathOnDisk Target path on the disk/partition
	 * @return Available disk space in bytes
	 * @throws IOException If available disk space can't be calculated 
	 */
	public static long getAvailableDiskSpace(final Path pathOnDisk) throws IOException {		
		final FileStore fileStore = Files.getFileStore(pathOnDisk);
		return fileStore.getUsableSpace() / 1024;		
	}
}
