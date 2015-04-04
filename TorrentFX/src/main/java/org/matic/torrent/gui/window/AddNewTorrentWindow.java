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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import org.controlsfx.tools.Borders;
import org.matic.torrent.gui.tree.TorrentContentTree;
import org.matic.torrent.io.DiskUtilities;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedInteger;
import org.matic.torrent.io.codec.BinaryEncodedList;
import org.matic.torrent.io.codec.BinaryEncodedString;
import org.matic.torrent.io.codec.BinaryEncodingKeyNames;
import org.matic.torrent.utils.UnitConverter;

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
	
	private final TextField nameTextField;
	
	private final Button collapseAllButton;
	private final Button expandAllButton;
	private final Button selectNoneButton;
	private final Button selectAllButton;
	private final Button advancedButton;
	private final Button browseButton;
	
	private final TextFlow fileSizeLabel;
	private final Text diskSpaceText;
	private final Text fileSizeText;
	
	private final BinaryEncodedDictionary infoDictionary;
	private final TorrentContentTree torrentContentTree;
	
	private final Dialog<ButtonType> window;
		
	private final String creationDate;
	private final String fileName;
	private final String comment;
	
	private Optional<Long> availableDiskSpace;
	private long fileSelectionLength;

	public AddNewTorrentWindow(final Window owner, final BinaryEncodedDictionary torrentMetaData) {	
		
		final BinaryEncodedInteger creationDateInSeconds = (BinaryEncodedInteger)torrentMetaData.get(
				BinaryEncodingKeyNames.KEY_CREATION_DATE);
		creationDate = creationDateInSeconds != null? UnitConverter.formatTime(creationDateInSeconds.getValue() * 1000) : "";
		infoDictionary = ((BinaryEncodedDictionary)torrentMetaData.get(BinaryEncodingKeyNames.KEY_INFO));
		
		final Path savePath = Paths.get(System.getProperty("user.home"));
		availableDiskSpace = DiskUtilities.getAvailableDiskSpace(savePath);
		
		savePathCombo = new ComboBox<String>();
		savePathCombo.getItems().add(savePath.toString());		
		
		final BinaryEncodedString metaDataComment = (BinaryEncodedString)torrentMetaData.get(BinaryEncodingKeyNames.KEY_COMMENT);
		comment = metaDataComment != null? metaDataComment.toString() : "";
		fileName = infoDictionary.get(BinaryEncodingKeyNames.KEY_NAME).toString();
		
		window = new Dialog<>();
		window.initOwner(owner);
		
		dontShowAgainCheckbox = new CheckBox("Don't show this again");
		addToTopQueueCheckbox = new CheckBox("Add to top of queue");
		createSubFolderCheckbox = new CheckBox("Create subfolder");
		startTorrentCheckbox = new CheckBox("Start torrent");
		skipHashCheckbox = new CheckBox("Skip hash check");
				
		diskSpaceText = new Text();
		fileSizeText = new Text();		
		fileSizeLabel = new TextFlow();
		
		fileSizeText.setFontSmoothingType(FontSmoothingType.LCD);		
		diskSpaceText.setFontSmoothingType(FontSmoothingType.LCD);
		fileSizeLabel.getChildren().addAll(fileSizeText, diskSpaceText);	
		
		labelCombo = new ComboBox<String>();
		advancedButton = new Button("Advanced...");
		
		torrentContentTree = new TorrentContentTree(fileName, 
				(BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeyNames.KEY_LENGTH),
				(BinaryEncodedList)infoDictionary.get(BinaryEncodingKeyNames.KEY_FILES));
		
		fileSelectionLength = torrentContentTree.getRootFileEntry().getSize();
		updateDiskUsageLabel();
						
		torrentContentTree.fileSelectionSize().addListener((observable, oldValue, newValue) -> {
			fileSelectionLength = newValue.longValue();
			updateDiskUsageLabel();
		});		
				
		nameTextField = new TextField(fileName);		
		
		collapseAllButton = new Button("Collapse All");
		expandAllButton = new Button("Expand All");
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
		labelCombo.setEditable(true);
		
		savePathCombo.setMinWidth(400);
		savePathCombo.setMaxWidth(400);
		savePathCombo.setEditable(true);
		savePathCombo.getSelectionModel().selectFirst();
		savePathCombo.setOnAction(event -> {
			final Path targetDownloadPath = Paths.get(savePathCombo.getSelectionModel().getSelectedItem());
			availableDiskSpace = DiskUtilities.getAvailableDiskSpace(targetDownloadPath);
			updateDiskUsageLabel();
		});
		
		browseButton.setOnAction(event -> onBrowseForTargetDirectory());
		
		selectAllButton.setOnAction(event -> {
			torrentContentTree.selectAllEntries();
			selectAllButton.requestFocus();
		});
		
		selectNoneButton.setOnAction(event -> {
			torrentContentTree.unselectAllEntries();
			selectNoneButton.requestFocus();
		});
		
		collapseAllButton.setOnAction(event -> torrentContentTree.collapseAll());
		expandAllButton.setOnAction(event -> torrentContentTree.expandAll());
        
		window.setHeaderText(null);
		window.setTitle(fileName + " - Add New Torrent");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private void updateDiskUsageLabel() {
		final long totalFilesLength = torrentContentTree.getRootFileEntry().getSize();
		final StringBuilder fileSizeBuilder = new StringBuilder();
		fileSizeBuilder.append(UnitConverter.formatByteCount(fileSelectionLength));
		
		if(fileSelectionLength < totalFilesLength) {
			fileSizeBuilder.append(" (of ");
			fileSizeBuilder.append(UnitConverter.formatByteCount(totalFilesLength));
			fileSizeBuilder.append(")");
		}
		
		final StringBuilder diskSpaceBuilder = new StringBuilder();
		diskSpaceBuilder.append(" (disk space: ");				
		
		fileSizeText.setText(fileSizeBuilder.toString());	
		
		try {
			final long availableDiskSpaceValue = availableDiskSpace.orElseThrow(() ->
				new IOException("Disk usage can't be calculated"));
			final long remainingDiskSpace = availableDiskSpaceValue - fileSelectionLength;
			
			if(remainingDiskSpace < 0) {
				diskSpaceBuilder.append(UnitConverter.formatByteCount(Math.abs(remainingDiskSpace)));
				diskSpaceBuilder.append(" too short)");
				diskSpaceText.setText(diskSpaceBuilder.toString());
				diskSpaceText.setFill(Color.RED);
			}
			else {
				diskSpaceBuilder.append(UnitConverter.formatByteCount(remainingDiskSpace));
				diskSpaceBuilder.append(")");
				diskSpaceText.setText(diskSpaceBuilder.toString());
				diskSpaceText.setFill(Color.BLACK);
			}	
		} catch(final IOException ioe) {
			diskSpaceBuilder.append("can't be calculated)");
			diskSpaceText.setText(diskSpaceBuilder.toString());
			diskSpaceText.setFill(Color.RED);
		}
	}
	
	private void onBrowseForTargetDirectory() {
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		directoryChooser.setTitle("Select download directory");
		
		final File selectedDirectory = directoryChooser.showDialog(window.getOwner());
		
		if(selectedDirectory != null) {
			final String selectedTargetDirectory = selectedDirectory.getAbsolutePath();
			if(!savePathCombo.getItems().contains(selectedTargetDirectory)) {
				savePathCombo.getItems().add(selectedTargetDirectory);
			}
			savePathCombo.getSelectionModel().select(selectedTargetDirectory);
		}		
	}
	
	private Node layoutContent() {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPrefHeight(500);
		mainPane.setPrefWidth(1100);
		
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
		labelPane.add(new Label(fileName), 1, 0);
		
		labelPane.add(new Label("Comment:"), 0, 1);	
		labelPane.add(new Label(comment), 1, 1);
		
		labelPane.add(new Label("Size:"), 0, 2);			
		labelPane.add(fileSizeLabel, 1, 2);
		
		labelPane.add(new Label("Date:"), 0, 3);			
		labelPane.add(new Label(creationDate), 1, 3);
		
		final HBox expandCollapseButtonsPane = new HBox(10);
		expandCollapseButtonsPane.setAlignment(Pos.CENTER_LEFT);
		expandCollapseButtonsPane.getChildren().addAll(expandAllButton, collapseAllButton);
		
		final HBox selectionButtonsPane = new HBox(10);
		selectionButtonsPane.setAlignment(Pos.CENTER_RIGHT);
		selectionButtonsPane.getChildren().addAll(selectAllButton, selectNoneButton);
		
		final BorderPane buttonsPane = new BorderPane();
		buttonsPane.setLeft(expandCollapseButtonsPane);
		buttonsPane.setRight(selectionButtonsPane);
		
		final VBox northPane = new VBox(10);
		northPane.getChildren().addAll(labelPane, buttonsPane);
		
		final ScrollPane torrentContentsScroll = new ScrollPane();
		torrentContentTree.wrapWith(torrentContentsScroll);
		torrentContentsScroll.setFitToWidth(true);
		torrentContentsScroll.setFitToHeight(true);
		
		final BorderPane torrentContentsPane = new BorderPane();
		torrentContentsPane.setPrefWidth(450);
		torrentContentsPane.setTop(northPane);
		torrentContentsPane.setCenter(torrentContentsScroll);
		
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