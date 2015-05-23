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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.GuiUtils.BorderType;
import org.matic.torrent.gui.action.FileActionHandler;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.PathProperties;

public final class DirectoriesContentPane implements CategoryContentPane {
	
	private static final String DIRECTORIES_CONTENT_PANE_NAME = "Directories";

	private final TextField storeTorrentsDirectoryField = new TextField();
	private final TextField loadTorrentsDirectoryField = new TextField();	
	private final TextField moveTorrentsDirectoryField = new TextField();
	private final TextField completedDirectoryField = new TextField();
	private final TextField downloadDirectoryField = new TextField();	
	private final TextField keyStoreLocationField = new TextField();
		
	private final Button browseStoreTorrentsDirectoryButton = new Button("...");
	private final Button browseLoadTorrentsDirectoryButton = new Button("...");
	private final Button browseMoveTorrentsDirectoryButton = new Button("...");
	private final Button browseCompletedDirectoryButton = new Button("...");
	private final Button browseDownloadDirectoryButton = new Button("...");
	private final Button browseKeyStoreButton = new Button("...");
	
	private final CheckBox moveFromDefaultCheck = new CheckBox("Only move from the default download directory");
	private final CheckBox moveTorrentsCheck = new CheckBox("Move .torrents for finished jobs to:");
	private final CheckBox loadTorrentsCheck = new CheckBox("Automatically load .torrents from:");
	private final CheckBox completedDirectoryCheck = new CheckBox("Move completed downloads to:");
	private final CheckBox downloadsDirectoryCheck = new CheckBox("Put new downloads in:");
	private final CheckBox deleteTorrentsCheck = new CheckBox("Delete loaded .torrents");	
	private final CheckBox storeTorrentsCheck = new CheckBox("Store .torrents in:");			
	
	private final ScrollPane contentPane;
	
	private final BooleanProperty preferencesChanged = new SimpleBooleanProperty(false);
	private final FileActionHandler fileActionHandler;
	private final Window owner;
	
	public DirectoriesContentPane(final Window owner, final FileActionHandler fileActionHandler, 
			final BooleanProperty preferencesChanged) {
		this.owner = owner;
		this.fileActionHandler = fileActionHandler;
		this.preferencesChanged.bind(preferencesChanged);
		initComponents(preferencesChanged);
		contentPane = buildDirectoriesOptionsView();
	}
	
	@Override
	public final void onSaveContentChanges() {
		if(preferencesChanged.get()) {
			//Save path locations
			ApplicationPreferences.setProperty(PathProperties.COMPLETED_DOWNLOADS, completedDirectoryField.getText());
			ApplicationPreferences.setProperty(PathProperties.COMPLETED_TORRENTS, moveTorrentsDirectoryField.getText());
			ApplicationPreferences.setProperty(PathProperties.KEY_STORE, keyStoreLocationField.getText());
			ApplicationPreferences.setProperty(PathProperties.LOAD_TORRENTS, loadTorrentsDirectoryField.getText());
			ApplicationPreferences.setProperty(PathProperties.NEW_DOWNLOADS, downloadDirectoryField.getText());
			ApplicationPreferences.setProperty(PathProperties.NEW_TORRENTS, storeTorrentsDirectoryField.getText());
			
			//Save checkbox values
			ApplicationPreferences.setProperty(PathProperties.NEW_DOWNLOADS_SET, 
					downloadsDirectoryCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.COMPLETED_DOWNLOADS_SET, 
					completedDirectoryCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.NEW_TORRENTS_SET, 
					storeTorrentsCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.COMPLETED_TORRENTS_SET, 
					moveTorrentsCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.LOAD_TORRENTS_SET, 
					loadTorrentsCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.MOVE_COMPLETED_DOWNLOADS_FROM_DEFAULT_SET, 
					moveFromDefaultCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
			ApplicationPreferences.setProperty(PathProperties.DELETE_LOADED_TORRENTS_SET, 
					deleteTorrentsCheck.isSelected()? ApplicationPreferences.BOOLEAN_PROPERTY_SET : 
						ApplicationPreferences.BOOLEAN_PROPERTY_UNSET);
		}
	};
	
	@Override
	public final ScrollPane getContentPane() {
		return contentPane;
	}
	
	@Override
	public final String getName() {
		return DIRECTORIES_CONTENT_PANE_NAME;
	}
	
	private void initComponents(final BooleanProperty preferencesChanged) {
		loadTorrentsDirectoryField.setDisable(true);
		storeTorrentsDirectoryField.setDisable(true);
		moveTorrentsDirectoryField.setDisable(true);
		completedDirectoryField.setDisable(true);
		downloadDirectoryField.setDisable(true);		
		
		browseLoadTorrentsDirectoryButton.setDisable(true);
		browseStoreTorrentsDirectoryButton.setDisable(true);
		browseMoveTorrentsDirectoryButton.setDisable(true);
		browseCompletedDirectoryButton.setDisable(true);
		browseDownloadDirectoryButton.setDisable(true);
		
		moveFromDefaultCheck.setDisable(true);
		deleteTorrentsCheck.setDisable(true);
		
		completedDirectoryField.setText(ApplicationPreferences.getProperty(PathProperties.COMPLETED_DOWNLOADS, null));
		moveTorrentsDirectoryField.setText(ApplicationPreferences.getProperty(PathProperties.COMPLETED_TORRENTS, null));
		keyStoreLocationField.setText(ApplicationPreferences.getProperty(PathProperties.KEY_STORE, System.getProperty("user.home")));
		loadTorrentsDirectoryField.setText(ApplicationPreferences.getProperty(PathProperties.LOAD_TORRENTS, null));
		downloadDirectoryField.setText(ApplicationPreferences.getProperty(PathProperties.NEW_DOWNLOADS, null));
		storeTorrentsDirectoryField.setText(ApplicationPreferences.getProperty(PathProperties.NEW_TORRENTS, null));
				
		browseLoadTorrentsDirectoryButton.setOnAction(e -> onBrowseForPath(loadTorrentsDirectoryField));			
		browseStoreTorrentsDirectoryButton.setOnAction(e -> onBrowseForPath(storeTorrentsDirectoryField));		
		browseMoveTorrentsDirectoryButton.setOnAction(e -> onBrowseForPath(moveTorrentsDirectoryField));					
		browseCompletedDirectoryButton.setOnAction(e -> onBrowseForPath(completedDirectoryField));					
		browseDownloadDirectoryButton.setOnAction(e -> onBrowseForPath(downloadDirectoryField));		
		browseKeyStoreButton.setOnAction(e -> onBrowseForPath(keyStoreLocationField));			
		
		storeTorrentsDirectoryField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		loadTorrentsDirectoryField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		moveTorrentsDirectoryField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		completedDirectoryField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		downloadDirectoryField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		keyStoreLocationField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
		
		downloadsDirectoryCheck.selectedProperty().addListener((obs, oldV, newV) -> onDownloadsDirectoryChecked(newV));
		downloadsDirectoryCheck.setOnAction(e -> {
			preferencesChanged.set(true);
			onDownloadsDirectoryChecked(downloadsDirectoryCheck.isSelected());
			
		});
		completedDirectoryCheck.selectedProperty().addListener((obs, oldV, newV) -> onCompletedDirectoryChecked(newV));
		completedDirectoryCheck.setOnAction(e -> {
			preferencesChanged.set(true);
			onCompletedDirectoryChecked(completedDirectoryCheck.isSelected());
			
		});
		storeTorrentsCheck.selectedProperty().addListener((obs, oldV, newV) -> onStoreTorrentsChecked(newV));
		storeTorrentsCheck.setOnAction(e -> {
			preferencesChanged.set(true);
			onStoreTorrentsChecked(storeTorrentsCheck.isSelected());
		});
		moveTorrentsCheck.selectedProperty().addListener((obs, oldV, newV) -> onMoveTorrentsChecked(newV));
		moveTorrentsCheck.setOnAction(e -> {
			preferencesChanged.set(true);
			onMoveTorrentsChecked(moveTorrentsCheck.isSelected());			
		});
		loadTorrentsCheck.selectedProperty().addListener((obs, oldV, newV) -> onLoadTorrentsChecked(newV));
		loadTorrentsCheck.setOnAction(e -> {
			preferencesChanged.set(true);
			onLoadTorrentsChecked(loadTorrentsCheck.isSelected());			
		});
		moveFromDefaultCheck.setOnAction(e -> preferencesChanged.set(true));
		deleteTorrentsCheck.setOnAction(e -> preferencesChanged.set(true));
		
		final String downloadsDirectorySet = ApplicationPreferences.getProperty(
				PathProperties.NEW_DOWNLOADS_SET, null);
		downloadsDirectoryCheck.selectedProperty().set(downloadsDirectorySet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(downloadsDirectorySet));
		
		final String completedDirectorySet = ApplicationPreferences.getProperty(
				PathProperties.COMPLETED_DOWNLOADS_SET, null); 
		completedDirectoryCheck.selectedProperty().set(completedDirectorySet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(completedDirectorySet));
		
		final String storeTorrentsSet = ApplicationPreferences.getProperty(
				PathProperties.NEW_TORRENTS_SET, null);
		storeTorrentsCheck.selectedProperty().set(storeTorrentsSet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(storeTorrentsSet));
				
		final String moveTorrentsSet = ApplicationPreferences.getProperty(
				PathProperties.COMPLETED_TORRENTS_SET, null);
		moveTorrentsCheck.selectedProperty().set(moveTorrentsSet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(moveTorrentsSet));
		
		final String loadTorrentsSet = ApplicationPreferences.getProperty(
				PathProperties.LOAD_TORRENTS_SET, null);
		loadTorrentsCheck.selectedProperty().set(loadTorrentsSet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(loadTorrentsSet));
		
		final String moveFromDefaultSet = ApplicationPreferences.getProperty(
				PathProperties.MOVE_COMPLETED_DOWNLOADS_FROM_DEFAULT_SET, null);
		moveFromDefaultCheck.selectedProperty().set(moveFromDefaultSet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(moveFromDefaultSet));
		
		final String deleteTorrentsSet = ApplicationPreferences.getProperty(
				PathProperties.DELETE_LOADED_TORRENTS_SET, null); 
		deleteTorrentsCheck.selectedProperty().set(deleteTorrentsSet != null && 
				!ApplicationPreferences.BOOLEAN_PROPERTY_UNSET.equals(deleteTorrentsSet));
	}
	
	private void onBrowseForPath(final TextField targetPathField) {
		final String enteredPath = targetPathField.getText();
		final String selectedPath = fileActionHandler.getTargetDirectoryPath(owner, 
				enteredPath != null? enteredPath : System.getProperty("user.home"), "Browse");
		if(selectedPath != null) {
			targetPathField.setText(selectedPath);
		}
	}
	
	private void onLoadTorrentsChecked(final boolean isChecked) {
		loadTorrentsDirectoryField.setDisable(!isChecked);
		browseLoadTorrentsDirectoryButton.setDisable(!isChecked);
		deleteTorrentsCheck.setDisable(!isChecked);
	}
	
	private void onMoveTorrentsChecked(final boolean isChecked) {
		moveTorrentsDirectoryField.setDisable(!isChecked);
		browseMoveTorrentsDirectoryButton.setDisable(!isChecked);
	}
	
	private void onStoreTorrentsChecked(final boolean isChecked) {
		storeTorrentsDirectoryField.setDisable(!isChecked);
		browseStoreTorrentsDirectoryButton.setDisable(!isChecked);
	}
	
	private void onDownloadsDirectoryChecked(final boolean isChecked) {
		downloadDirectoryField.setDisable(!isChecked);
		browseDownloadDirectoryButton.setDisable(!isChecked);
	}
	
	private void onCompletedDirectoryChecked(final boolean isChecked) {
		completedDirectoryField.setDisable(!isChecked);
		browseCompletedDirectoryButton.setDisable(!isChecked);
		moveFromDefaultCheck.setDisable(!isChecked);
	}
	
	private ScrollPane buildDirectoriesOptionsView() {		
		//Downloaded files directory				
		final VBox downloadDirectoryPane = buildDownloadDirectoryPane();		
		VBox.setMargin(moveFromDefaultCheck, new Insets(0, 0, 0, 25));
		
		//Torrent location directory
		final VBox torrentDirectoryPane = buildTorrentDirectoryPane();
		
		//Key store directory				
		final HBox keyStorePane = new HBox();
		keyStorePane.getStyleClass().add("layout-horizontal-spacing");
		keyStorePane.getChildren().addAll(keyStoreLocationField, browseKeyStoreButton);
												
		HBox.setHgrow(keyStoreLocationField, Priority.ALWAYS);
		
		final VBox content = new VBox();
		content.getStyleClass().add("layout-vertical-spacing");		
		content.getChildren().addAll(
				GuiUtils.applyBorder(downloadDirectoryPane, "Location of Downloaded Files", BorderType.OPTIONS_WINDOW_BORDER),
				GuiUtils.applyBorder(torrentDirectoryPane, "Location of .torrents", BorderType.OPTIONS_WINDOW_BORDER),
				GuiUtils.applyBorder(keyStorePane, "Location of Certificate Key Store", BorderType.OPTIONS_WINDOW_BORDER));
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		
		return contentScroll;
	}
	
	private VBox buildTorrentDirectoryPane() {
		final HBox browseStoreTorrentDirectoryPane = new HBox();
		browseStoreTorrentDirectoryPane.getStyleClass().addAll(
				"option-category-indentation", "layout-horizontal-spacing");
		browseStoreTorrentDirectoryPane.getChildren().addAll(storeTorrentsDirectoryField, 
				browseStoreTorrentsDirectoryButton);
		
		final HBox browseMoveTorrentDirectoryPane = new HBox();
		browseMoveTorrentDirectoryPane.getStyleClass().addAll(
				"option-category-indentation", "layout-horizontal-spacing");
		browseMoveTorrentDirectoryPane.getChildren().addAll(moveTorrentsDirectoryField, 
				browseMoveTorrentsDirectoryButton);
		
		final GridPane loadTorrentsPane = new GridPane();
		loadTorrentsPane.setHgap(30);
		loadTorrentsPane.add(loadTorrentsCheck, 0, 0);
		loadTorrentsPane.add(deleteTorrentsCheck, 1, 0);
		
		final HBox browseLoadTorrentDirectoryPane = new HBox();
		browseLoadTorrentDirectoryPane.getStyleClass().addAll(
				"option-category-indentation", "layout-horizontal-spacing");
		browseLoadTorrentDirectoryPane.getChildren().addAll(loadTorrentsDirectoryField, 
				browseLoadTorrentsDirectoryButton);
		
		HBox.setHgrow(storeTorrentsDirectoryField, Priority.ALWAYS);
		HBox.setHgrow(moveTorrentsDirectoryField, Priority.ALWAYS);
		HBox.setHgrow(loadTorrentsDirectoryField, Priority.ALWAYS);
		
		final VBox torrentDirectoryPane = new VBox();
		torrentDirectoryPane.getStyleClass().add("layout-vertical-spacing");
		torrentDirectoryPane.getChildren().addAll(storeTorrentsCheck, browseStoreTorrentDirectoryPane,
				moveTorrentsCheck, browseMoveTorrentDirectoryPane, loadTorrentsPane, 
				browseLoadTorrentDirectoryPane);
		
		return torrentDirectoryPane;
	}
	
	private VBox buildDownloadDirectoryPane() {
		final HBox browseDownloadDirectoryPane = new HBox();
		browseDownloadDirectoryPane.getStyleClass().addAll(
				"option-category-indentation", "layout-horizontal-spacing");
		browseDownloadDirectoryPane.getChildren().addAll(downloadDirectoryField, 
				browseDownloadDirectoryButton);
		
		final HBox browseCompletedDirectoryPane = new HBox();
		browseCompletedDirectoryPane.getStyleClass().addAll(
				"option-category-indentation", "layout-horizontal-spacing");
		browseCompletedDirectoryPane.getChildren().addAll(completedDirectoryField, 
				browseCompletedDirectoryButton);	
		
		HBox.setHgrow(completedDirectoryField, Priority.ALWAYS);
		HBox.setHgrow(downloadDirectoryField, Priority.ALWAYS);
		
		final VBox downloadDirectoryPane = new VBox();
		downloadDirectoryPane.getStyleClass().add("layout-vertical-spacing");
		downloadDirectoryPane.getChildren().addAll(downloadsDirectoryCheck, browseDownloadDirectoryPane,
				completedDirectoryCheck, browseCompletedDirectoryPane, moveFromDefaultCheck);
		
		return downloadDirectoryPane;
	}
}
