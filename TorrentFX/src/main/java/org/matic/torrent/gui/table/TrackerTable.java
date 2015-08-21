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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.utils.UnitConverter;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class TrackerTable {	
	
	private static final String MIN_INTERVAL_COLUMN_NAME = "Min Interval";
	private static final String DOWNLOADED_COLUMN_NAME = "Downloaded";
	private static final String UPDATE_IN_COLUMN_NAME = "Update In";	
	private static final String INTERVAL_COLUMN_NAME = "Interval";
	private static final String STATUS_COLUMN_NAME = "Status";
	private static final String SEEDS_COLUMN_NAME = "Seeds";
	private static final String PEERS_COLUMN_NAME = "Peers";
	private static final String NAME_COLUMN_NAME = "Name";

	private final TableView<TrackerView> trackerTable = new TableView<>();
	
	public TrackerTable() {
		initComponents();
	}
	
	//TODO: Don't expose TableView, unless necessary (doesn't appear to be the case)
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
	
	/**
	 * Sort the table based on the current sort order and latest table entry values
	 */
	public final void sort() {
		final List<TableColumn<TrackerView, ?>> sortOrder = new ArrayList<>(trackerTable.getSortOrder());
		trackerTable.getSortOrder().clear();
		trackerTable.getSortOrder().addAll(sortOrder);
	}
	
	/**
	 * Store any changes to column order, visibility, and/or size
	 */
	public void storeColumnStates() {		
		TableUtils.storeColumnStates(trackerTable.getColumns(), GuiProperties.TRACKER_TAB_COLUMN_VISIBILITY,
			GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_VISIBILITIES, GuiProperties.TRACKER_TAB_COLUMN_SIZE,
			GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_SIZES, GuiProperties.TRACKER_TAB_COLUMN_ORDER,
			GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_ORDER);		
	}
	
	private void initComponents() {
		trackerTable.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		trackerTable.setTableMenuButtonVisible(false);
		
		createColumns();		
	}
	
	private void createColumns() {									
		final LinkedHashMap<String, TableColumn<TrackerView, ?>> columnMappings = buildColumnMappings();
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {				
			final TableColumn<TrackerView,?> tableColumn = columnMappings.get(columnId);						
			trackerTable.getColumns().add(tableColumn);
			trackerTable.resizeColumn(tableColumn, targetWidth- tableColumn.getWidth());			
		};
		final TableState<TrackerView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.TRACKER_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_VISIBILITIES,
				GuiProperties.TRACKER_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_SIZES,
				GuiProperties.TRACKER_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(trackerTable.getColumns(), columnState, columnResizer);
	}
	
	private LinkedHashMap<String, TableColumn<TrackerView, ?>> buildColumnMappings() {
		final Function<TrackerView, String> updateInValueConverter = tv -> {			
			if(tv.getTorrentState() == QueuedTorrent.State.STOPPED) {
				return "";
			}
			final long nextUpdateValue = tv.getNextUpdate();
			if(nextUpdateValue < 1000) {
				return Tracker.getStatusMessage(Tracker.Status.UPDATING);					
			}
			else {
				return UnitConverter.formatMillisToTime(nextUpdateValue);
			}			
		}; 		
		
		final Function<TrackerView, String> intervalValueConverter = tv -> {
			final long interval = tv.getInterval(); 
			return (tv.getTorrentState() != QueuedTorrent.State.STOPPED) && (interval > 0)?
					UnitConverter.formatMillisToTime(interval) : "";
		};
		
		final Function<TrackerView, String> minIntervalValueConverter = tv -> {
			return tv.getTorrentState() != QueuedTorrent.State.STOPPED? 
					UnitConverter.formatMillisToTime(tv.getMinInterval()) : "";
		};
		
		final Callback<CellDataFeatures<TrackerView, String>, ObservableValue<String>> nameValueFactory =
				tv -> tv.getValue().trackerNameProperty();
		final Callback<CellDataFeatures<TrackerView, String>, ObservableValue<String>> statusValueFactory =
				tv -> tv.getValue().statusProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> nextUpdateValueFactory =
				tv -> tv.getValue().nextUpdateProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> intervalValueFactory =
				tv -> tv.getValue().intervalProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> minIntervalValueFactory =
				tv -> tv.getValue().minIntervalProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> seedsValueFactory =
				tv -> tv.getValue().seedsProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> peersValueFactory =
				tv -> tv.getValue().leechersProperty();
		final Callback<CellDataFeatures<TrackerView, Number>, ObservableValue<Number>> downloadedValueFactory =
				tv -> tv.getValue().downloadedProperty();
				
		final LinkedHashMap<String, TableColumn<TrackerView, ?>> columnMappings = new LinkedHashMap<>();
		columnMappings.put(NAME_COLUMN_NAME, TableUtils.buildColumn(nameValueFactory,
				tv -> tv.getTrackerName(), GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_NAME));
		columnMappings.put(STATUS_COLUMN_NAME, TableUtils.buildColumn(statusValueFactory,
				tv -> tv.getStatus(), GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, STATUS_COLUMN_NAME));
		columnMappings.put(UPDATE_IN_COLUMN_NAME, TableUtils.buildColumn(nextUpdateValueFactory,
				updateInValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, UPDATE_IN_COLUMN_NAME));
		columnMappings.put(INTERVAL_COLUMN_NAME, TableUtils.buildColumn(intervalValueFactory,
				intervalValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, INTERVAL_COLUMN_NAME));
		columnMappings.put(MIN_INTERVAL_COLUMN_NAME, TableUtils.buildColumn(minIntervalValueFactory,
				minIntervalValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, MIN_INTERVAL_COLUMN_NAME));
		columnMappings.put(SEEDS_COLUMN_NAME, TableUtils.buildColumn(seedsValueFactory,
				val -> String.valueOf(val.getSeeds()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SEEDS_COLUMN_NAME));
		columnMappings.put(PEERS_COLUMN_NAME, TableUtils.buildColumn(peersValueFactory, val ->
			String.valueOf(val.getLeechers()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PEERS_COLUMN_NAME));
		columnMappings.put(DOWNLOADED_COLUMN_NAME, TableUtils.buildColumn(downloadedValueFactory, val ->
			String.valueOf(val.getDownloaded()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, DOWNLOADED_COLUMN_NAME));
		
		return columnMappings;
	}	
}