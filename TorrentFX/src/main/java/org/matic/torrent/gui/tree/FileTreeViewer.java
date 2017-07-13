/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.FileTree;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.queue.QueuedFileMetaData;
import org.matic.torrent.queue.enums.FilePriority;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class implements a graphic tree representation of a torrent's contents.
 *
 * @author Vedran Matic
 *
 */
public final class FileTreeViewer {

	private final Map<InfoHash, TreeItem<TorrentFileEntry>> fileViews = new HashMap<>();

	private final TreeTableView<TorrentFileEntry> defaultTree;
	private TreeTableView<TorrentFileEntry> currentTree;

	/**
	 * Create a viewer with a tree renderer that will display the tree data
	 *
	 * @param defaultTree Tree view renderer
	 */
	public FileTreeViewer(final TreeTableView<TorrentFileEntry> defaultTree) {
		this.defaultTree = this.currentTree = defaultTree;
	}

	/**
	 * Switch back to the default tree view renderer
	 */
	public void restoreDefault() {
		currentTree = defaultTree;
	}

	/**
	 * Add a new view to be managed
	 *
	 * @param id Matching torrent's info hash (used as the identifier)
	 * @param view View to add
	 */
	public void attach(final InfoHash id, final TreeItem<TorrentFileEntry> view) {
		fileViews.put(id, view);
	}

	/**
	 * Remove a managed view
	 *
	 * @param id Target torrent's info hash (used as the identifier)
	 */
	public void detach(final InfoHash id) {
		fileViews.remove(id);
	}

	/**
	 * Set current tree view
	 *
	 * @param id Target torrent's info hash (used as the identifier)
	 */
	public void show(final InfoHash id) {
		currentTree.setRoot(fileViews.get(id));
	}

	/**
	 * Hide current view contents
	 */
	public void hide() {
		currentTree.setRoot(null);
	}

	/**
	 * Handler for selection of all torrent file entries
	 */
	public void selectAllEntries() {
		final int selectedIndex = currentTree.getSelectionModel().getSelectedIndex();
		currentTree.getRoot().getChildren().forEach(child -> child.getValue().selectedProperty().set(true));
		currentTree.getSelectionModel().select(selectedIndex);
	}

	/**
	 * Handler for deselection of all torrent file entries
	 */
	public void unselectAllEntries() {
		final int selectedIndex = currentTree.getSelectionModel().getSelectedIndex();
		currentTree.getRoot().getChildren().forEach(child -> {
			final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)child;
			if(checkBoxItem.isIndeterminate()) {
				checkBoxItem.setSelected(true);
			}
			child.getValue().selectedProperty().set(false);
		});
		currentTree.getSelectionModel().select(selectedIndex);
	}

	/**
	 * Handler for collapsing of all torrent file entries
	 */
	public void collapseAll() {
		currentTree.getRoot().getChildren().forEach(this::onCollapseFolderTree);
	}

	/**
	 * Handler for expansion of all torrent file entries
	 */
	public void expandAll() {
		onExpandFolderTree(currentTree.getRoot());
	}

	/**
	 * Handler for collapsing of a tree item
	 *
	 * @param targetItem Collapsed tree item
	 */
	protected void onCollapseTreeItem(final TreeItem<TorrentFileEntry> targetItem) {
		TreeItem<TorrentFileEntry> treeItem = targetItem;
		if(treeItem.isLeaf()) {
			final TreeItem<TorrentFileEntry> leafParent = treeItem.getParent();
			if(leafParent != currentTree.getRoot()) {
				treeItem = leafParent;
			}
			else {
				return;
			}
		}
		onCollapseFolderTree(treeItem);
	}

	/**
	 * Handler for a tree item expansion
	 *
	 * @param treeItem Expanded tree item
	 */
	protected void onExpandFolderTree(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(this::onExpandFolderTree);
		treeItem.setExpanded(true);
	}

	/**
	 * Select a tree item
	 *
	 * @param treeItem Tree item to select
	 */
	protected void selectItem(final CheckBoxTreeItem<TorrentFileEntry> treeItem) {
		currentTree.getSelectionModel().select(treeItem);
	}

	/**
	 * Update the parent(s) when a child's priority changes
	 *
	 * @param treeItem A child for which the priority has been changed
	 */
	protected void onUpdateParentPriority(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem == currentTree.getRoot()) {
			return;
		}
		final List<FilePriority> priorities = treeItem.getChildren().stream().map(child ->
			child.getValue().getPriority()).distinct().collect(Collectors.toList());

        if(!priorities.isEmpty()) {
            treeItem.getValue().setPriority(priorities.size() > 1 ?
                    FilePriority.MIXED : priorities.get(0));
        }

		onUpdateParentPriority(treeItem.getParent());
	}

	/**
	 * Update the children when their parent's priority changes
	 *
	 * @param parent Parent for which the priority has been changed
	 * @param priority New priority
	 */
	protected void onUpdateChildrenPriority(final TreeItem<TorrentFileEntry> parent, final FilePriority priority) {
		parent.getChildren().forEach(c -> {
			final CheckBoxTreeItem<TorrentFileEntry> child = (CheckBoxTreeItem<TorrentFileEntry>)c;
			if(!child.isLeaf()) {
				onUpdateChildrenPriority(child, priority);
			}
			final boolean isSelected = child.isSelected();
			child.setSelected(!isSelected && priority != FilePriority.SKIP ||
					isSelected && priority == FilePriority.SKIP || isSelected);
			child.getValue().setPriority(priority);
		});
	}

	private void onCollapseFolderTree(final TreeItem<TorrentFileEntry> treeItem) {
		if(treeItem.isLeaf()) {
			return;
		}
		treeItem.getChildren().forEach(this::onCollapseFolderTree);
		treeItem.setExpanded(false);
	}

	/**
	 * Create a file tree view from a torrent's file tree.
	 *
	 * @param fileTreeView Action handler tree for the created view.
	 * @param fileTree A bean containing the torrent's meta data and progress.
	 * @return A view to the torrent's data.
	 */
	public TreeItem<TorrentFileEntry> createTreeView(final TreeTableView<TorrentFileEntry> fileTreeView,
                                                     final FileTree fileTree) {
		currentTree = fileTreeView;
		final TreeItem<TorrentFileEntry> root;

		if(fileTree.isSingleFile()) {
			//Handle single file torrent mode
			root = buildSingleFileTree(fileTree);

		}
		else {
			//Handle multiple files torrent mode
			root = buildMultiFileTree(fileTree);
		}

		root.setExpanded(true);
		return root;
	}

	private TreeItem<TorrentFileEntry> buildSingleFileTree(final FileTree fileTree) {
		final String fileName = fileTree.getName();
		final long fileLength = fileTree.getLength();
		final TorrentFileEntry fileEntry = new TorrentFileEntry(fileName, ". (current path)",
				fileLength, true, ImageUtils.getFileTypeImage(fileName));
		fileEntry.setPieceCount((long)Math.ceil(fileLength / fileTree.getPieceLength()));
        fileEntry.setFirstPiece(0);

		final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);
		final CheckBoxTreeItem<TorrentFileEntry> root = new CheckBoxTreeItem<>(new TorrentFileEntry(
				"root", ". (current path)", fileLength, true, null));
		root.getChildren().add(treeItem);

        final Path filePath = Paths.get(fileName);

        fileEntry.selectedProperty().addListener((obs, oldV, newV) -> {
            root.getValue().updateSelectionLength(newV? fileLength : -fileLength);
            fileTree.setFilePriority(filePath, newV? FilePriority.NORMAL : FilePriority.SKIP, false);
        });

        fileEntry.priorityProperty().addListener((obs, oldV, newV) ->
                fileTree.setFilePriority(filePath, newV, true));

		final FilePriority filePriority = fileTree.getFilePriority(filePath);
		if(filePriority == FilePriority.SKIP || filePriority == FilePriority.MIXED) {
			fileEntry.setSelected(false);
		}

		return root;
	}

	private TreeItem<TorrentFileEntry> buildMultiFileTree(final FileTree fileTree) {
		final String fileName = fileTree.getName();
		final TorrentFileEntry fileDirEntry = new TorrentFileEntry(fileName, ". (current path)", 0L, false , null);
		final CheckBoxTreeItem<TorrentFileEntry> fileDirTreeItem = new CheckBoxTreeItem<>(fileDirEntry);
		final TorrentEntryNode<TreeItem<TorrentFileEntry>> fileDirNode = new TorrentEntryNode<>(fileName, fileDirTreeItem);

		fileDirTreeItem.selectedProperty().bindBidirectional(fileDirEntry.selectedProperty());

		long fileLengthRead = 0;

		//Iterate through all the files contained by this torrent
        final List<QueuedFileMetaData> fileMetaDatas = fileTree.getFiles();
        for(final QueuedFileMetaData fileMetaData : fileMetaDatas) {
            final long fileLength = fileMetaData.getLength();
            final Path filePath = fileMetaData.getPath();

			TorrentEntryNode<TreeItem<TorrentFileEntry>> currentNode = fileDirNode;
			final StringBuilder pathBuilder = new StringBuilder("./");
			final int filePathSize = filePath.getNameCount();

			//Process all directories on the file path
			for(int i = 0; i < filePathSize - 1; ++i) {
				final String pathName = filePath.getName(i).toString();
				pathBuilder.append(pathName);
				if(!currentNode.contains(pathName)) {
					final TorrentFileEntry fileEntry = new TorrentFileEntry(pathName, pathBuilder.toString(), 0L, false, null);
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
			final String leafName = filePath.getName(filePathSize-1).toString();

            final FilePriority filePriority = fileTree.getFilePriority(filePath);
            final boolean isSelected = filePriority != FilePriority.SKIP && filePriority != FilePriority.MIXED;

			final TorrentFileEntry fileEntry = new TorrentFileEntry(leafName, pathBuilder.toString(),
					fileLength, !isSelected, ImageUtils.getFileTypeImage(leafName));

			fileEntry.setFirstPiece((long)Math.floor(((double)fileLengthRead) / fileTree.getPieceLength()));
			fileLengthRead += fileLength;
			fileEntry.setPieceCount((long)Math.ceil(((double)fileLength) / fileTree.getPieceLength()));

			final TreeItem<TorrentFileEntry> treeItem = initTreeItem(fileEntry);
			currentNode.getData().getChildren().add(treeItem);

			final TorrentEntryNode<TreeItem<TorrentFileEntry>> childNode = new TorrentEntryNode<>(leafName, treeItem);
			currentNode.add(childNode);

			//Update file sizes for all of the childNode's parents in the tree
			final Consumer<CheckBoxTreeItem<TorrentFileEntry>> fileSizePropagation = item -> {
				final TorrentFileEntry parentEntry = item.getValue();
				parentEntry.updateLength(fileEntry.getLength());
                if(fileEntry.isSelected()) {
                    parentEntry.updateSelectionLength(fileEntry.getLength());
                }
				parentEntry.setPieceCount((long)Math.ceil(((double)parentEntry.getLength()) / fileTree.getPieceLength()));
                fileEntry.selectedProperty().addListener((obs, oldV, newV) -> {
                    parentEntry.updateSelectionLength(newV? fileLength : -fileLength);
                    fileTree.setFilePriority(filePath, newV? FilePriority.NORMAL : FilePriority.SKIP, false);
                });
			};
			applyOnParents(treeItem, fileSizePropagation);

            fileEntry.priorityProperty().addListener((obs, oldV, newV) ->
                    fileTree.setFilePriority(filePath, newV, true));
            fileEntry.setSelected(isSelected);
            fileEntry.setPriority(filePriority);
		}
		return fileDirTreeItem;
	}

	private TreeItem<TorrentFileEntry> initTreeItem(final TorrentFileEntry fileEntry) {
		final CheckBoxTreeItem<TorrentFileEntry> treeItem = new CheckBoxTreeItem<>(fileEntry);
		treeItem.selectedProperty().bindBidirectional(fileEntry.selectedProperty());

		treeItem.setSelected(fileEntry.isSelected());
        addTreeItemListener(treeItem);

		return treeItem;
	}

	private void addTreeItemListener(final CheckBoxTreeItem<TorrentFileEntry> treeItem) {
		treeItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if(treeItem.isLeaf()) {
				treeItem.getValue().setSelected(newValue);
			}
			treeItem.getValue().setPriority(newValue? FilePriority.NORMAL : FilePriority.SKIP);

			currentTree.getSelectionModel().select(treeItem);
			currentTree.requestFocus();

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