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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.utils.UnitConverter;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
	
	public final void sort() {
		final List<TableColumn<TrackerView, ?>> sortOrder = new ArrayList<>(trackerTable.getSortOrder());
		trackerTable.getSortOrder().clear();
		trackerTable.getSortOrder().addAll(sortOrder);
	}
	
	private void initComponents() {
		trackerTable.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		trackerTable.setTableMenuButtonVisible(false);
		
		createColumns();		
	}
	
	private void createColumns() {	
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
		
		trackerTable.getColumns().addAll(Arrays.asList(
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().trackerNameProperty(),
					tv -> tv.getTrackerName(), GuiUtils.NAME_COLUMN_PREFERRED_SIZE,
					GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_NAME),
			TableFactory.buildSimpleStringColumn(tv -> tv.getValue().statusProperty(),
					tv -> tv.getStatus(), 140, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, STATUS_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().nextUpdateProperty(),
					updateInValueConverter, 120, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, UPDATE_IN_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().intervalProperty(),
					intervalValueConverter, 70, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, INTERVAL_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().minIntervalProperty(),
					minIntervalValueConverter, 90, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, MIN_INTERVAL_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().seedsProperty(), 
					val -> String.valueOf(val.getSeeds()), 70, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SEEDS_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().leechersProperty(),
					val -> String.valueOf(val.getLeechers()), 70, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PEERS_COLUMN_NAME),
			TableFactory.buildSimpleNumberColumn(tv -> tv.getValue().downloadedProperty(),
					val -> String.valueOf(val.getDownloaded()), 90, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, DOWNLOADED_COLUMN_NAME)));
		
		TableFactory.addHeaderContextMenus(trackerTable.getColumns());
	}
}