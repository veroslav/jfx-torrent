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

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

public final class GuiUtils {
	
	//Table column options
	public static final String LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME = "left-aligned-column-header";
	public static final String RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME = "right-aligned-column-header";
	
	public static final int NAME_COLUMN_PREFERRED_SIZE = 230;
	
	//Padding options
	private static final Insets RIGHT_PADDING_INSETS = new Insets(0, 5, 0, 0);
	private static final Insets LEFT_PADDING_INSETS = new Insets(0, 0, 0, 5);
	private static final Insets NO_PADDING_INSETS = new Insets(0);
	
	public static Insets noPadding() {
		return NO_PADDING_INSETS;
	}
	
	public static Node getEmptyTablePlaceholder() {
		final HBox placeholder = new HBox();
		placeholder.getStyleClass().add("empty-placeholder");
		return placeholder;		
	}
	
	public static Insets leftPadding() {
		return LEFT_PADDING_INSETS;
	}
	
	public static Insets rightPadding() {
		return RIGHT_PADDING_INSETS;
	}
}