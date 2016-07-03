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
package org.matic.torrent.gui.model;

import org.matic.torrent.tracking.TrackableSession;

public final class PeerExchangeView extends TrackableView {

private static final String NAME = "[Peer Exchange]";
	
	public PeerExchangeView(final TrackableSession trackableSession) {
		super(trackableSession);
	}

	@Override
	public void update() {
        super.setStatus("inactive");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isUserManaged() {
		return false;
	}
}