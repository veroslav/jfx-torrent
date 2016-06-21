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

package org.matic.torrent.gui.window;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.PathProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A window shown when the user chooses to add tracker(s)
 * 
 * @author vedran
 *
 */
public class AddTrackerWindow {
	
	private static final String WINDOW_TITLE = "Add Trackers";
	
	private final TextArea trackerInputArea;
	
	private final Dialog<ButtonType> window;

	public AddTrackerWindow(final Window owner) {
		window = new Dialog<>();
		window.initOwner(owner);
		
		trackerInputArea = new TextArea();		
		initComponents();
	}
	
	public final List<String> showAndWait() {
		final List<String> result = new ArrayList<>();	
		final Optional<ButtonType> choice = window.showAndWait();
		
		if(choice.isPresent() && choice.get() == ButtonType.OK) {
			final String lineSeparator = System.getProperty(PathProperties.LINE_SEPARATOR);
			result.addAll(Arrays.asList(trackerInputArea.getText().split(lineSeparator)));
		}
		
		return result;
	}
	
	private void initComponents() {		
		window.setTitle(WINDOW_TITLE);		
		window.setResizable(true);
		
		final DialogPane windowPane = window.getDialogPane(); 
		windowPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		final Button okButton = (Button)windowPane.lookupButton(ButtonType.OK);
		trackerInputArea.textProperty().addListener((obs, oldV, newV) -> okButton.setDisable(newV.isEmpty()));
		
		final ScrollPane inputAreaScroll = new ScrollPane(trackerInputArea);		
		inputAreaScroll.setFitToHeight(true);
		inputAreaScroll.setFitToWidth(true);
		
		final TitledBorderPane titledTrackerPane = new TitledBorderPane("List of trackers to add",
				inputAreaScroll, BorderStyle.COMPACT, TitledBorderPane.PRIMARY_BORDER_COLOR_STYLE);
		
		final Pane trackerInputPane = new Pane(titledTrackerPane);		
		titledTrackerPane.prefWidthProperty().bind(trackerInputPane.widthProperty());
		titledTrackerPane.prefHeightProperty().bind(trackerInputPane.heightProperty());
		
		windowPane.setContent(trackerInputPane);
	}
}
