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

package org.matic.torrent.gui.windows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.controlsfx.tools.Borders;
import org.matic.torrent.gui.model.TorrentFile;

/**
 * A window showing contents of a torrent to be opened and added to a list of torrents
 * 
 * @author vedran
 *
 */
public final class AddNewTorrentWindow {
	
	private final CheckBox createSubFolderCheckbox;
	private final CheckBox dontShowAgainCheckbox;
	private final CheckBox addToTopQueueCheckbox;
	private final CheckBox startTorrentCheckbox;
	private final CheckBox skipHashCheckbox;		
	
	private final ComboBox<String> savePathCombo;
	private final ComboBox<String> labelCombo;
	
	private final TreeTableView<TorrentFile> torrentContentsTable;
	private final TextField nameTextField;
	
	private final Button selectNoneButton;
	private final Button selectAllButton;
	private final Button advancedButton;
	private final Button browseButton;
	
	private final Dialog<ButtonType> window;
	private final Path torrentPath;

	public AddNewTorrentWindow(final Window owner, final Path torrentPath) {
		this.torrentPath = torrentPath;
		window = new Dialog<>();
		window.initOwner(owner);
		
		dontShowAgainCheckbox = new CheckBox("Don't show this again");
		addToTopQueueCheckbox = new CheckBox("Add to top of queue");
		createSubFolderCheckbox = new CheckBox("Create subfolder");
		startTorrentCheckbox = new CheckBox("Start torrent");
		skipHashCheckbox = new CheckBox("Skip hash check");
		
		torrentContentsTable = new TreeTableView<TorrentFile>();
		nameTextField = new TextField(torrentPath.getFileName().toString());
		
		savePathCombo = new ComboBox<String>();
		labelCombo = new ComboBox<String>();
		advancedButton = new Button("Advanced...");
		
		selectNoneButton = new Button("Select None");
		selectAllButton = new Button("Select All");
		browseButton = new Button("...");
		
		initComponents();
	}
	
	public final AddNewTorrentOptions showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();
		return null;
	}
	
	private void initComponents() {
		createSubFolderCheckbox.setSelected(true);
		startTorrentCheckbox.setSelected(true);
		savePathCombo.setMinWidth(400);		
		
		final Collection<TreeTableColumn<TorrentFile, String>> tableColumns = Arrays.asList(
				new TreeTableColumn<TorrentFile, String>("Name"),
				new TreeTableColumn<TorrentFile, String>("Path"),
				new TreeTableColumn<TorrentFile, String>("Size"));
        
		torrentContentsTable.setEditable(false);
		torrentContentsTable.getColumns().addAll(tableColumns);
        
		window.setHeaderText(null);
		window.setTitle(torrentPath.getFileName() + " - Add New Torrent");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private Node layoutContent() {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPrefHeight(400);
		
		mainPane.setLeft(buildLeftPane());
		mainPane.setCenter(buildTorrentContentsPane());
		
		return mainPane;
	}
	
	private Pane buildLeftPane() {
		final VBox northPane = new VBox();		
		northPane.getChildren().addAll(buildSaveOptionsPane(), buildNamePane(), buildTorrentOptionsPane());
		
		final HBox advancedPane = new HBox(10);
		advancedPane.setAlignment(Pos.BOTTOM_LEFT);
		advancedPane.getChildren().addAll(advancedButton, dontShowAgainCheckbox);		
		
		final Node borderedAdvancedPane = Borders.wrap(
				advancedPane).etchedBorder().buildAll();
		
		final BorderPane centerPane = new BorderPane();	
		centerPane.setBottom(borderedAdvancedPane);
		
		final BorderPane leftPane = new BorderPane();
		leftPane.setTop(northPane);
		leftPane.setCenter(centerPane);
		
		return leftPane;
	}
	
	private Node buildTorrentContentsPane() {
		final GridPane labelPane = new GridPane();
		
		final ColumnConstraints nameColumnConstraints = new ColumnConstraints(100);		
		final ColumnConstraints valueColumnConstraints = new ColumnConstraints();		
		
		labelPane.getColumnConstraints().addAll(nameColumnConstraints, valueColumnConstraints);
		
		labelPane.add(new Label("Name:"), 0, 0);
		labelPane.add(new Label(torrentPath.getFileName().toString()), 1, 0);
		
		labelPane.add(new Label("Comment:"), 0, 1);	
		labelPane.add(new Label("My comment"), 1, 1);
		
		labelPane.add(new Label("Size:"), 0, 2);	
		labelPane.add(new Label("761 MB (disk space: 11.3 GB)"), 1, 2);
		
		labelPane.add(new Label("Date:"), 0, 3);	
		labelPane.add(new Label("2015-02-24 12:41:00"), 1, 3);
		
		final HBox selectionButtonsPane = new HBox(10);
		selectionButtonsPane.setAlignment(Pos.CENTER_RIGHT);
		selectionButtonsPane.getChildren().addAll(selectAllButton, selectNoneButton);
		
		final VBox northPane = new VBox(10);
		northPane.getChildren().addAll(labelPane, selectionButtonsPane);				
		
		final BorderPane torrentContentsPane = new BorderPane();
		torrentContentsPane.setPrefWidth(450);
		torrentContentsPane.setTop(northPane);
		torrentContentsPane.setCenter(torrentContentsTable);
		
		BorderPane.setMargin(northPane, new Insets(0, 0, 10, 0));
		
		final Node borderedTorrentContentsPane = Borders.wrap(
				torrentContentsPane).etchedBorder().title("Torrent Contents").buildAll();
		
		return borderedTorrentContentsPane;
	}
	
	private Node buildSaveOptionsPane() {
		final BorderPane saveLocationPane = new BorderPane();
		saveLocationPane.setCenter(savePathCombo);
		saveLocationPane.setRight(browseButton);
		
		BorderPane.setMargin(browseButton, new Insets(0, 0, 0, 10));
		
		final VBox saveOptionsPane = new VBox(10);
		saveOptionsPane.getChildren().addAll(saveLocationPane, createSubFolderCheckbox);
		
		final Node borderedSaveOptionsPane = Borders.wrap(
				saveOptionsPane).etchedBorder().title("Save In").buildAll();
		
		return borderedSaveOptionsPane;
	}
	
	private Node buildNamePane() {
		final StackPane namePane = new StackPane(nameTextField);
		
		final Node borderedNamePane = Borders.wrap(
				namePane).etchedBorder().title("Name").buildAll();
		
		return borderedNamePane;
	}
	
	private Node buildTorrentOptionsPane() {
		final HBox labelPane = new HBox(5);		
		labelPane.getChildren().addAll(new Label("Label: "), labelCombo);
		labelPane.setAlignment(Pos.CENTER);
		HBox.setHgrow(labelCombo, Priority.ALWAYS);		
		labelCombo.setMaxWidth(Double.MAX_VALUE);
		
		final GridPane torrentOptionsPane = new GridPane();
		torrentOptionsPane.setVgap(5);
		
		final ColumnConstraints nameColumnConstraints = new ColumnConstraints(200);		
		final ColumnConstraints valueColumnConstraints = new ColumnConstraints();
		valueColumnConstraints.setFillWidth(true);
		valueColumnConstraints.setMaxWidth(Double.MAX_VALUE);
		valueColumnConstraints.setHalignment(HPos.LEFT);				
		
		torrentOptionsPane.getColumnConstraints().addAll(nameColumnConstraints, valueColumnConstraints);
		
		torrentOptionsPane.add(skipHashCheckbox, 0, 0);
		torrentOptionsPane.add(labelPane, 1, 0);
		
		torrentOptionsPane.add(startTorrentCheckbox, 0, 1);	
		torrentOptionsPane.add(addToTopQueueCheckbox, 1, 1);
		
		GridPane.setHgrow(labelPane, Priority.ALWAYS);
		
		final Node borderedTorrentOptionsPane = Borders.wrap(
				torrentOptionsPane).etchedBorder().title("Torrent Options").buildAll();
		
		return borderedTorrentOptionsPane;
	}
}