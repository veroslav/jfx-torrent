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

package org.matic.torrent.gui.action;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class TabActionHandler {
	
	public void onTabAction(final ObservableList<Tab> tabs, final List<String> tabNames,
			final TabPane tabPane, final Map<String, Tab> tabMap) {	
		final ContextMenu tabHeaderContextMenu = new ContextMenu();		
		final List<String> visibleTabNames = ApplicationPreferences.getCompositePropertyValues(
				GuiProperties.TAB_VISIBILITY, GuiProperties.DEFAULT_TAB_VISIBILITY);
				
		tabHeaderContextMenu.getItems().addAll(tabs.stream().map(t -> {
			final CheckMenuItem tabMenuItem = new CheckMenuItem(t.getText());	
			tabMenuItem.setId(t.getId());
			tabMenuItem.selectedProperty().addListener((obs, oldV, selected) -> {				
				if(selected && !tabs.contains(t)) {					
					if(tabs.size() == 1) {						
						final Tab remainingTab = tabs.get(0);						
						tabHeaderContextMenu.getItems().stream().filter(
								mi -> remainingTab.getId().equals(mi.getId())).forEach(mi -> mi.setDisable(false));											
					}
					final int insertedTabOrder = tabNames.indexOf(t.getText());
					int insertionIndex = 0;
					for(int i = 0; i < tabs.size(); ++i) {						
						if(tabNames.indexOf(tabs.get(i).getId()) > insertedTabOrder) {
							insertionIndex = i;
							break;
						}
						if(i == tabs.size() - 1) {
							insertionIndex = tabs.size();
						}
					}
					tabs.add(insertionIndex, t);
					if(tabs.size() > 1) {
						tabs.forEach(tb -> tb.getContextMenu().getItems().forEach(mi -> mi.setDisable(false)));	
					}
				}
				else if(!selected && tabs.contains(t)) {					
					tabs.remove(t);
					if(tabs.size() == 1) {						
						final Tab remainingTab = tabs.get(0);						
						tabHeaderContextMenu.getItems().stream().filter(mi -> 
							remainingTab.getId().equals(mi.getId())).forEach(mi -> mi.setDisable(true));								
					}
				}
			});
			final boolean tabVisible = visibleTabNames.contains(t.getId());
			Platform.runLater(() -> {
				if(!tabVisible) {
					tabs.remove(t);
					if(tabs.size() == 1) {
						final String singleTabId = tabs.get(0).getId();
						tabHeaderContextMenu.getItems().stream().filter(mi ->
						singleTabId.equals(mi.getId())).forEach(mi -> mi.setDisable(true));
					}
				}
			});
			tabMenuItem.setSelected(tabVisible);
			t.setContextMenu(tabHeaderContextMenu);
			return tabMenuItem;
		}).collect(Collectors.toList()));	
		
		final MenuItem resetTabsMenuItem = new MenuItem("_Reset");
		final List<CheckMenuItem> checkMenuItems = tabHeaderContextMenu.getItems().stream().filter(
				mi -> mi instanceof CheckMenuItem).map(mi -> (CheckMenuItem)mi).collect(Collectors.toList());
		resetTabsMenuItem.setOnAction(e -> onResetTabs(checkMenuItems, tabPane, tabMap));
		tabHeaderContextMenu.getItems().addAll(new SeparatorMenuItem(), resetTabsMenuItem);
	}
	
	private void onResetTabs(final List<CheckMenuItem> checkMenuItems,
			final TabPane tabPane, final Map<String, Tab> tabMap) {
		tabPane.getTabs().clear();
		final Set<String> defaultVisibleTabNames = Arrays.stream(GuiProperties.DEFAULT_TAB_VISIBILITY.split(
				GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR)).collect(Collectors.toSet());
		checkMenuItems.stream().filter(mi -> mi instanceof CheckMenuItem).forEach(cm -> {
			final CheckMenuItem tabVisibilityCheck = (CheckMenuItem)cm;
			tabVisibilityCheck.setDisable(false);
			final String tabId = tabVisibilityCheck.getId();			
			final boolean tabVisible = defaultVisibleTabNames.contains(tabId);				
			if(tabVisible) {
				tabPane.getTabs().add(tabMap.get(tabId));
			}
		});
		checkMenuItems.stream().forEach(cm -> cm.setSelected(defaultVisibleTabNames.contains(cm.getId())));		
	}
}