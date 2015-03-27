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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.io.codec.BinaryEncodedInteger;
import org.matic.torrent.io.codec.BinaryEncodedList;
import org.matic.torrent.io.codec.BinaryEncodingKeyNames;
import org.matic.torrent.queue.FilePriority;
import org.matic.torrent.utils.UnitConverter;

/**
 * This class implements a graphic tree representation of a torrent's contents. 
 * 
 * @author Vedran Matic
 * 
 */
public final class TorrentContentTree {

	private final TreeTableView<TorrentFileEntry> fileEntryTree;
	
	private final ObservableList<TorrentFileEntry> torrentFileEntries = 
			FXCollections.observableArrayList();
	
	private final LongProperty selectedFilesSize;
	
	private final BinaryEncodedInteger fileLength;
	private final BinaryEncodedList files;
	private final String fileName;
	
	/**
	 * Create and populate a file tree with a torrent's meta data
	 * 
	 * @param fileName Torrent's file (or directory) name 
	 * @param fileLength Length of the torrent file (in single file mode)
	 * @param files Files contained by the torrent (in multi file mode)
	 */
	public TorrentContentTree(final String fileName, final BinaryEncodedInteger fileLength, 
			final BinaryEncodedList files) {
		this.fileName = fileName;
		this.fileLength = fileLength;
		this.files = files;
		
		selectedFilesSize = new SimpleLongProperty();		 
		fileEntryTree = new TreeTableView<TorrentFileEntry>();
		initComponents();
	}
	
	/**
	 * Wrap the internal tree table in a scroll pane for easier navigation
	 * 
	 * @param wrapper Wrapping scroll pane
	 */
	public final void wrapWith(final ScrollPane wrapper) {
		wrapper.setContent(fileEntryTree);
	}
	
	/**
	 * Return the root file of a torrent (invisible)
	 * 
	 * @return
	 */
	public final TorrentFileEntry getRootFileEntry() {
		return fileEntryTree.getRoot().getValue();
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
	
	private void initComponents() {
		final TreeItem<TorrentFileEntry> rootNode = buildTorrentContentTree();		
		fileEntryTree.setRoot(rootNode);
		fileEntryTree.setTableMenuButtonVisible(true);
		fileEntryTree.setShowRoot(false);
		fileEntryTree.setEditable(true);					
		
		addColumns();
		fileEntryTree.setRowFactory(table -> new TorrentContentTreeRow(this));
		
		selectedFilesSize.set(getRootFileEntry().getSize());
	}
	
	private void addColumns() {
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = buildFileNameColumn();
		fileEntryTree.getColumns().addAll(Arrays.asList(fileNameColumn, 
				buildPathColumn(), buildSizeColumn(), buildPriorityColumn()));
		fileEntryTree.getSortOrder().add(fileNameColumn);
	}
	
	private TreeTableColumn<TorrentFileEntry, Long> buildSizeColumn() {
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
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER_RIGHT);
				}
			}			
		});
		return sizeColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, Integer> buildPriorityColumn() {
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

					valueLabel.setText(FilePriority.valueOf(fileContent.priorityProperty().get()));
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER);
				}
			}		
		});
		return priorityColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, String> buildPathColumn() {
		final TreeTableColumn<TorrentFileEntry, String> pathColumn = new TreeTableColumn<TorrentFileEntry, String>("Path");
		pathColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, String>("path"));
		pathColumn.setVisible(false);
		
		return pathColumn;
	}
	
	private TreeTableColumn<TorrentFileEntry, FileNameColumnModel> buildFileNameColumn() {
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
			return new ReadOnlyObjectWrapper<FileNameColumnModel>(columnModel);
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
		return fileNameColumn;
	}
	
	private TreeItem<TorrentFileEntry> buildTorrentContentTree() {			
		TreeItem<TorrentFileEntry> root = null;						
		
		if(fileLength != null) {
			//Handle single file torrent mode
			root = buildSingleFileTree(fileLength.getValue());
			
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