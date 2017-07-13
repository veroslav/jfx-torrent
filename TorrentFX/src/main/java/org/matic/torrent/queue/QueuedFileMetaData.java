/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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
package org.matic.torrent.queue;

import java.nio.file.Path;

/**
 * This class contains useful info about a file that is part of a torrent.
 *
 * @author Vedran Matic
 */
public final class QueuedFileMetaData {

    private final Path path;
    private final long length;
    private final long offset;

    /**
     * Create a new file meta data instance.
     *
     * @param path Path to this file relative to its containing torrent
     * @param length File length (in bytes)
     * @param offset Byte offset within its containing torrent
     */
    public QueuedFileMetaData(final Path path, final long length, final long offset) {
        this.path = path;
        this.length = length;
        this.offset = offset;
    }

    public Path getPath() {
        return path;
    }

    public long getLength() {
        return length;
    }

    public long getOffset() {
        return offset;
    }
}