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

package org.matic.torrent.gui.window.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

/**
 * Common pane for all category panes in the preferences window
 * 
 * @author vedran
 *
 */
public abstract class CategoryContentPane {
	
	protected final BooleanProperty preferencesChanged = new SimpleBooleanProperty(false);
	private final String paneName;
	
	protected CategoryContentPane(final String paneName, final BooleanProperty preferencesChanged) {
		this.preferencesChanged.bind(preferencesChanged);
		this.paneName = paneName;
	}

	/**
	 * Save all changes made by user, if any, since last content save
	 */
	public abstract void onSaveContentChanges();

	/**
	 * Build a pane that holds all of the content for this category
	 * 
	 * @return
	 */
	protected abstract Node build();
	
	/**
	 * Return this category name (used as the category title)
	 * 
	 * @return
	 */
	public String getName() {
		return paneName;
	}
	
	protected GridPane buildGridPane() {
		final ColumnConstraints firstColumn = new ColumnConstraints();
		final ColumnConstraints secondColumn = new ColumnConstraints();
	    
		firstColumn.setPercentWidth(50);	     
	    secondColumn.setPercentWidth(50);
	    
	    final GridPane gridPane = new GridPane();
	    gridPane.getColumnConstraints().addAll(firstColumn, secondColumn);
		gridPane.setVgap(10);
		
		return gridPane;
	}
}