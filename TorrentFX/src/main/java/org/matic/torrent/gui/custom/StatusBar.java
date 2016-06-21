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

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.preferences.CssProperties;

/**
 * A bar that displays connectivity and client status
 * 
 * @author Vedran Matic
 *
 */
public class StatusBar extends HBox {
	
	private final Label label1 = new Label("");
	private final Label label2 = new Label("");
	private final Label dhtLabel = new Label("DHT: Waiting to login...");
	private final Label downloadLabel = new Label("D:? kiB/s T:? MiB");
	private final Label uploadLabel = new Label("U:? kiB/s T:? MiB");
	
	public StatusBar() {			
		this.setAlignment(Pos.CENTER_LEFT);
		this.initComponents();
	}
	
	private void initComponents() {		
		final GridPane gridPane = new GridPane();
		gridPane.setAlignment(Pos.CENTER);
		gridPane.setHgap(2);   
		
		final VBox label1Pane = new VBox(label1);
		label1Pane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		label1.setPadding(GuiUtils.leftPadding());
		
		final VBox label2Pane = new VBox(label2);
		label2Pane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		label2.setPadding(GuiUtils.leftPadding());
		
		final VBox dhtPane = new VBox(dhtLabel);
		dhtPane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		dhtLabel.setPadding(GuiUtils.leftPadding());
		
		final VBox downloadPane = new VBox(downloadLabel);
		downloadPane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		downloadLabel.setPadding(GuiUtils.leftPadding());
		
		final VBox uploadPane = new VBox(uploadLabel);
		uploadPane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		uploadLabel.setPadding(GuiUtils.leftPadding());
		
		final ColumnConstraints columnConstraints = new ColumnConstraints();
		columnConstraints.setPercentWidth(20);
		
		final ColumnConstraints firstColumnConstraints = new ColumnConstraints();
		firstColumnConstraints.setPercentWidth(40);
		
	    gridPane.getColumnConstraints().addAll(firstColumnConstraints, columnConstraints,
	    		columnConstraints, columnConstraints, columnConstraints);	   
	    
	    gridPane.add(label1Pane, 0, 0);
	    gridPane.add(label2Pane, 1, 0);
	    gridPane.add(dhtPane, 2, 0);
	    gridPane.add(downloadPane, 3, 0);
	    gridPane.add(uploadPane, 4, 0);	    
	    
	    this.getChildren().add(gridPane);	  	    
	    HBox.setHgrow(gridPane, Priority.ALWAYS);
	}
}