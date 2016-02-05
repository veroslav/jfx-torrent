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

package org.matic.torrent.gui.custom;

import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.preferences.CssProperties;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * A container for a {@link Node} with a title displayed on the surrounding border
 * 
 * @author Vedran Matic
 *
 */
public final class TitledBorderPane extends StackPane {
	
	public static final String PRIMARY_BORDER_COLOR_STYLE = "-fx-background-color:-fx-base;";
	public static final String SECONDARY_BORDER_COLOR_STYLE = "-fx-background-color:-fx-background;";

	private final ObjectProperty<Pos>  titleAlignment = new SimpleObjectProperty<>();
	
	/**
	 * Create a titled container for a given {@link Node}
	 * 
	 * @param title Surrounding border's title
	 * @param contentNode Pane contents
	 * @param style Border properties, such as spacing and padding
	 * @param titleColorStyle Title label background color
	 */
	public TitledBorderPane(final String title, final Node contentNode,
			final BorderStyle style, final String titleColorStyle) {
	    final Label titleLabel = new Label();
	    titleLabel.setText(title + " ");
	    titleLabel.getStyleClass().add(CssProperties.BORDER_TITLE);	
	    titleLabel.setStyle(titleColorStyle);

	    titleAlignment.addListener(obs -> StackPane.setAlignment(titleLabel, titleAlignment.get()));

	    final StackPane contentPane = new StackPane();

	    getStyleClass().add("titled-border-" + style.name().toLowerCase());
	    getChildren().addAll(titleLabel, contentPane);
	    contentNode.getStyleClass().add("titled-border-" + style.name().toLowerCase() + "-content");
	    contentPane.getChildren().setAll(contentNode);
	    
	    titleAlignment.set(Pos.TOP_LEFT);	   
	  }
}