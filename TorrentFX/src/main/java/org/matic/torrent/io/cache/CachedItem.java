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
package org.matic.torrent.io.cache;

/**
 * An object (often a data piece that is part of a torrent) that can be cached.
 *
 * @author Vedran Matic
 * @param <T> Type of the object that is cached
 */
public final class CachedItem<T> implements Comparable<CachedItem<T>> {

    private final T item;

    private final long cacheEntryTime;

    public CachedItem(final T item) {
        this.item = item;

        cacheEntryTime = System.currentTimeMillis();
    }

    public T getItem() {
        return item;
    }

    @Override
    public int compareTo(final CachedItem<T> other) {
        return Long.compare(this.cacheEntryTime, other.cacheEntryTime);
    }
}