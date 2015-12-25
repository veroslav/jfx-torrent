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

import org.matic.torrent.gui.action.enums.ApplicationTheme;
import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class UiExtrasContentPane extends CategoryContentPane {

	private static final String UI_EXTRAS_CONTENT_PANE_NAME = "UI Settings";
	
	private final ComboBox<ApplicationTheme> themeOptionsComboBox = new ComboBox<>();
	private final Scene scene;
	
	protected UiExtrasContentPane(final BooleanProperty preferencesChanged, final Scene scene) {
		super(UI_EXTRAS_CONTENT_PANE_NAME, preferencesChanged);
		this.scene = scene;
		initComponents(preferencesChanged);
	}

	@Override
	public void onSaveContentChanges() {
		if(preferencesChanged.get()) {			
			final String targetTheme = themeOptionsComboBox.getSelectionModel().getSelectedItem().name().toLowerCase();
			final String targetUiStyle = GuiProperties.THEME_STYLESHEET_PATH_TEMPLATE.replace("?", targetTheme)
					+ GuiProperties.THEME_UI_STYLE_CSS;
						
			if(!scene.getStylesheets().contains(targetUiStyle)) {
				scene.getStylesheets().removeIf(s -> s.startsWith("/themes/"));
				scene.getStylesheets().add(targetUiStyle);
				ApplicationPreferences.setProperty(GuiProperties.APPLICATION_THEME, targetTheme);
			}					
		}
	}

	@Override
	public Node build() {
		return buildUiExtrasOptionsView();
	}
	
	private void initComponents(final BooleanProperty preferencesChanged) {
		themeOptionsComboBox.setItems(FXCollections.observableArrayList(ApplicationTheme.values()));		
		setComboBoxActions(preferencesChanged);
	}
	
	private void setComboBoxActions(final BooleanProperty preferencesChanged) {
		themeOptionsComboBox.setOnAction(e -> preferencesChanged.set(true));
		
		final ApplicationTheme applicationTheme = 
				ApplicationTheme.valueOf(ApplicationPreferences.getProperty(
				GuiProperties.APPLICATION_THEME, ApplicationTheme.LIGHT.name()).toUpperCase());
		themeOptionsComboBox.getSelectionModel().select(applicationTheme);
	}
	
	private Node buildUiExtrasOptionsView() {
		final TitledBorderPane themeOptions = new TitledBorderPane(
				"Theme Options", buildThemeOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		
		final VBox content = new VBox();
		content.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		content.getChildren().addAll(themeOptions);
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		
		return contentScroll;
	}
	
	private Node buildThemeOptionsPane() {
		final ColumnConstraints firstColumn = new ColumnConstraints();	    
		firstColumn.setPercentWidth(40);
		
		final GridPane themeOptionsPane = new GridPane();
		themeOptionsPane.getColumnConstraints().addAll(firstColumn);
		themeOptionsPane.setVgap(10);
		
		themeOptionsPane.add(new Label("Theme: "), 0, 0);
		themeOptionsPane.add(themeOptionsComboBox, 1, 0);
		
		themeOptionsComboBox.setMaxWidth(Double.POSITIVE_INFINITY);
		GridPane.setHgrow(themeOptionsComboBox, Priority.ALWAYS);
		
		return themeOptionsPane;
	}
}