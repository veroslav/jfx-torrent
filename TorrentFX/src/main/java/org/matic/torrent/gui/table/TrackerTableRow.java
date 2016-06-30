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
package org.matic.torrent.gui.table;

import javafx.scene.control.TableRow;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;

public final class TrackerTableRow<T> extends TableRow<T> {

	@Override
	protected void updateItem(final T item, final boolean empty) {
		super.updateItem(item, empty);
		
		if(!empty) {
            if(this.getIndex() % 2 != 0 && ApplicationPreferences.getProperty(
                    GuiProperties.ALTERNATE_LIST_ROW_COLOR, false)) {
                getStyleClass().removeAll(CssProperties.ALTERNATE_LIST_ROW_EVEN);
                getStyleClass().add(CssProperties.ALTERNATE_LIST_ROW_ODD);
            }
            else {
                getStyleClass().removeAll(CssProperties.ALTERNATE_LIST_ROW_ODD);
                getStyleClass().add(CssProperties.ALTERNATE_LIST_ROW_EVEN);
            }
		}
	}	
}