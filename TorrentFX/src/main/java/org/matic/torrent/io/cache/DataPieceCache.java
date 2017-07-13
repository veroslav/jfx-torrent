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

import java.util.TreeMap;

/**
 * A simple cache for generic kind of items (mostly DataPiece:s).
 *
 * @param <T> The key that uniquely identifies each cached item
 * @param <U> The type of cached items
 */
public final class DataPieceCache<T,U> {

    private TreeMap<T, CachedItem<U>> cache;
    private int maxSize;

    public DataPieceCache(final int maxSize) {
        cache = new TreeMap<>();
        this.maxSize = maxSize;
    }

    //TODO: Can we remove synchronization?
    public synchronized void setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized U getItem(final T itemKey) {
        return cache.get(itemKey).getItem();
    }

    public synchronized void addItem(final T itemKey, final U item) {
        if(cache.size() == maxSize) {
            cache.remove(cache.lastEntry());
        }
        cache.put(itemKey, new CachedItem<>(item));
    }
}