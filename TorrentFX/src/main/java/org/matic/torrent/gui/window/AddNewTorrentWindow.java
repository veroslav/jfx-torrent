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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.ImageView;
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
import javafx.stage.Window;

import org.controlsfx.tools.Borders;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.tree.FileNameColumnModel;
import org.matic.torrent.gui.tree.TorrentEntryNode;
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
	
	private final TreeTableView<TorrentFileEntry> torrentContentsTable;
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
	
	private final Dialog<ButtonType> window;
	
	private final BinaryEncodedDictionary infoDictionary;
	
	private final ObservableList<TorrentFileEntry> torrentFileEntries = 
			FXCollections.observableArrayList();
	
	private final LongProperty selectedFilesSize;
		
	private final String creationDate;
	private final String fileName;
	private final String comment;
	
	private long availableDiskSpace;

	//TODO: Dont pass filePath to constructor; use download target partition for available disk space calculation
	public AddNewTorrentWindow(final Window owner, final Path filePath, final BinaryEncodedDictionary torrentMetaData) {	
		
		final BinaryEncodedInteger creationDateInSeconds = (BinaryEncodedInteger)torrentMetaData.get(
				BinaryEncodingKeyNames.KEY_CREATION_DATE);
		creationDate = creationDateInSeconds != null? UnitConverter.formatTime(creationDateInSeconds.getValue() * 1000) : "";
		infoDictionary = ((BinaryEncodedDictionary)torrentMetaData.get(BinaryEncodingKeyNames.KEY_INFO));
		
		try {
			availableDiskSpace = DiskUtilities.getAvailableDiskSpace(filePath);
		} catch (final IOException ioe) {
			//TODO: Perhaps throw an exception here?
			availableDiskSpace = -1;
		}
		
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
		
		final TreeItem<TorrentFileEntry> rootNode = createTorrentContentTree(infoDictionary); 
		torrentContentsTable = new TreeTableView<TorrentFileEntry>();
		torrentContentsTable.setRoot(rootNode);
		
		final long totalSelectionLength = rootNode.getValue().getSize();
		updateSelectedFileLengths(totalSelectionLength, totalSelectionLength);
		
		selectedFilesSize = new SimpleLongProperty(rootNode.getValue().getSize());				
		selectedFilesSize.addListener((observable, oldValue, newValue) ->
			updateSelectedFileLengths(newValue.longValue(), rootNode.getValue().getSize()));		
				
		nameTextField = new TextField(fileName);		
		
		savePathCombo = new ComboBox<String>();
		labelCombo = new ComboBox<String>();
		advancedButton = new Button("Advanced...");
		
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
		savePathCombo.setMinWidth(400);
		savePathCombo.setEditable(true);
		labelCombo.setEditable(true);
		
		selectAllButton.setOnAction(event -> {
			onSelectAllTorrentEntries(torrentContentsTable);
			selectAllButton.requestFocus();
		});
		
		selectNoneButton.setOnAction(event -> {
			onUnselectAllTorrentEntries(torrentContentsTable);
			selectNoneButton.requestFocus();
		});
		
		collapseAllButton.setOnAction(event -> torrentContentsTable.getRoot().getChildren().forEach(
				child -> onCollapseFolderTree(child)));
		expandAllButton.setOnAction(event -> onExpandFolderTree(torrentContentsTable.getRoot()));
			
		torrentContentsTable.setTableMenuButtonVisible(true);
		torrentContentsTable.setShowRoot(false);
		torrentContentsTable.setEditable(true);				
		addTreeViewColumns(torrentContentsTable);
		addTreeViewContextMenus();
        
		window.setHeaderText(null);
		window.setTitle(fileName + " - Add New Torrent");
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private void onSelectAllTorrentEntries(final TreeTableView<TorrentFileEntry> table) {
		final int selectedIndex = table.getSelectionModel().getSelectedIndex();
		table.getRoot().getChildren().forEach(child -> child.getValue().selectedProperty().set(true));
		table.getSelectionModel().select(selectedIndex);
	}
	
	private void onUnselectAllTorrentEntries(final TreeTableView<TorrentFileEntry> table) {
		final int selectedIndex = table.getSelectionModel().getSelectedIndex();
		table.getRoot().getChildren().forEach(child -> {
			final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)child;
			if(checkBoxItem.isIndeterminate()) {
				checkBoxItem.setSelected(true);
			}
			child.getValue().selectedProperty().set(false);
		});		
		table.getSelectionModel().select(selectedIndex);
	}
	
	private void onCollapseFolderTree(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(child -> onCollapseFolderTree(child));
		treeItem.setExpanded(false);
	}
	
	private void onExpandFolderTree(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(child -> onExpandFolderTree(child));
		treeItem.setExpanded(true);
	}
	
	private void addTreeViewContextMenus() {
		torrentContentsTable.setRowFactory(table -> {
			final ContextMenu contextMenu = new ContextMenu();
			
			final MenuItem collapseFolderTreeMenuItem = new MenuItem("Collapse Folder Tree");
			final MenuItem expandFolderTreeMenuItem = new MenuItem("Expand Folder Tree");
			final MenuItem selectNoneMenuItem = new MenuItem("Select None");						
			final MenuItem selectAllMenuItem = new MenuItem("Select All");
			final MenuItem unselectMenuItem = new MenuItem("Unselect");
			final MenuItem selectMenuItem = new MenuItem("Select");
			
			final Menu priorityTreeMenu = new Menu("Priority");
			
			final RadioMenuItem noPriorityMenuItem = new RadioMenuItem("Skip");
			final RadioMenuItem highestPriorityMenuItem = new RadioMenuItem("Highest");
			final RadioMenuItem lowestPriorityMenuItem = new RadioMenuItem("Lowest");			
			final RadioMenuItem normalPriorityMenuItem = new RadioMenuItem("Normal");
			final RadioMenuItem highPriorityMenuItem = new RadioMenuItem("High");
			final RadioMenuItem lowPriorityMenuItem = new RadioMenuItem("Low");			
			{			
				final List<RadioMenuItem> radioMenuItems = Arrays.asList(lowestPriorityMenuItem, 
						lowPriorityMenuItem, normalPriorityMenuItem, highPriorityMenuItem, 
						highestPriorityMenuItem, noPriorityMenuItem);
				final ToggleGroup radioMenuGroup = new ToggleGroup();
				
				for(final RadioMenuItem radioMenuItem : radioMenuItems) {
					radioMenuItem.setToggleGroup(radioMenuGroup);
				}
				
				normalPriorityMenuItem.setSelected(true);
				priorityTreeMenu.getItems().addAll(lowestPriorityMenuItem, lowPriorityMenuItem,
						normalPriorityMenuItem, highPriorityMenuItem, highestPriorityMenuItem,
						new SeparatorMenuItem(), noPriorityMenuItem);
				contextMenu.getItems().addAll(selectMenuItem, unselectMenuItem, new SeparatorMenuItem(), 
						selectAllMenuItem, selectNoneMenuItem, new SeparatorMenuItem(), 
						collapseFolderTreeMenuItem, expandFolderTreeMenuItem, new SeparatorMenuItem(), 
						priorityTreeMenu);
			}
			
			final TreeTableRow<TorrentFileEntry> row = new TreeTableRow<TorrentFileEntry>() {				
				@Override
				protected void updateItem(final TorrentFileEntry item, final boolean empty) {
					super.updateItem(item, empty);
					if(empty) {
		                setContextMenu(null);
		            } 
					else {
						final CheckBoxTreeItem<TorrentFileEntry> treeItem = (CheckBoxTreeItem<TorrentFileEntry>)super.getTreeItem();
						expandFolderTreeMenuItem.setDisable(treeItem.isLeaf() || treeItem.isExpanded());
						collapseFolderTreeMenuItem.setDisable((treeItem.isLeaf() && (treeItem.getParent() == 
								this.getTreeTableView().getRoot())) || (!treeItem.isLeaf() && !treeItem.isExpanded()));
						selectMenuItem.setDisable(treeItem.isLeaf() && treeItem.isSelected() ||
								!treeItem.isLeaf() && !treeItem.isIndeterminate() && treeItem.isSelected());
						unselectMenuItem.setDisable(treeItem.isLeaf() && !treeItem.isSelected() ||
								!treeItem.isLeaf() && !treeItem.isIndeterminate() && !treeItem.isSelected());
		                setContextMenu(contextMenu);
		            }
				}				
			};
			
			selectMenuItem.setOnAction(evt -> {
				final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)row.getTreeItem();
				if(checkBoxItem.isIndeterminate()) {
					checkBoxItem.setSelected(false);
				}
				row.getItem().setSelected(true);
				unselectMenuItem.setDisable(false);
				}
			);
			unselectMenuItem.setOnAction(evt -> {
				final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)row.getTreeItem();
				if(checkBoxItem.isIndeterminate()) {
					checkBoxItem.setSelected(true);
				}
				row.getItem().setSelected(false);
				selectMenuItem.setDisable(false);
			});
			selectAllMenuItem.setOnAction(evt -> onSelectAllTorrentEntries(torrentContentsTable));
			selectNoneMenuItem.setOnAction(evt -> onUnselectAllTorrentEntries(torrentContentsTable));
			expandFolderTreeMenuItem.setOnAction(evt -> onExpandFolderTree(row.getTreeItem()));
			collapseFolderTreeMenuItem.setOnAction(evt -> {
				TreeItem<TorrentFileEntry> treeItem = row.getTreeItem();
				if(row.getTreeItem().isLeaf()) {
					final TreeItem<TorrentFileEntry> leafParent = treeItem.getParent();
					if(leafParent != torrentContentsTable.getRoot()) {
						treeItem = leafParent;
					}
					else {
						return;
					}
				}
				onCollapseFolderTree(treeItem);
			});
			
			return row;
		});
	}
	
	private void addTreeViewColumns(final TreeTableView<TorrentFileEntry> table) {
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = 
				new TreeTableColumn<TorrentFileEntry, FileNameColumnModel>("Name");	
		fileNameColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
		fileNameColumn.setEditable(true);
		fileNameColumn.setPrefWidth(350);
		fileNameColumn.setCellValueFactory(p -> {
			final TreeItem<TorrentFileEntry> treeItem = p.getValue();
			final TorrentFileEntry fileEntry = p.getValue().getValue();
			final FileNameColumnModel columnModel = new FileNameColumnModel(
					treeItem.isLeaf(), fileEntry.nameProperty().get());
			return new SimpleObjectProperty<FileNameColumnModel>(columnModel);
		});			
		fileNameColumn.setCellFactory(column -> new CheckBoxTreeTableCell<
				TorrentFileEntry, FileNameColumnModel>() {			
			final ImageView imageView = new ImageView();	
			final Label fileNameLabel = new Label();
			
			{
				ImageUtils.cropToSmallImage(imageView);
			}
			
			@Override
			public final void updateItem(final FileNameColumnModel item, final boolean empty) {
				super.updateItem(item, empty);				
				
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final TorrentFileEntry fileEntry = this.getTreeTableRow().getItem();					
					
					if(fileEntry == null) {
						return;
					}			
					
					final CheckBoxTreeItem<TorrentFileEntry> treeItem = 
							(CheckBoxTreeItem<TorrentFileEntry>)this.getTreeTableRow().getTreeItem();
					
					if(treeItem.isLeaf()) {
						imageView.setImage(fileEntry.getImage());
					}
					else {
						imageView.setImage(treeItem.isExpanded()? 
								ImageUtils.FOLDER_OPENED_IMAGE: ImageUtils.FOLDER_CLOSED_IMAGE);
					}
					
					final CheckBox selectionCheckBox = new CheckBox();					
					selectionCheckBox.setFocusTraversable(false);
					selectionCheckBox.setSelected(fileEntry.selectedProperty().get());					
					selectionCheckBox.selectedProperty().bindBidirectional(fileEntry.selectedProperty());
					selectionCheckBox.setIndeterminate(treeItem.isIndeterminate());
					
					treeItem.indeterminateProperty().bindBidirectional(
							selectionCheckBox.indeterminateProperty());					
					
					fileNameLabel.setText(fileEntry.nameProperty().get());
					fileNameLabel.setGraphic(imageView);
					
					final HBox checkBoxPane = new HBox();			
					checkBoxPane.getChildren().addAll(selectionCheckBox, fileNameLabel);
											                
	                setGraphic(checkBoxPane);
				}
			}			
		});
		fileNameColumn.setComparator((m, o) -> {
			if(!m.isLeaf() && o.isLeaf()) {
				return 1;
			}
			if(m.isLeaf() && !o.isLeaf()) {
				return -1;
			}
			return o.getName().compareTo(m.getName());
		});
		
		final TreeTableColumn<TorrentFileEntry, String> pathColumn = new TreeTableColumn<TorrentFileEntry, String>("Path");
		pathColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, String>("path"));
		pathColumn.setVisible(false);
		
		final TreeTableColumn<TorrentFileEntry, Integer> priorityColumn = new TreeTableColumn<TorrentFileEntry, Integer>("Priority");
		priorityColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Integer>("priority"));
		priorityColumn.setCellFactory(column -> new TreeTableCell<TorrentFileEntry, Integer>() {
			final Label valueLabel = new Label();			
			@Override
			protected final void updateItem(final Integer value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}
					
					valueLabel.setText(String.valueOf(fileContent.priorityProperty().get()));
	                setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER);
				}
			}		
		});
		
		final TreeTableColumn<TorrentFileEntry, Long> sizeColumn = new TreeTableColumn<TorrentFileEntry, Long>("Size");
		sizeColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Long>("size"));
		sizeColumn.setCellFactory(column -> new TreeTableCell<TorrentFileEntry, Long>() {
			final Label valueLabel = new Label();			
			
			@Override
			protected final void updateItem(final Long value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}
					
					final String formattedValue = UnitConverter.formatByteCount(fileContent.sizeProperty().get());					
					valueLabel.setText(formattedValue);
	                setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER_RIGHT);
				}
			}			
		});
		
		table.getColumns().addAll(Arrays.asList(fileNameColumn, pathColumn, sizeColumn, priorityColumn));
		table.getSortOrder().add(fileNameColumn);
	}
	
	private void updateSelectedFileLengths(final long selectedFilesLength, final long totalFilesLength) {
		final StringBuilder fileSizeBuilder = new StringBuilder();
		fileSizeBuilder.append(UnitConverter.formatByteCount(selectedFilesLength));
		
		if(selectedFilesLength < totalFilesLength) {
			fileSizeBuilder.append(" (of ");
			fileSizeBuilder.append(UnitConverter.formatByteCount(totalFilesLength));
			fileSizeBuilder.append(")");
		}
		
		final StringBuilder diskSpaceBuilder = new StringBuilder();
		diskSpaceBuilder.append(" (disk space: ");				
		
		fileSizeText.setText(fileSizeBuilder.toString());			
		final long remainingDiskSpace = availableDiskSpace - selectedFilesLength;
		
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
		expandCollapseButtonsPane.getChildren().addAll(collapseAllButton, expandAllButton);
		
		final HBox selectionButtonsPane = new HBox(10);
		selectionButtonsPane.setAlignment(Pos.CENTER_RIGHT);
		selectionButtonsPane.getChildren().addAll(selectAllButton, selectNoneButton);
		
		final BorderPane buttonsPane = new BorderPane();
		buttonsPane.setLeft(expandCollapseButtonsPane);
		buttonsPane.setRight(selectionButtonsPane);
		
		final VBox northPane = new VBox(10);
		northPane.getChildren().addAll(labelPane, buttonsPane);
		
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
	
	private TreeItem<TorrentFileEntry> createTorrentContentTree(final BinaryEncodedDictionary infoDictionary) {	
		final BinaryEncodedInteger length = (BinaryEncodedInteger)infoDictionary.get(BinaryEncodingKeyNames.KEY_LENGTH);
		final BinaryEncodedList files = (BinaryEncodedList)infoDictionary.get(BinaryEncodingKeyNames.KEY_FILES);
		
		TreeItem<TorrentFileEntry> root = null;						
		
		if(length != null) {
			//Handle single file torrent mode
			root = buildSingleFileTree(length.getValue());
			
		}
		else if(files != null) {
			//Handle multiple files torrent mode
			root = buildMultiFileTree(files);
		}
		else {
			//TODO: Handle invalid torrent, no files found (show an error to the user)
			return null;
		}
		
		root.setExpanded(true);
		
		return root;
	}
	
	private void addTreeItemListener(final TreeItem<TorrentFileEntry> treeItem) {
		final TorrentFileEntry fileEntry = treeItem.getValue();
		fileEntry.selectedProperty().addListener((observable, oldValue, newValue) -> {	
			if(treeItem.isLeaf()) {
				final long fileSize = fileEntry.sizeProperty().get();
				final long affectedFileSize = (newValue? -fileSize: fileSize);
				selectedFilesSize.set(selectedFilesSize.get() - affectedFileSize);
			}								
			torrentContentsTable.getSelectionModel().select(treeItem);
			torrentContentsTable.requestFocus();
		});
	}
	
	private TreeItem<TorrentFileEntry> buildSingleFileTree(final long fileLength) {
		final TorrentFileEntry fileEntry = new TorrentFileEntry(fileName, ". (current path)", 
				fileLength, ImageUtils.getFileTypeImage(fileName));
		final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);		
		final CheckBoxTreeItem<TorrentFileEntry> root = new CheckBoxTreeItem<>(new TorrentFileEntry(
				"root", ". (current path)", fileLength, null));			
		root.getChildren().add(treeItem);
		
		return root;
	}
	
	private TreeItem<TorrentFileEntry> buildMultiFileTree(final BinaryEncodedList files) {			
		final TorrentFileEntry fileDirEntry = new TorrentFileEntry(fileName, ". (current path)", 0L, null);					
		final CheckBoxTreeItem<TorrentFileEntry> fileDirTreeItem = new CheckBoxTreeItem<>(fileDirEntry);
		final TorrentEntryNode<TreeItem<TorrentFileEntry>> fileDirNode = new TorrentEntryNode<>(fileName, fileDirTreeItem);
		
		fileDirTreeItem.selectedProperty().bindBidirectional(fileDirEntry.selectedProperty());	
		
		//Iterate through all the files contained by this torrent
		files.stream().forEach(fd -> {
			final BinaryEncodedDictionary fileDictionary = (BinaryEncodedDictionary)fd;																
			final long fileLength = ((BinaryEncodedInteger)fileDictionary.get(
					BinaryEncodingKeyNames.KEY_LENGTH)).getValue();
									
			final BinaryEncodedList filePaths = (BinaryEncodedList)fileDictionary.get(BinaryEncodingKeyNames.KEY_PATH);
			TorrentEntryNode<TreeItem<TorrentFileEntry>> currentNode = fileDirNode;
			final StringBuilder pathBuilder = new StringBuilder("./");
			final int filePathSize = filePaths.size();
			
			//Process all directories on the file path
			for(int i = 0; i < filePathSize - 1; ++i) {				
				final String pathName = filePaths.get(i).toString();
				pathBuilder.append(pathName);
				if(!currentNode.contains(pathName)) {										
					final TorrentFileEntry fileEntry = new TorrentFileEntry(pathName, pathBuilder.toString(), 0L, null);
					final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);
					currentNode.getData().getChildren().add(treeItem);		
					
					final TorrentEntryNode<TreeItem<TorrentFileEntry>> childNode = new TorrentEntryNode<>(pathName, treeItem);					
					currentNode.add(childNode);					
					currentNode = childNode;
				}
				else {
					currentNode = currentNode.getChild(pathName);
				}
				pathBuilder.append("/");
			}			
			
			//Process the file itself when we get here (last element on the path)
			final String leafName = filePaths.get(filePathSize-1).toString();			
			final TorrentFileEntry fileEntry = new TorrentFileEntry(leafName, pathBuilder.toString(), 
					fileLength, ImageUtils.getFileTypeImage(leafName));
			final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);
			currentNode.getData().getChildren().add(treeItem);
			
			final TorrentEntryNode<TreeItem<TorrentFileEntry>> childNode = new TorrentEntryNode<>(leafName, treeItem);					
			currentNode.add(childNode);		
			
			//Update file sizes for all of the childNode's parents in the tree
			final Consumer<CheckBoxTreeItem<TorrentFileEntry>> fileSizePropagation = (item) -> 
				item.getValue().updateSize(fileLength);
			applyOnParents(treeItem, fileSizePropagation);
		});
		return fileDirTreeItem;
	}
	
	private TreeItem<TorrentFileEntry> initTreeItem(final TorrentFileEntry fileEntry) {		
		torrentFileEntries.add(fileEntry);
		final CheckBoxTreeItem<TorrentFileEntry> treeItem = new CheckBoxTreeItem<>(fileEntry);
		treeItem.selectedProperty().bindBidirectional(fileEntry.selectedProperty());
		treeItem.setSelected(true);
		addTreeItemListener(treeItem);
		
		return treeItem;
	}
	
	private void applyOnParents(final TreeItem<TorrentFileEntry> child,
			final Consumer<CheckBoxTreeItem<TorrentFileEntry>> consumer) {
		CheckBoxTreeItem<TorrentFileEntry> current = (CheckBoxTreeItem<TorrentFileEntry>)child.getParent();		
		while(current != null) {			
			consumer.accept(current);
			current = (CheckBoxTreeItem<TorrentFileEntry>)current.getParent();
		}		
	}
}