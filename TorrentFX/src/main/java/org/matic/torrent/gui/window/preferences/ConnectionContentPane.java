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
package org.matic.torrent.gui.window.preferences;

import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.preferences.NetworkProperties;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class ConnectionContentPane extends CategoryContentPane {
	
	private static final String CONNECTION_CONTENT_PANE_NAME = "Connection";
	
	private final TextField inConnectionPortField = new TextField();	
	private final Button randomInConnectionButton = new Button("Random port");
	
	private final CheckBox randomPortEachStartCheck = new CheckBox("Randomize port each start");
	private final CheckBox upnpPortMappingCheck = new CheckBox("Enable UPnP port mapping");
	
	public ConnectionContentPane(final BooleanProperty preferencesChanged) {
		super(CONNECTION_CONTENT_PANE_NAME, preferencesChanged);
		initComponents(preferencesChanged);
	}

	@Override
	public void onSaveContentChanges() {
		if(preferencesChanged.get()) {
			//Apply changed values
			ApplicationPreferences.setProperty(
					NetworkProperties.INCOMING_CONNECTION_PORT, inConnectionPortField.getText());
			ApplicationPreferences.setProperty(NetworkProperties.RANDOMIZE_CONNECTION_PORT,
					randomPortEachStartCheck.isSelected());
			ApplicationPreferences.setProperty(NetworkProperties.ENABLE_UPNP_PORT_MAPPING,
					upnpPortMappingCheck.isSelected());
		}
	}

	@Override
	protected Node build() {
		final VBox content = new VBox();
		content.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		content.getChildren().addAll(
				new TitledBorderPane("Listening Port", buildPortSettingsPane(), BorderStyle.COMPACT,
						TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE));
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		return contentScroll;
	}

	protected void initComponents(final BooleanProperty preferencesChanged) {
		inConnectionPortField.setMaxWidth(80);
		
		applyValues();
		addListeners(preferencesChanged);
	}
	
	private void applyValues() {
		inConnectionPortField.setText(String.valueOf(ApplicationPreferences.getProperty(
				NetworkProperties.INCOMING_CONNECTION_PORT, NetworkProperties.DEFAULT_INCOMING_CONNECTION_PORT)));
		final boolean randomizePortOnStart = ApplicationPreferences.getProperty(
				NetworkProperties.RANDOMIZE_CONNECTION_PORT, false);
		randomPortEachStartCheck.setSelected(randomizePortOnStart);
		final boolean upnpPortMappingEnabled = ApplicationPreferences.getProperty(
				NetworkProperties.ENABLE_UPNP_PORT_MAPPING, false);
		upnpPortMappingCheck.setSelected(upnpPortMappingEnabled);
	}
	
	private void addListeners(final BooleanProperty preferencesChanged) {
		inConnectionPortField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		randomPortEachStartCheck.setOnAction(e -> preferencesChanged.set(true));
		upnpPortMappingCheck.setOnAction(e -> preferencesChanged.set(true));
	}
	
	private Node buildPortSettingsPane() {
		final GridPane portSettingsPane = super.buildGridPane();				
		portSettingsPane.add(new Label("Port used for incoming connections: "), 0, 0);
		
		final HBox inPortInputPane = new HBox(inConnectionPortField, randomInConnectionButton);		
		inPortInputPane.setAlignment(Pos.CENTER_RIGHT);
		inPortInputPane.setSpacing(10);
		
		portSettingsPane.add(inPortInputPane, 1, 0);
		portSettingsPane.add(upnpPortMappingCheck, 0, 1);
		portSettingsPane.add(randomPortEachStartCheck, 1, 1);
		
		return portSettingsPane;
	}
}