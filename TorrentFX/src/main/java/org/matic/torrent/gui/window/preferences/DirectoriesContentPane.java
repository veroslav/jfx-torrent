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

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.GuiUtils.BorderType;

public final class DirectoriesContentPane implements CategoryContentPane {

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
	
	public DirectoriesContentPane() {
		initComponents();
		contentPane = buildDirectoriesOptionsView();
	}
	
	@Override
	public final ScrollPane getContentPane() {
		return contentPane;
	}
	
	@Override
	public final String getName() {
		return "Directories";
	}
	
	private void initComponents() {
		loadTorrentsDirectoryField.setDisable(true);
		storeTorrentsDirectoryField.setDisable(true);
		moveTorrentsDirectoryField.setDisable(true);
		completedDirectoryField.setDisable(true);
		downloadDirectoryField.setDisable(true);		
		keyStoreLocationField.setDisable(true);
		
		browseLoadTorrentsDirectoryButton.setDisable(true);
		browseStoreTorrentsDirectoryButton.setDisable(true);
		browseMoveTorrentsDirectoryButton.setDisable(true);
		browseCompletedDirectoryButton.setDisable(true);
		browseDownloadDirectoryButton.setDisable(true);
		
		moveFromDefaultCheck.setSelected(true);
		moveFromDefaultCheck.setDisable(true);
		deleteTorrentsCheck.setDisable(true);
		
		downloadsDirectoryCheck.setOnAction(e -> {
			final boolean isSelected = downloadsDirectoryCheck.isSelected();
			downloadDirectoryField.setDisable(!isSelected);
			browseDownloadDirectoryButton.setDisable(!isSelected);
		});
		completedDirectoryCheck.setOnAction(e -> {
			final boolean isSelected = completedDirectoryCheck.isSelected();
			completedDirectoryField.setDisable(!isSelected);
			browseCompletedDirectoryButton.setDisable(!isSelected);
			moveFromDefaultCheck.setDisable(!isSelected);
		});
		storeTorrentsCheck.setOnAction(e -> {
			final boolean isSelected = storeTorrentsCheck.isSelected();
			storeTorrentsDirectoryField.setDisable(!isSelected);
			browseStoreTorrentsDirectoryButton.setDisable(!isSelected);
		});
		moveTorrentsCheck.setOnAction(e -> {
			final boolean isSelected = moveTorrentsCheck.isSelected();
			moveTorrentsDirectoryField.setDisable(!isSelected);
			browseMoveTorrentsDirectoryButton.setDisable(!isSelected);
		});
		loadTorrentsCheck.setOnAction(e -> {
			final boolean isSelected = loadTorrentsCheck.isSelected();
			loadTorrentsDirectoryField.setDisable(!isSelected);
			browseLoadTorrentsDirectoryButton.setDisable(!isSelected);
			deleteTorrentsCheck.setDisable(!isSelected);
		});
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
