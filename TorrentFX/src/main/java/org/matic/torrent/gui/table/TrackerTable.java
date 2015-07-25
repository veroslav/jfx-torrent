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
import java.util.List;

import javafx.scene.Node;
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
	
	public final void setContent(final List<TrackerView> trackerViews) {
		trackerTable.getItems().clear();
		trackerTable.getItems().addAll(trackerViews);
	}
	
	public final List<TrackerView> getTrackerViews() {
		return trackerTable.getItems();
	}
	
	private void initComponents() {
		trackerTable.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		trackerTable.setTableMenuButtonVisible(true);
		
		addColumns();
	}
	
	private void addColumns() {		
		trackerTable.getColumns().addAll(Arrays.asList(
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().trackerNameProperty(), GuiUtils.NAME_COLUMN_PREFERRED_SIZE,
					GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Name"),
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().statusProperty(), 140, 
					GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Status"),
			TableFactory.buildSimpleStringColumn(
					tv -> tv.getValue().nextUpdateProperty(), 120, 
					GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Update In"),
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().intervalProperty(),
					70, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Interval"),
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().minIntervalProperty(),
					90, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Min Interval"),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().seedsProperty(), 
					val -> String.valueOf(val), 70, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Seeds"),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().leechersProperty(),
					val -> String.valueOf(val), 70, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Peers"),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().downloadedProperty(),
					val -> String.valueOf(val), 90, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, "Downloaded")));
	}
}