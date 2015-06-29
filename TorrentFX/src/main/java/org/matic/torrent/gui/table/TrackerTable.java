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

package org.matic.torrent.gui.table;

import java.util.Arrays;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TrackerView;

public class TrackerTable {

	private final TableView<TrackerView> trackerTable = new TableView<>();
	
	public TrackerTable() {
		initComponents();
	}
	
	public Node getView() {
		return trackerTable;
	}
	
	private void initComponents() {
		trackerTable.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		
		addColumns();
	}
	
	private void addColumns() {
		trackerTable.getColumns().addAll(Arrays.asList(buildTrackerUrlColumn()));
	}
	
	private TableColumn<TrackerView, String> buildTrackerUrlColumn() {
		final TableColumn<TrackerView, String> trackerNameColumn = new TableColumn<>("URL");		
		trackerNameColumn.setPrefWidth(350);
		trackerNameColumn.getStyleClass().add("left-aligned-column-header");
		trackerNameColumn.setCellValueFactory(tv -> tv.getValue().trackerNameProperty());
		return trackerNameColumn;
	}
}