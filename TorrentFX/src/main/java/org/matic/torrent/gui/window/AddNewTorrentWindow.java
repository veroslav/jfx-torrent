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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
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
import org.matic.torrent.gui.model.TorrentContentModel;
import org.matic.torrent.io.DiskUtilities;
import org.matic.torrent.io.codec.BinaryDecoder;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedString;
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
	
	private final TreeTableView<TorrentContentModel> torrentContentsTable;
	private final TextField nameTextField;
	
	private final Button selectNoneButton;
	private final Button selectAllButton;
	private final Button advancedButton;
	private final Button browseButton;
	
	private final Label fileSizeLabel;
	
	private final Dialog<ButtonType> window;
	
	private final BinaryEncodedDictionary infoDictionary;
	
	private ObservableList<TorrentContentModel> torrentContents;
		
	private final String fileName;
	private final String comment;
	private final Path filePath;
	
	private long availableDiskSpace;

	public AddNewTorrentWindow(final Window owner, final Path filePath, final BinaryEncodedDictionary torrentMetaData) {		
		infoDictionary = ((BinaryEncodedDictionary)torrentMetaData.get(BinaryDecoder.KEY_INFO));
		
		final BinaryEncodedString metaDataComment = (BinaryEncodedString)torrentMetaData.get(BinaryDecoder.KEY_COMMENT);
		comment = metaDataComment != null? metaDataComment.toString() : "";
		fileName = infoDictionary.get(BinaryDecoder.KEY_NAME).toString();		
		
		try {
			availableDiskSpace = DiskUtilities.getAvailableDiskSpace(filePath);
		} catch (final IOException ioe) {
			availableDiskSpace = -1;
		}
		
		this.filePath = filePath;
		
		window = new Dialog<>();
		window.initOwner(owner);
		
		dontShowAgainCheckbox = new CheckBox("Don't show this again");
		addToTopQueueCheckbox = new CheckBox("Add to top of queue");
		createSubFolderCheckbox = new CheckBox("Create subfolder");
		startTorrentCheckbox = new CheckBox("Start torrent");
		skipHashCheckbox = new CheckBox("Skip hash check");
		
		torrentContentsTable = new TreeTableView<TorrentContentModel>();
		nameTextField = new TextField(fileName);
		
		fileSizeLabel = new Label();
		
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
		
		final TreeTableColumn<TorrentContentModel, String> pathColumn = new TreeTableColumn<TorrentContentModel, String>("Path");
		pathColumn.setVisible(false);
			
		torrentContentsTable.setRoot(createTorrentContentTree());
		torrentContentsTable.setTableMenuButtonVisible(true);
		torrentContentsTable.setEditable(true);				
		addTreeViewColumns(torrentContentsTable);
        
		window.setHeaderText(null);
		window.setTitle(fileName + " - Add New Torrent");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private void addTreeViewColumns(final TreeTableView<TorrentContentModel> torrentContentsTable) {
		final TreeTableColumn<TorrentContentModel, Boolean> selectedColumn = new TreeTableColumn<TorrentContentModel, Boolean>("Name");	
		selectedColumn.setCellValueFactory(param -> param.getValue().getValue().selectedProperty());	
		//selectedColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(selectedColumn));
		
		/*selectedColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call(final Integer index) {
				return torrentContents.get(index).selectedProperty();
			}			
		}));*/
		
		/*selectedColumn.setCellFactory(new Callback<TreeTableColumn<TorrentContentModel,Boolean>, 
				TreeTableCell<TorrentContentModel,Boolean>>() {
			@Override
			public TreeTableCell<TorrentContentModel, Boolean> call(
					TreeTableColumn<TorrentContentModel, Boolean> column) {
				return new CheckBoxTreeTableCell<>();
			}
		});*/
		
		selectedColumn.setCellFactory(column -> new TreeTableCell<TorrentContentModel, Boolean>() {			
			@Override
			protected final void updateItem(final Boolean selected, boolean empty) {
				super.updateItem(selected, empty);
				if(selected == null || empty) {
					this.setGraphic(null);
				}
				else {
					final TorrentContentModel fileContent = torrentContents.get(getIndex());					
					final CheckBox selectionCheckBox = new CheckBox();
					final HBox checkBoxPane = new HBox();
					checkBoxPane.getChildren().addAll(selectionCheckBox, 
							new Label(fileContent.nameProperty().get()));
										
	                selectionCheckBox.setSelected(fileContent.selectedProperty().get());
	                selectionCheckBox.selectedProperty().bindBidirectional(fileContent.selectedProperty());
	                setGraphic(checkBoxPane);
				}
			}			
		});
		
		selectedColumn.setEditable(true);
		
		final TreeTableColumn<TorrentContentModel, String> pathColumn = new TreeTableColumn<TorrentContentModel, String>("Path");
		pathColumn.setCellValueFactory(param -> param.getValue().getValue().pathProperty());
		pathColumn.setVisible(false);
		
		final TreeTableColumn<TorrentContentModel, Long> sizeColumn = new TreeTableColumn<TorrentContentModel, Long>("Size");
		sizeColumn.setCellValueFactory(param -> param.getValue().getValue().sizeProperty().asObject());
		sizeColumn.setCellFactory(column -> new TreeTableCell<TorrentContentModel, Long>() {
			@Override
			protected void updateItem(final Long value, final boolean empty) {
				super.updateItem(value, empty);
				if(value == null || empty) {
					this.setGraphic(null);
				}
				else {
					final TorrentContentModel fileContent = torrentContents.get(getIndex());					
					final String formattedValue = UnitConverter.formatByteCount(fileContent.sizeProperty().get());
					final Label valueLabel = new Label(formattedValue);
	                setGraphic(valueLabel);
				}
			}			
		});
		
		final Collection<TreeTableColumn<TorrentContentModel, ?>> tableColumns = Arrays.asList(
				selectedColumn, pathColumn, sizeColumn);
		
		torrentContentsTable.getColumns().addAll(tableColumns);
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
		labelPane.add(new Label(fileName), 1, 0);
		
		labelPane.add(new Label("Comment:"), 0, 1);	
		labelPane.add(new Label(comment), 1, 1);
		
		labelPane.add(new Label("Size:"), 0, 2);			
		labelPane.add(fileSizeLabel, 1, 2);
		
		labelPane.add(new Label("Date:"), 0, 3);	
		labelPane.add(new Label("2015-02-24 12:41:00"), 1, 3);
		
		final HBox selectionButtonsPane = new HBox(10);
		selectionButtonsPane.setAlignment(Pos.CENTER_RIGHT);
		selectionButtonsPane.getChildren().addAll(selectAllButton, selectNoneButton);
		
		final VBox northPane = new VBox(10);
		northPane.getChildren().addAll(labelPane, selectionButtonsPane);
		
		final ScrollPane torrentContentsScroll = new ScrollPane(torrentContentsTable);
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
	
	private TreeItem<TorrentContentModel> createTorrentContentTree() {
		//TODO: Populate from infoDictionary
		final TorrentContentModel file1 = new TorrentContentModel("VTS_1.vob", "/home/Downloads", 565324762);		
		final TorrentContentModel file2 = new TorrentContentModel("VTS_2.vob", "/temp", 13753859387L);
		
		torrentContents = FXCollections.observableArrayList(file1, file2);
		torrentContents.forEach(m -> {
			m.selectedProperty().addListener((observable, oldValue, newValue) -> {	
				if(availableDiskSpace != -1) {
					final long affectedFileSize = m.sizeProperty().get();
					availableDiskSpace += (newValue? -affectedFileSize : affectedFileSize);
					
					fileSizeLabel.setText("761 MB (disk space: " + 
					UnitConverter.formatByteCount(availableDiskSpace) + ")");
				}
				else {
					fileSizeLabel.setText("761 MB (disk space: unavailable)");
				}
			});
		});
		
		final TreeItem<TorrentContentModel> root = new TreeItem<>(torrentContents.get(0));
		root.getChildren().add(new TreeItem<TorrentContentModel>(torrentContents.get(1)));
		
		root.setExpanded(true);
		return root;
	}
}