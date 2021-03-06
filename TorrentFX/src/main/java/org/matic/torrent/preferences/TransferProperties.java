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
package org.matic.torrent.preferences;

public final class TransferProperties {

	//Torrent addition options
	public static final String START_DOWNLOADS_AUTOMATICALLY = "transfer.download.auto";

    public static final String ACTIVE_TORRENTS_LIMIT = "transfer.max.torrents.active";
    public static final String DOWNLOADING_TORRENTS_LIMIT = "transfer.max.torrents.downloading";
    public static final String UPLOADING_TORRENTS_LIMIT = "transfer.max.torrents.uploading";

    public static final int DEFAULT_ACTIVE_TORRENTS_LIMIT = 5;
    public static final int DEFAULT_DOWNLOADING_TORRENTS_LIMIT = 3;
    public static final int DEFAULT_UPLOADING_TORRENTS_LIMIT = 3;
}