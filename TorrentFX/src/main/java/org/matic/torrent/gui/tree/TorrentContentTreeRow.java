/* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2016 Vedran Matic
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeTableRow;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.enums.FilePriority;

import java.util.Arrays;

public final class TorrentContentTreeRow extends TreeTableRow<TorrentFileEntry> {
	private final ContextMenu contextMenu = new ContextMenu();
	
	private final MenuItem collapseFolderTreeMenuItem = new MenuItem("Collapse Folder Tree");
	private final MenuItem expandFolderTreeMenuItem = new MenuItem("Expand Folder Tree");
	private final MenuItem selectNoneMenuItem = new MenuItem("Select None");						
	private final MenuItem selectAllMenuItem = new MenuItem("Select All");
	private final MenuItem unselectMenuItem = new MenuItem("Unselect");
	private final MenuItem selectMenuItem = new MenuItem("Select");
		
	private final Menu priorityTreeMenu = new Menu("Priority");
	
	private final ToggleGroup radioMenuGroup = new ToggleGroup();
	private final RadioMenuItem[] priorityMenuItems = {
			new RadioMenuItem(FilePriority.SKIP.toString()),
			new RadioMenuItem(FilePriority.LOWEST.toString()),
			new RadioMenuItem(FilePriority.LOW.toString()),
			new RadioMenuItem(FilePriority.NORMAL.toString()),
			new RadioMenuItem(FilePriority.HIGH.toString()),
			new RadioMenuItem(FilePriority.HIGHEST.toString())				
	};
	private final FileTreeViewer fileTreeViewer;
	
	public TorrentContentTreeRow(final FileTreeViewer fileTreeViewer) {
		this.fileTreeViewer = fileTreeViewer;
		
		for(int i = 0; i < priorityMenuItems.length; ++i) {
			priorityMenuItems[i].setId(String.valueOf(i));
			priorityMenuItems[i].setToggleGroup(radioMenuGroup);
		}
		
		priorityTreeMenu.getItems().addAll(priorityMenuItems[5], priorityMenuItems[4],
				priorityMenuItems[3], priorityMenuItems[2], priorityMenuItems[1], 						  
				new SeparatorMenuItem(), priorityMenuItems[0]);
		contextMenu.getItems().addAll(selectMenuItem, unselectMenuItem, new SeparatorMenuItem(), 
				selectAllMenuItem, selectNoneMenuItem, new SeparatorMenuItem(), 
				collapseFolderTreeMenuItem, expandFolderTreeMenuItem, new SeparatorMenuItem(), 
				priorityTreeMenu);

        selectMenuItem.setOnAction(evt -> onSelectAction());
        unselectMenuItem.setOnAction(evt -> onUnselectAction());
        selectAllMenuItem.setOnAction(evt -> fileTreeViewer.selectAllEntries());
        selectNoneMenuItem.setOnAction(evt -> fileTreeViewer.unselectAllEntries());
	}
				
	@Override
	protected void updateItem(final TorrentFileEntry item, final boolean empty) {					
		super.updateItem(item, empty);
		
		if(empty) {
            getStyleClass().removeAll(CssProperties.ALTERNATE_LIST_ROW_ODD);
            getStyleClass().add(CssProperties.ALTERNATE_LIST_ROW_EVEN);
            setContextMenu(null);
            return;
        } 
		else {				
			if(this.getIndex() % 2 != 0 && ApplicationPreferences.getProperty(
					GuiProperties.ALTERNATE_LIST_ROW_COLOR, false)) {
                getStyleClass().removeAll(CssProperties.ALTERNATE_LIST_ROW_EVEN);
				getStyleClass().add(CssProperties.ALTERNATE_LIST_ROW_ODD);                
            }
            else {
                getStyleClass().removeAll(CssProperties.ALTERNATE_LIST_ROW_ODD);
            	getStyleClass().add(CssProperties.ALTERNATE_LIST_ROW_EVEN);
            }
			
			final CheckBoxTreeItem<TorrentFileEntry> treeItem = (CheckBoxTreeItem<TorrentFileEntry>)super.getTreeItem();
			expandFolderTreeMenuItem.setDisable(treeItem.isLeaf() || treeItem.isExpanded());
			collapseFolderTreeMenuItem.setDisable((treeItem.isLeaf() && (treeItem.getParent() == 
					this.getTreeTableView().getRoot())) || (!treeItem.isLeaf() && !treeItem.isExpanded()));
			selectMenuItem.setDisable(treeItem.isLeaf() && treeItem.isSelected() ||
					!treeItem.isLeaf() && !treeItem.isIndeterminate() && treeItem.isSelected());
			unselectMenuItem.setDisable(treeItem.isLeaf() && !treeItem.isSelected() ||
					!treeItem.isLeaf() && !treeItem.isIndeterminate() && !treeItem.isSelected());
            setContextMenu(contextMenu);		                
            
            final FilePriority filePriority = item.getPriority();
            if(filePriority != FilePriority.MIXED) {
            	priorityMenuItems[item.getPriority().getValue()].setSelected(true);
            }
            else {
            	final Toggle selectedPriorityToggle = radioMenuGroup.getSelectedToggle();
            	if(selectedPriorityToggle != null) {
            		selectedPriorityToggle.setSelected(false);
            	}
            }
        }
		
		Arrays.asList(priorityMenuItems).stream().forEach(priorityMenuItem -> priorityMenuItem.setOnAction(
				event -> onPriorityAction(priorityMenuItem)));

		expandFolderTreeMenuItem.setOnAction(evt -> fileTreeViewer.onExpandFolderTree(getTreeItem()));
		collapseFolderTreeMenuItem.setOnAction(evt -> fileTreeViewer.onCollapseTreeItem(getTreeItem()));
	}
	
	private void onUnselectAction() {
		final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)getTreeItem();
		if(checkBoxItem.isIndeterminate()) {
			checkBoxItem.setSelected(true);
		}
		getItem().setSelected(false);
		selectMenuItem.setDisable(false);
	}
	
	private void onSelectAction() {
		final CheckBoxTreeItem<TorrentFileEntry> checkBoxItem = (CheckBoxTreeItem<TorrentFileEntry>)getTreeItem();
		if(checkBoxItem.isIndeterminate()) {
			checkBoxItem.setSelected(false);
		}
		getItem().setSelected(true);
		unselectMenuItem.setDisable(false);
	}
	
	private void onPriorityAction(final RadioMenuItem priorityMenuItem) {
		final CheckBoxTreeItem<TorrentFileEntry> treeItem = (CheckBoxTreeItem<TorrentFileEntry>)getTreeItem();
		final FilePriority newPriorityValue = FilePriority.values()[Integer.parseInt(priorityMenuItem.getId())];
		if(treeItem.getValue().getPriority() != newPriorityValue) { 
			if(treeItem.isIndeterminate()) {
				treeItem.setSelected(true);
				treeItem.getValue().selectedProperty().set(newPriorityValue != FilePriority.SKIP);
			}
			else {
				treeItem.setSelected(newPriorityValue != FilePriority.SKIP);
			}
								
			if(!treeItem.isLeaf()) {
				fileTreeViewer.onUpdateChildrenPriority(treeItem, newPriorityValue);
			}
			treeItem.getValue().priorityProperty().set(newPriorityValue);
			fileTreeViewer.onUpdateParentPriority(treeItem.getParent());
			fileTreeViewer.selectItem(treeItem);
		}
	}
}
