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
import org.matic.torrent.gui.action.enums.DownloadingTorrentClickAction;
import org.matic.torrent.gui.action.enums.SeedingTorrentClickAction;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.preferences.TransferProperties;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class UiSettingsContentPane extends CategoryContentPane {
	
	private static final String UI_SETTINGS_CONTENT_PANE_NAME = "UI Settings";
		
	//Display options
	private final CheckBox speedLimitsInStatusBarCheck = new CheckBox("Show speed limits in the status bar");
	private final CheckBox confirmCriticalSeederExitCheck = new CheckBox("Confirm exit if critical seeder");
	private final CheckBox confirmTorrentDeleteCheck = new CheckBox("Confirm when deleting torrents");
	private final CheckBox confirmTrackerDeleteCheck = new CheckBox("Confirm when deleting trackers");
	private final CheckBox speedInTitleBarCheck = new CheckBox("Show current speed in the title bar");	
	private final CheckBox alternateListColorCheck = new CheckBox("Alternate list background color");	
	private final CheckBox confirmOnExitCheck = new CheckBox("Show confirmation dialog on exit");
	
	//System Tray options
	private final CheckBox showBalloonNotificationCheck = new CheckBox("Show balloon notifications in tray");
	private final CheckBox openOnTrayClickCheck = new CheckBox("Single click on tray icon to open");
	private final CheckBox minimizeToTrayCheck = new CheckBox("Minimize button minimizes to tray");			
	private final CheckBox activateOnTrayClickCheck = new CheckBox("Always activate when clicked");
	private final CheckBox closeToTrayCheck = new CheckBox("Close button closes to tray");
	private final CheckBox showTrayIconCheck = new CheckBox("Always show tray icon");
	
	//Torrent addition options	
	private final CheckBox dontDownloadAutomaticallyCheck = new CheckBox("Don't start the download automatically");
	private final CheckBox activateProgramWindowCheck = new CheckBox("Activate the program window");
	private final CheckBox showChangeNameAndLocationCheck = new CheckBox(
			"Show options to change the name and location of the torrent data");
	
	//Double Click action options
	private final ComboBox<DownloadingTorrentClickAction> downloadingTorrentOptionsComboBox = new ComboBox<>();
	private final ComboBox<SeedingTorrentClickAction> seedingTorrentOptionsComboBox = new ComboBox<>();
	
	public UiSettingsContentPane(final BooleanProperty preferencesChanged) {
		super(UI_SETTINGS_CONTENT_PANE_NAME, preferencesChanged);
		initComponents(preferencesChanged);
	}
	
	@Override
	protected Node build() {
		return buildUISettingsOptionsView();
	}

	@Override
	public void onSaveContentChanges() {
		if(preferencesChanged.get()) {
			//Apply changed values
			final boolean alternateListRowColorEnabled = ApplicationPreferences.getProperty(
					GuiProperties.ALTERNATE_LIST_ROW_COLOR, false);
			if(alternateListRowColorEnabled != alternateListColorCheck.isSelected()) {
				ApplicationPreferences.setProperty(GuiProperties.ALTERNATE_LIST_ROW_COLOR,
						alternateListColorCheck.isSelected());
			}
			
			//Save check box values
			ApplicationPreferences.setProperty(GuiProperties.DELETE_TORRENT_CONFIRMATION,
					confirmTorrentDeleteCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.DELETE_TRACKER_CONFIRMATION,
					confirmTrackerDeleteCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.EXIT_CONFIRMATION,
					confirmOnExitCheck.isSelected());			
			ApplicationPreferences.setProperty(GuiProperties.SHOW_SPEED_IN_TITLEBAR,
					speedInTitleBarCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.SHOW_SPEED_LIMITS_IN_STATUSBAR,
					speedLimitsInStatusBarCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.EXIT_CONFIRMATION_CRITICAL_SEEDER,
					confirmCriticalSeederExitCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.MINIMIZE_APPLICATION_TO_TRAY,
					minimizeToTrayCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.ALWAYS_SHOW_TRAY,
					showTrayIconCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.CLOSE_APPLICATION_TO_TRAY,
					closeToTrayCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.SINGLE_CLICK_ON_TRAY_TO_OPEN,
					openOnTrayClickCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.BALLOON_NOTIFICATIONS_ON_TRAY,
					showBalloonNotificationCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.ACTIVATE_ON_TRAY_CLICK,
					activateOnTrayClickCheck.isSelected());
			ApplicationPreferences.setProperty(TransferProperties.START_DOWNLOADS_AUTOMATICALLY,
					!dontDownloadAutomaticallyCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.ACTIVATE_WINDOW_ON_TORRENT_ADDITION,
					activateProgramWindowCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.ALLOW_NAME_AND_LOCATION_CHANGE,
					showChangeNameAndLocationCheck.isSelected());
			
			//Save combo box values
			ApplicationPreferences.setProperty(GuiProperties.CLICK_ON_DOWNLOADING_TORRENT_ACTION,
					downloadingTorrentOptionsComboBox.getSelectionModel().getSelectedItem().name());
			ApplicationPreferences.setProperty(GuiProperties.CLICK_ON_SEEDING_TORRENT_ACTION,
					seedingTorrentOptionsComboBox.getSelectionModel().getSelectedItem().name());			
		}
	}

	private void initComponents(final BooleanProperty preferencesChanged) {
		downloadingTorrentOptionsComboBox.setItems(
				FXCollections.observableArrayList(DownloadingTorrentClickAction.values()));
		
		seedingTorrentOptionsComboBox.setItems(
				FXCollections.observableArrayList(SeedingTorrentClickAction.values()));
		
		setComboBoxActions(preferencesChanged);
		setCheckBoxActions(preferencesChanged);
		applyCheckBoxValues();		
	}
	
	private void setComboBoxActions(final BooleanProperty preferencesChanged) {
		downloadingTorrentOptionsComboBox.setOnAction(e -> preferencesChanged.set(true));
		final DownloadingTorrentClickAction downloadingTorrentOnClickAction = 
				DownloadingTorrentClickAction.valueOf(ApplicationPreferences.getProperty(
				GuiProperties.CLICK_ON_DOWNLOADING_TORRENT_ACTION,
				DownloadingTorrentClickAction.SHOW_PROPERTIES.name()));
		downloadingTorrentOptionsComboBox.getSelectionModel().select(downloadingTorrentOnClickAction);
		
		seedingTorrentOptionsComboBox.setOnAction(e -> preferencesChanged.set(true));
		final SeedingTorrentClickAction seedingTorrentOnClickAction = 
				SeedingTorrentClickAction.valueOf(ApplicationPreferences.getProperty(
				GuiProperties.CLICK_ON_SEEDING_TORRENT_ACTION,
				SeedingTorrentClickAction.OPEN_FOLDER.name()));
		seedingTorrentOptionsComboBox.getSelectionModel().select(seedingTorrentOnClickAction);
	}
	
	private void setCheckBoxActions(final BooleanProperty preferencesChanged) {
		confirmCriticalSeederExitCheck.setOnAction(e -> preferencesChanged.set(true));
		dontDownloadAutomaticallyCheck.setOnAction(e -> preferencesChanged.set(true));
		showChangeNameAndLocationCheck.setOnAction(e -> preferencesChanged.set(true));
		showBalloonNotificationCheck.setOnAction(e -> preferencesChanged.set(true));
		speedLimitsInStatusBarCheck.setOnAction(e -> preferencesChanged.set(true));
		activateProgramWindowCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmTorrentDeleteCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmTrackerDeleteCheck.setOnAction(e -> preferencesChanged.set(true));
		activateOnTrayClickCheck.setOnAction(e -> preferencesChanged.set(true));
		alternateListColorCheck.setOnAction(e -> preferencesChanged.set(true));
		speedInTitleBarCheck.setOnAction(e -> preferencesChanged.set(true));
		openOnTrayClickCheck.setOnAction(e -> preferencesChanged.set(true));
		minimizeToTrayCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmOnExitCheck.setOnAction(e -> preferencesChanged.set(true));		
		showTrayIconCheck.setOnAction(e -> preferencesChanged.set(true));
		closeToTrayCheck.setOnAction(e -> preferencesChanged.set(true));		
	}
	
	private void applyCheckBoxValues() {
		final boolean showChangeNameAndLocationSet = ApplicationPreferences.getProperty(
				GuiProperties.ALLOW_NAME_AND_LOCATION_CHANGE, true);
		showChangeNameAndLocationCheck.setSelected(showChangeNameAndLocationSet);
		
		final boolean activateProgramWindowSet = ApplicationPreferences.getProperty(
				GuiProperties.ACTIVATE_WINDOW_ON_TORRENT_ADDITION, true);
		activateProgramWindowCheck.setSelected(activateProgramWindowSet);
		
		final boolean dontDownloadAutomaticallySet = ApplicationPreferences.getProperty(
				TransferProperties.START_DOWNLOADS_AUTOMATICALLY, true);
		dontDownloadAutomaticallyCheck.setSelected(!dontDownloadAutomaticallySet);
		
		final boolean activateOnTrayClickCheckSet = ApplicationPreferences.getProperty(
				GuiProperties.ACTIVATE_ON_TRAY_CLICK, true);
		activateOnTrayClickCheck.setSelected(activateOnTrayClickCheckSet);
		
		final boolean showBalloonNotificationSet = ApplicationPreferences.getProperty(
				GuiProperties.BALLOON_NOTIFICATIONS_ON_TRAY, true);
		showBalloonNotificationCheck.setSelected(showBalloonNotificationSet);
		
		final boolean openOnTrayClickSet = ApplicationPreferences.getProperty(
				GuiProperties.SINGLE_CLICK_ON_TRAY_TO_OPEN, false);
		openOnTrayClickCheck.setSelected(openOnTrayClickSet);
		
		final boolean closeToTraySet = ApplicationPreferences.getProperty(
				GuiProperties.CLOSE_APPLICATION_TO_TRAY, true);
		closeToTrayCheck.setSelected(closeToTraySet);
		
		final boolean showTrayIconSet = ApplicationPreferences.getProperty(
				GuiProperties.ALWAYS_SHOW_TRAY, true);
		showTrayIconCheck.setSelected(showTrayIconSet);
		
		final boolean minimizeToTraySet = ApplicationPreferences.getProperty(
				GuiProperties.MINIMIZE_APPLICATION_TO_TRAY, false);
		minimizeToTrayCheck.setSelected(minimizeToTraySet);
		
		final boolean confirmCriticalSeederExitSet = ApplicationPreferences.getProperty(
				GuiProperties.EXIT_CONFIRMATION_CRITICAL_SEEDER	, true);
		confirmCriticalSeederExitCheck.setSelected(confirmCriticalSeederExitSet);
		
		final boolean speedLimitsInStatusBarSet = ApplicationPreferences.getProperty(
				GuiProperties.SHOW_SPEED_LIMITS_IN_STATUSBAR, false);
		speedLimitsInStatusBarCheck.setSelected(speedLimitsInStatusBarSet);
		
		final boolean confirmTorrentDeleteSet = ApplicationPreferences.getProperty(
				GuiProperties.DELETE_TORRENT_CONFIRMATION, true);
		confirmTorrentDeleteCheck.setSelected(confirmTorrentDeleteSet);
		
		final boolean confirmTrackerDeleteSet = ApplicationPreferences.getProperty(
				GuiProperties.DELETE_TRACKER_CONFIRMATION, false);
		confirmTrackerDeleteCheck.setSelected(confirmTrackerDeleteSet);
		
		final boolean confirmOnExitSet = ApplicationPreferences.getProperty(
				GuiProperties.EXIT_CONFIRMATION, true);
		confirmOnExitCheck.setSelected(confirmOnExitSet);
		
		final boolean alternateListColorSet = ApplicationPreferences.getProperty(
				GuiProperties.ALTERNATE_LIST_ROW_COLOR, false);
		alternateListColorCheck.setSelected(alternateListColorSet);
		
		final boolean speedInTitleBarSet = ApplicationPreferences.getProperty(
				GuiProperties.SHOW_SPEED_IN_TITLEBAR, false);
		speedInTitleBarCheck.setSelected(speedInTitleBarSet);
	}
	
	private Node buildUISettingsOptionsView() {
		final TitledBorderPane displayOptions = new TitledBorderPane(
				"Display Options", buildDisplayOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		final TitledBorderPane systemTrayOptions = new TitledBorderPane(
				"System Tray", buildSystemTrayOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		final TitledBorderPane torrentAdditionOptions = new TitledBorderPane(
				"When Adding Torrents", buildOnTorrentAdditionOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		final TitledBorderPane doubleClickActionsOptions = new TitledBorderPane(
				"Actions for Double Click", buildDoubleClickActionsOptionPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		
		final VBox content = new VBox();
		content.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		content.getChildren().addAll(displayOptions, systemTrayOptions, torrentAdditionOptions,
				doubleClickActionsOptions);
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		
		return contentScroll;
	}
	
	private Node buildDisplayOptionsPane() {		
	    final GridPane displayOptionsPane = buildGridPane();	    
		displayOptionsPane.add(confirmTorrentDeleteCheck, 0, 0);
		displayOptionsPane.add(confirmTrackerDeleteCheck, 1, 0);
		displayOptionsPane.add(confirmOnExitCheck, 0, 1);
		displayOptionsPane.add(alternateListColorCheck, 1, 1);
		displayOptionsPane.add(speedInTitleBarCheck, 0, 2);
		displayOptionsPane.add(speedLimitsInStatusBarCheck, 1, 2);
		displayOptionsPane.add(confirmCriticalSeederExitCheck, 0, 3, 2, 1);
		
		return displayOptionsPane;
	}
	
	private Node buildSystemTrayOptionsPane() {		
		final GridPane systemTrayOptionsPane = buildGridPane();
		systemTrayOptionsPane.add(minimizeToTrayCheck, 0, 0);
		systemTrayOptionsPane.add(showTrayIconCheck, 1, 0);
		systemTrayOptionsPane.add(closeToTrayCheck, 0, 1);
		systemTrayOptionsPane.add(openOnTrayClickCheck, 1, 1);
		systemTrayOptionsPane.add(showBalloonNotificationCheck, 0, 2);
		systemTrayOptionsPane.add(activateOnTrayClickCheck, 1, 2);
		
		return systemTrayOptionsPane;
	}
	
	private Node buildOnTorrentAdditionOptionsPane() {
		final GridPane torrentAdditionOptionsPane = buildGridPane();		
		torrentAdditionOptionsPane.add(dontDownloadAutomaticallyCheck, 0, 0);
		torrentAdditionOptionsPane.add(activateProgramWindowCheck, 1, 0);
		torrentAdditionOptionsPane.add(showChangeNameAndLocationCheck, 0, 1, 2, 1);
				
		return torrentAdditionOptionsPane;
	}
	
	private Node buildDoubleClickActionsOptionPane() {		
		final ColumnConstraints firstColumn = new ColumnConstraints();	    
		firstColumn.setPercentWidth(40);
		
		final GridPane doubleClickOptionsPane = new GridPane();
		doubleClickOptionsPane.getColumnConstraints().addAll(firstColumn);
		doubleClickOptionsPane.setVgap(10);
		
		doubleClickOptionsPane.add(new Label("For seeding torrents: "), 0, 0);
		doubleClickOptionsPane.add(seedingTorrentOptionsComboBox, 1, 0);
		doubleClickOptionsPane.add(new Label("For downloading torrents: "), 0, 1);
		doubleClickOptionsPane.add(downloadingTorrentOptionsComboBox, 1, 1);
		
		seedingTorrentOptionsComboBox.setMaxWidth(Double.POSITIVE_INFINITY);
		downloadingTorrentOptionsComboBox.setMaxWidth(Double.POSITIVE_INFINITY);
		
		GridPane.setHgrow(seedingTorrentOptionsComboBox, Priority.ALWAYS);
		GridPane.setHgrow(downloadingTorrentOptionsComboBox, Priority.ALWAYS);
		
		return doubleClickOptionsPane;
	}
}