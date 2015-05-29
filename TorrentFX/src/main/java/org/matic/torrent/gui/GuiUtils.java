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

package org.matic.torrent.gui;

import org.controlsfx.tools.Borders;
import org.controlsfx.tools.Borders.EtchedBorders;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public final class GuiUtils {
	
	public enum BorderType {
		OPTIONS_WINDOW_BORDER, ADD_NEW_TORRENT_BORDER
	}

	public static Node applyBorder(final Node targetNode, final String title, BorderType borderType) {		
		EtchedBorders border = Borders.wrap(targetNode).etchedBorder().title(title).highlight(Color.LIGHTGRAY);
		
		switch(borderType) {
		case OPTIONS_WINDOW_BORDER:
			border = border.outerPadding(10, 0, 0, 0).innerPadding(8, 10, 5, 8);
			break;
		case ADD_NEW_TORRENT_BORDER:
			border = border.outerPadding(10, 0, 10, 0).innerPadding(13, 10, 5, 8);
			break;
		}
		
		
		return border.buildAll();		
	}
	
	public static Insets noPadding() {
		return new Insets(0);
	}
}