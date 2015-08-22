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

package org.matic.torrent.gui.tree;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodingKeyNames;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.table.TableState;
import org.matic.torrent.gui.table.TableUtils;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.FilePriority;
import org.matic.torrent.utils.UnitConverter;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.ProgressBarTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * This class implements a graphic tree representation of a torrent's contents. 
 * 
 * @author Vedran Matic
 * 
 */
public final class TorrentContentTree {
		
	private static final String FIRST_PIECE_COLUMN_NAME = "First Piece";
	private static final String PIECE_COUNT_COLUMN_NAME = "#Pieces";
	private static final String PROGRESS_COLUMN_NAME = "Progress";
	private static final String PRIORITY_COLUMN_NAME = "Priority";
	private static final String PATH_COLUMN_NAME = "Path";
	private static final String NAME_COLUMN_NAME = "Name";
	private static final String SIZE_COLUMN_NAME = "Size";
	private static final String DONE_COLUMN_NAME = "Done";

	private final TreeTableView<TorrentFileEntry> fileEntryTree;
	private final LongProperty selectedFilesSize;
	
	/**
	 * Create and populate a file tree with data from a torrent's info dictionary
	 * 
	 * @param infoDictionary Torrent's info dictionary
	 * @param showProgressDetailColumns Whether to display columns showing download progress 
	 */
	public TorrentContentTree(final BinaryEncodedDictionary infoDictionary,
			final boolean showProgressDetailColumns) {		
		selectedFilesSize = new SimpleLongProperty();		 
		fileEntryTree = new TreeTableView<TorrentFileEntry>();
		initComponents(infoDictionary, showProgressDetailColumns);
	}
	
	/**
	 * Create an empty torrent content tree
	 * 
	 * @param addProgressDetailColumns Whether to display columns showing download progress
	 */
	public TorrentContentTree(final boolean addProgressDetailColumns) {
		this(null, addProgressDetailColumns);
	}
	
	@Override
	public final int hashCode() {
		return fileEntryTree.hashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TorrentContentTree other = (TorrentContentTree) obj;
		return other.fileEntryTree.equals(fileEntryTree);
	}
	
	/**
	 * Expose wrapped TreeTableView
	 * 
	 * @return
	 */
	public TreeTableView<TorrentFileEntry> getView() {
		return fileEntryTree;
	}
	
	/**
	 * Return the root file of a torrent (invisible)
	 * 
	 * @return
	 */
	public final TorrentFileEntry getRootFileEntry() {
		final TreeItem<TorrentFileEntry> root = fileEntryTree.getRoot();
		return root == null? null : root.getValue();
	}
	
	/**
	 * Return a property containing total file selection size
	 * 
	 * @return
	 */
	public final LongProperty fileSelectionSize() {
		return selectedFilesSize;
	}
	
	/**
	 * Handler for selection of all torrent file entries
	 */
	public final void selectAllEntries() {
		final int selectedIndex = fileEntryTree.getSelectionModel().getSelectedIndex();
		fileEntryTree.getRoot().getChildren().forEach(child -> child.getValue().selectedProperty().set(true));
		fileEntryTree.getSelectionModel().select(selectedIndex);
	}
	
	/**
	 * Handler for deselection of all torrent file entries
	 */
	public final void unselectAllEntries() {
		final int selectedIndex = fileEntryTree.getSelectionModel().getSelectedIndex();
		fileEntryTree.getRoot().getChildren().forEach(child -> {
			final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)child;
			if(checkBoxItem.isIndeterminate()) {
				checkBoxItem.setSelected(true);
			}
			child.getValue().selectedProperty().set(false);
		});		
		fileEntryTree.getSelectionModel().select(selectedIndex);
	}
	
	/**
	 * Handler for collapsing of all torrent file entries
	 */
	public final void collapseAll() {
		fileEntryTree.getRoot().getChildren().forEach(this::onCollapseFolderTree);
	}
	
	/**
	 * Handler for expansion of all torrent file entries
	 */
	public final void expandAll() {
		onExpandFolderTree(fileEntryTree.getRoot());
	}
	
	/**
	 * Update the content tree with new content
	 * 
	 * @param contentRoot The root of the new tree
	 */
	public void setContent(final TreeItem<TorrentFileEntry> contentRoot) {
		fileEntryTree.setRoot(contentRoot);
		selectedFilesSize.set(contentRoot != null? contentRoot.getValue().getSize() : 0);
	}
	
	/**
	 * Store any changes to column order, visibility, and/or size
	 */
	public void storeColumnStates() {		
		TableUtils.storeColumnStates(fileEntryTree.getColumns(), GuiProperties.INFO_TAB_COLUMN_VISIBILITY,
			GuiProperties.DEFAULT_INFO_TAB_COLUMN_VISIBILITIES, GuiProperties.INFO_TAB_COLUMN_SIZE,
			GuiProperties.DEFAULT_INFO_TAB_COLUMN_SIZES, GuiProperties.INFO_TAB_COLUMN_ORDER,
			GuiProperties.DEFAULT_INFO_TAB_COLUMN_ORDER);		
	}
	
	protected void onCollapseTreeItem(final TreeItem<TorrentFileEntry> targetItem) {
		TreeItem<TorrentFileEntry> treeItem = targetItem;
		if(treeItem.isLeaf()) {
			final TreeItem<TorrentFileEntry> leafParent = treeItem.getParent();
			if(leafParent != fileEntryTree.getRoot()) {
				treeItem = leafParent;
			}
			else {
				return;
			}
		}
		onCollapseFolderTree(treeItem);
	}
	
	protected void onExpandFolderTree(final TreeItem<TorrentFileEntry> treeItem) {		
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(this::onExpandFolderTree);
		treeItem.setExpanded(true);		
	}
	
	protected void selectItem(final CheckBoxTreeItem<TorrentFileEntry> treeItem) {
		fileEntryTree.getSelectionModel().select(treeItem);
	}
	
	protected void onUpdateParentPriority(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem == fileEntryTree.getRoot()) {
			return;
		}		
		final List<Integer> priorities = treeItem.getChildren().stream().map(child -> 
			child.getValue().priorityProperty().get()).distinct().collect(Collectors.toList());
		
		treeItem.getValue().priorityProperty().set(priorities.size() > 1? 
				FilePriority.MIXED.getValue() : priorities.get(0));
		
		onUpdateParentPriority(treeItem.getParent());
	}
	
	protected void onUpdateChildrenPriority(final TreeItem<TorrentFileEntry> parent, final int priority) {
		parent.getChildren().forEach(c -> {
			final CheckBoxTreeItem<TorrentFileEntry> child = (CheckBoxTreeItem<TorrentFileEntry>)c; 			
			if(!child.isLeaf()) {
				onUpdateChildrenPriority(child, priority);
			}
			final boolean isSelected = child.isSelected();
			child.setSelected(!isSelected && priority != FilePriority.SKIP.getValue() ||
					isSelected && priority == FilePriority.SKIP.getValue() || isSelected);
			child.getValue().priorityProperty().set(priority);
		});
	}
	
	private void onCollapseFolderTree(final TreeItem<TorrentFileEntry> treeItem) {		
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(this::onCollapseFolderTree);
		treeItem.setExpanded(false);		
	}
	
	private void initComponents(final BinaryEncodedDictionary infoDictionary,
			final boolean addProgressDetailColumns) {
		if(infoDictionary != null) {
			final TreeItem<TorrentFileEntry> rootNode = buildTorrentContentTree(infoDictionary);		
			fileEntryTree.setRoot(rootNode);
			selectedFilesSize.set(getRootFileEntry().getSize());
		}
		fileEntryTree.setTableMenuButtonVisible(false);
		fileEntryTree.setShowRoot(false);
		fileEntryTree.setEditable(true);					
		
		createColumns(addProgressDetailColumns);
		fileEntryTree.setRowFactory(table -> new TorrentContentTreeRow(this));				
	}
	
	private void createColumns(final boolean addProgressDetailColumns) {
		final LinkedHashMap<String, TreeTableColumn<TorrentFileEntry, ?>> columnMappings = new LinkedHashMap<>();
		
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = buildFileNameColumn();
		columnMappings.put(NAME_COLUMN_NAME, fileNameColumn);
		columnMappings.put(PATH_COLUMN_NAME, buildPathColumn());
		columnMappings.put(SIZE_COLUMN_NAME, buildSimpleLongValueColumn(SIZE_COLUMN_NAME, "size", 
						GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(), 
						tfe -> UnitConverter.formatByteCount(tfe.sizeProperty().get())));
		columnMappings.put(PRIORITY_COLUMN_NAME, buildPriorityColumn());
		
		if(addProgressDetailColumns) {
			columnMappings.put(DONE_COLUMN_NAME, buildSimpleLongValueColumn(DONE_COLUMN_NAME, "done",
					GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(),
							tfe -> UnitConverter.formatByteCount(tfe.doneProperty().get())));
			columnMappings.put(FIRST_PIECE_COLUMN_NAME,buildSimpleLongValueColumn(FIRST_PIECE_COLUMN_NAME, "firstPiece",
							GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(),
							tfe -> String.valueOf(tfe.firstPieceProperty().get())));
			columnMappings.put(PIECE_COUNT_COLUMN_NAME, buildSimpleLongValueColumn(
							PIECE_COUNT_COLUMN_NAME, "pieceCount", GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME,
							GuiUtils.rightPadding(),tfe -> String.valueOf(tfe.pieceCountProperty().get())));
			columnMappings.put(PROGRESS_COLUMN_NAME, buildProgressColumn());
		}		
		
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {				
			final TreeTableColumn<TorrentFileEntry,?> tableColumn = columnMappings.get(columnId);						
			fileEntryTree.getColumns().add(tableColumn);
			fileEntryTree.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());			
		};
		
		final TableState<TreeItem<TorrentFileEntry>> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.INFO_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_INFO_TAB_COLUMN_VISIBILITIES,
				GuiProperties.INFO_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_INFO_TAB_COLUMN_SIZES,
				GuiProperties.INFO_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_INFO_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(fileEntryTree.getColumns(), columnState, columnResizer);		
		fileEntryTree.getSortOrder().add(fileNameColumn);
	}
	
	private TreeTableColumn<TorrentFileEntry, Long> buildSimpleLongValueColumn(
			final String columnName, final String propertyName, final String style, final Insets padding,
			final Function<TorrentFileEntry, String> valueGetter) {
		final TreeTableColumn<TorrentFileEntry, Long> longValueColumn = new TreeTableColumn<TorrentFileEntry, Long>(columnName);
		longValueColumn.setId(columnName);
		longValueColumn.setGraphic(TableUtils.buildColumnHeader(longValueColumn, style));
		longValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Long>(propertyName));
		longValueColumn.setCellFactory(column -> new TreeTableCell<TorrentFileEntry, Long>() {
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
					
					final String formattedValue = valueGetter.apply(fileContent);					
					valueLabel.setText(formattedValue);
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER_RIGHT);
	                super.setPadding(padding);
				}
			}			
		});
		return longValueColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, Integer> buildPriorityColumn() {		
		final TreeTableColumn<TorrentFileEntry, Integer> priorityColumn =
				new TreeTableColumn<TorrentFileEntry, Integer>(PRIORITY_COLUMN_NAME);
		priorityColumn.setId(PRIORITY_COLUMN_NAME);
		priorityColumn.setGraphic(TableUtils.buildColumnHeader(priorityColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
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

					valueLabel.setText(FilePriority.valueOf(fileContent.priorityProperty().get()));
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.BASELINE_LEFT);
	                super.setPadding(GuiUtils.leftPadding());
				}
			}		
		});
		return priorityColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, String> buildPathColumn() {
		final TreeTableColumn<TorrentFileEntry, String> pathColumn =
				new TreeTableColumn<TorrentFileEntry, String>(PATH_COLUMN_NAME);
		pathColumn.setId(PATH_COLUMN_NAME);		
		pathColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, String>("path"));
		pathColumn.setGraphic(TableUtils.buildColumnHeader(pathColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		pathColumn.setVisible(false);		
		
		return pathColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, Double> buildProgressColumn() {
		final TreeTableColumn<TorrentFileEntry, Double> progressColumn = 
				new TreeTableColumn<TorrentFileEntry, Double>(PROGRESS_COLUMN_NAME);
		progressColumn.setId(PROGRESS_COLUMN_NAME);
		progressColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Double>("progress"));
		progressColumn.setGraphic(TableUtils.buildColumnHeader(progressColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		progressColumn.setCellFactory(column -> new ProgressBarTreeTableCell<TorrentFileEntry>() {			
			@Override
			public final void updateItem(final Double value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					super.setText(null);
					super.setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}
					
					super.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> 
						fileEntryTree.getSelectionModel().select(super.getTreeTableRow().getIndex()));
					
					super.getStyleClass().add("progress-bar-stopped");
					super.setItem(fileContent.progressProperty().doubleValue());
					super.setPadding(GuiUtils.noPadding());
				}
			}		
		});	
		
		return progressColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, FileNameColumnModel> buildFileNameColumn() {
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = 
				new TreeTableColumn<TorrentFileEntry, FileNameColumnModel>(NAME_COLUMN_NAME);
		fileNameColumn.setId(NAME_COLUMN_NAME);
		fileNameColumn.setGraphic(TableUtils.buildColumnHeader(fileNameColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		fileNameColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
		fileNameColumn.setEditable(true);
		fileNameColumn.setPrefWidth(GuiUtils.NAME_COLUMN_PREFERRED_SIZE);
		fileNameColumn.setCellValueFactory(p -> {
			final TreeItem<TorrentFileEntry> treeItem = p.getValue();
			final TorrentFileEntry fileEntry = p.getValue().getValue();
			final FileNameColumnModel columnModel = new FileNameColumnModel(
					treeItem.isLeaf(), fileEntry.nameProperty().get());
			return new ReadOnlyObjectWrapper<FileNameColumnModel>(columnModel);
		});			
		fileNameColumn.setCellFactory(column -> new CheckBoxTreeTableCell<
				TorrentFileEntry, FileNameColumnModel>() {	
			final Label fileNameLabel = new Label();
			
			@Override
			public final void updateItem(final FileNameColumnModel item, final boolean empty) {				
				super.updateItem(item, empty);			
				
				if(empty) {
					this.setText(null);
					this.setGraphic(null);
				}
				else {
					final TorrentFileEntry fileEntry = this.getTreeTableRow().getItem();					
					
					if(fileEntry == null) {
						return;
					}			
					
					final CheckBoxTreeItem<TorrentFileEntry> treeItem = 
							(CheckBoxTreeItem<TorrentFileEntry>)this.getTreeTableRow().getTreeItem();
					
					final Image image = treeItem.isLeaf()? fileEntry.getImage() : (treeItem.isExpanded()? 
							ImageUtils.FOLDER_OPENED_IMAGE: ImageUtils.FOLDER_CLOSED_IMAGE);
					
					final ImageView imageView = ImageUtils.colorImage(image, Color.DARKOLIVEGREEN, 
							ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, 
							ImageUtils.FILE_TYPE_IMAGE_SIZE, ImageUtils.FILE_TYPE_IMAGE_SIZE);
					
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
		return fileNameColumn;
	}
	
	private TreeItem<TorrentFileEntry> buildTorrentContentTree(
			final BinaryEncodedDictionary infoDictionary) {			
		TreeItem<TorrentFileEntry> root = null;		
		final BinaryEncodedInteger fileLength = (BinaryEncodedInteger)infoDictionary.get(
				BinaryEncodingKeyNames.KEY_LENGTH);
		final BinaryEncodedList files = (BinaryEncodedList)infoDictionary.get(
				BinaryEncodingKeyNames.KEY_FILES);
		final String fileName = infoDictionary.get(BinaryEncodingKeyNames.KEY_NAME).toString();
		
		if(fileLength != null) {
			//Handle single file torrent mode
			root = buildSingleFileTree(fileName, fileLength.getValue());
			
		}
		else if(files != null) {
			//Handle multiple files torrent mode
			root = buildMultiFileTree(fileName, files);
		}
		else {
			//TODO: Handle invalid torrent, no files found (show an error to the user)
			return null;
		}
		
		root.setExpanded(true);		
		return root;
	}
	
	private TreeItem<TorrentFileEntry> buildSingleFileTree(final String fileName, final long fileLength) {
		final TorrentFileEntry fileEntry = new TorrentFileEntry(fileName, ". (current path)", 
				fileLength, ImageUtils.getFileTypeImage(fileName));
		final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);		
		final CheckBoxTreeItem<TorrentFileEntry> root = new CheckBoxTreeItem<>(new TorrentFileEntry(
				"root", ". (current path)", fileLength, null));			
		root.getChildren().add(treeItem);
		
		return root;
	}
	
	private TreeItem<TorrentFileEntry> buildMultiFileTree(final String fileName, final BinaryEncodedList files) {			
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
		final CheckBoxTreeItem<TorrentFileEntry> treeItem = new CheckBoxTreeItem<>(fileEntry);
		treeItem.selectedProperty().bindBidirectional(fileEntry.selectedProperty());
		treeItem.setSelected(true);
		addTreeItemListener(treeItem);
		
		return treeItem;
	}
	
	private void addTreeItemListener(final CheckBoxTreeItem<TorrentFileEntry> treeItem) { 		
		treeItem.selectedProperty().addListener((observable, oldValue, newValue) -> {	
			if(treeItem.isLeaf()) {
				final long fileSize = treeItem.getValue().sizeProperty().get();
				final long affectedFileSize = (newValue? -fileSize: fileSize);
				selectedFilesSize.set(selectedFilesSize.get() - affectedFileSize);
			}			
			treeItem.getValue().priorityProperty().set(newValue? 
					FilePriority.NORMAL.getValue() : FilePriority.SKIP.getValue());
			
			fileEntryTree.getSelectionModel().select(treeItem);			
			fileEntryTree.requestFocus();
			
			final CheckBoxTreeItem<TorrentFileEntry> parentItem = (CheckBoxTreeItem<TorrentFileEntry>)treeItem.getParent();
			onUpdateParentPriority(parentItem);					
		});		
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