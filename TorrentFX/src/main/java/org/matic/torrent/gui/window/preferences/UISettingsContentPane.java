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

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.GuiUtils.BorderType;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.preferences.GuiProperties.DownloadingTorrentClickAction;
import org.matic.torrent.preferences.GuiProperties.SeedingTorrentClickAction;
import org.matic.torrent.preferences.TransferProperties;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UISettingsContentPane implements CategoryContentPane {
	
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
	private final CheckBox minimizeToTrayCheck = new CheckBox("Minimize button minimizes application to tray");
	private final CheckBox showBalloonNotificationCheck = new CheckBox("Show balloon notification in tray");
	private final CheckBox closeToTrayCheck = new CheckBox("Close button closes application to tray");	
	private final CheckBox openOnTrayClickCheck = new CheckBox("Single click on tray icon to open");
	private final CheckBox activateOnTrayClickCheck = new CheckBox("Always activate when clicked");
	private final CheckBox showTrayIconCheck = new CheckBox("Always show tray icon");
	
	//Torrent addition options
	private final CheckBox showChangeNameAndLocationCheck = new CheckBox(
			"Show options to change the name and location of the torrent data");
	private final CheckBox dontDownloadAutomaticallyCheck = new CheckBox("Don't start the download automatically");
	private final CheckBox activateProgramWindowCheck = new CheckBox("Activate the program window");
	
	//Double Click action options
	private final ComboBox<DownloadingTorrentClickAction> downloadingTorrentOptionsComboBox = new ComboBox<>();
	private final ComboBox<SeedingTorrentClickAction> seedingTorrentOptionsComboBox = new ComboBox<>();
	
	private final BooleanProperty preferencesChanged = new SimpleBooleanProperty(false);
	
	private final ScrollPane contentPane;
	
	public UISettingsContentPane(final BooleanProperty preferencesChanged) {
		this.preferencesChanged.bind(preferencesChanged);
		initComponents(preferencesChanged);
		contentPane = buildUISettingsOptionsView();
	}

	@Override
	public final void onSaveContentChanges() {
		if(preferencesChanged.get()) {
			//Save check box values
			ApplicationPreferences.setProperty(GuiProperties.DELETE_TORRENT_CONFIRMATION,
					confirmTorrentDeleteCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.DELETE_TRACKER_CONFIRMATION,
					confirmTrackerDeleteCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.EXIT_CONFIRMATION,
					confirmOnExitCheck.isSelected());
			ApplicationPreferences.setProperty(GuiProperties.ALTERNATE_LIST_ROW_COLOR,
					alternateListColorCheck.isSelected());
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

	@Override
	public final ScrollPane getContentPane() {
		return contentPane;
	}

	@Override
	public final String getName() {
		return UI_SETTINGS_CONTENT_PANE_NAME;
	}
	
	private void initComponents(final BooleanProperty preferencesChanged) {
		downloadingTorrentOptionsComboBox.setItems(
				FXCollections.observableArrayList(DownloadingTorrentClickAction.values()));
		downloadingTorrentOptionsComboBox.getSelectionModel().select(0);
		
		seedingTorrentOptionsComboBox.setItems(
				FXCollections.observableArrayList(SeedingTorrentClickAction.values()));
		seedingTorrentOptionsComboBox.getSelectionModel().select(0);
		
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
		confirmTorrentDeleteCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmTrackerDeleteCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmOnExitCheck.setOnAction(e -> preferencesChanged.set(true));
		alternateListColorCheck.setOnAction(e -> preferencesChanged.set(true));
		speedInTitleBarCheck.setOnAction(e -> preferencesChanged.set(true));
		speedLimitsInStatusBarCheck.setOnAction(e -> preferencesChanged.set(true));
		confirmCriticalSeederExitCheck.setOnAction(e -> preferencesChanged.set(true));
		minimizeToTrayCheck.setOnAction(e -> preferencesChanged.set(true));
		showTrayIconCheck.setOnAction(e -> preferencesChanged.set(true));
		closeToTrayCheck.setOnAction(e -> preferencesChanged.set(true));
		openOnTrayClickCheck.setOnAction(e -> preferencesChanged.set(true));
		showBalloonNotificationCheck.setOnAction(e -> preferencesChanged.set(true));
		activateOnTrayClickCheck.setOnAction(e -> preferencesChanged.set(true));
		dontDownloadAutomaticallyCheck.setOnAction(e -> preferencesChanged.set(true));
		activateProgramWindowCheck.setOnAction(e -> preferencesChanged.set(true));
		showChangeNameAndLocationCheck.setOnAction(e -> preferencesChanged.set(true));
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
	
	private ScrollPane buildUISettingsOptionsView() {
		final Node displayOptions = GuiUtils.applyBorder(buildDisplayOptionsPane(),
				"Display Options", BorderType.DEFAULT_WINDOW_BORDER);
		final Node systemTrayOptions = GuiUtils.applyBorder(buildSystemTrayOptionsPane(),
				"System Tray", BorderType.DEFAULT_WINDOW_BORDER);
		final Node torrentAdditionOptions = GuiUtils.applyBorder(buildOnTorrentAdditionOptionsPane(),
				"When Adding Torrents", BorderType.DEFAULT_WINDOW_BORDER);
		final Node doubleClickActionsOptions = GuiUtils.applyBorder(buildDoubleClickActionsOptionPane(),
				"Actions for Double Click", BorderType.DEFAULT_WINDOW_BORDER);
		
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
	
	private VBox buildDisplayOptionsPane() {
		final VBox displayOptionsPane = new VBox();
		displayOptionsPane.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		displayOptionsPane.getChildren().addAll(confirmTorrentDeleteCheck, confirmTrackerDeleteCheck,
				confirmOnExitCheck, alternateListColorCheck, speedInTitleBarCheck,
				speedLimitsInStatusBarCheck, confirmCriticalSeederExitCheck);
		
		return displayOptionsPane;
	}
	
	private VBox buildSystemTrayOptionsPane() {
		final VBox systemTrayOptionsPane = new VBox();
		systemTrayOptionsPane.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		systemTrayOptionsPane.getChildren().addAll(minimizeToTrayCheck, showTrayIconCheck,
				closeToTrayCheck, openOnTrayClickCheck, showBalloonNotificationCheck,
				activateOnTrayClickCheck);
		
		return systemTrayOptionsPane;
	}
	
	private VBox buildOnTorrentAdditionOptionsPane() {
		final VBox torrentAdditionOptionsPane = new VBox();
		torrentAdditionOptionsPane.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		torrentAdditionOptionsPane.getChildren().addAll(dontDownloadAutomaticallyCheck,
				activateProgramWindowCheck, showChangeNameAndLocationCheck);
		
		return torrentAdditionOptionsPane;
	}
	
	private VBox buildDoubleClickActionsOptionPane() {			
		final Label seedingTorrentsLabel = new Label("For seeding torrents: ");
		final HBox seedingTorrentsOptions = new HBox();
		
		seedingTorrentsOptions.getChildren().addAll(seedingTorrentsLabel, seedingTorrentOptionsComboBox);		
		seedingTorrentsOptions.setAlignment(Pos.CENTER_LEFT);
		
		final Label downloadingTorrentsLabel = new Label("For downloading torrents: ");
		final HBox downloadingTorrentsOptions = new HBox();
		
		downloadingTorrentsOptions.getChildren().addAll(downloadingTorrentsLabel, downloadingTorrentOptionsComboBox);
		downloadingTorrentsOptions.setAlignment(Pos.CENTER_LEFT);
		
		final VBox doubleClickOptionsPane = new VBox();
		doubleClickOptionsPane.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		doubleClickOptionsPane.getChildren().addAll(seedingTorrentsOptions, downloadingTorrentsOptions);
		
		return doubleClickOptionsPane;
	}
}