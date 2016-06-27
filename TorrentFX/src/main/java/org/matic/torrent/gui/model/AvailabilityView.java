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

import java.util.BitSet;

public final class AvailabilityView {
	
	private final BitSet downloadedPieces;
	private final int totalPieces;	

	public AvailabilityView(final int totalPieces) {
		this.totalPieces = totalPieces;
		downloadedPieces = new BitSet(this.totalPieces);
	}

	public int getLastHaveIndex() {
		return downloadedPieces.length();
	}

	public int getTotalPieces() {
		return totalPieces;
	}

	public void setHave(final int pieceIndex, final boolean have) {
		downloadedPieces.set(pieceIndex, have);
	}
	
	public boolean getHave(final int pieceIndex) {
		return downloadedPieces.get(pieceIndex);
	}

	public int getHavePiecesCount() {
		return downloadedPieces.cardinality();
	}		
}