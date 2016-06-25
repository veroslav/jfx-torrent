/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
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
package org.matic.torrent.gui.table;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Window;
import javafx.util.Callback;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.gui.window.AddTrackerWindow;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.TorrentStatus;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.utils.UnitConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrackerTable {	
	
	private static final long MIN_USER_REQUESTED_ANNOUNCE_DELAY = 60000;	//60 s
	
	//Tracker column header names
	private static final String MIN_INTERVAL_COLUMN_NAME = "Min Interval";
	private static final String DOWNLOADED_COLUMN_NAME = "Downloaded";
	private static final String UPDATE_IN_COLUMN_NAME = "Update In";	
	private static final String INTERVAL_COLUMN_NAME = "Interval";
	private static final String STATUS_COLUMN_NAME = "Status";
	private static final String SEEDS_COLUMN_NAME = "Seeds";
	private static final String PEERS_COLUMN_NAME = "Peers";
	private static final String NAME_COLUMN_NAME = "Name";
	
	//Context menu commands	
	private static final String ENABLE_LOCAL_PEER_DISCOVERY = "Use _Local Peer Discovery";
	private static final String ENABLE_PEER_EXCHANGE = "Use _Peer Exchange";
	private static final String ENABLE_DHT = "Use _DHT";
	
	private static final String REMOVE_TRACKER = "_Remove Tracker";
	private static final String UPDATE_TRACKER = "_Update Tracker";
	private static final String ADD_TRACKER = "_Add Tracker...";
	
	//Context menu items
	private final MenuItem removeTrackerMenuItem = new MenuItem(REMOVE_TRACKER);
	private final MenuItem updateTrackerMenuItem = new MenuItem(UPDATE_TRACKER);
	private final MenuItem addTrackerMenuItem = new MenuItem(ADD_TRACKER);

	private final TableView<TrackerView> trackerTable = new TableView<>();
	
	public TrackerTable() {
		initComponents();
		createColumns();
		createContextMenu();
	}
	
	public void setContent(final List<TrackerView> trackerViews) {
		trackerTable.getItems().clear();		
		trackerTable.getItems().addAll(trackerViews);
	}
	
	/**
	 * Update tracker view beans with the latest tracker statistics
	 */
	public void updateContent() {
		trackerTable.getItems().forEach(TrackerView::update);
	}
	
	public boolean addTracker(final TrackerView trackerView) {
		final ObservableList<TrackerView> tableItems = trackerTable.getItems();
		if(tableItems.contains(trackerView)) {
			return false;
		}
		return tableItems.add(trackerView);
	}
	
	public boolean removeTracker(final TrackerView trackerView) {
		return trackerTable.getItems().remove(trackerView);
	}
	
	public void wrapWith(final ScrollPane wrapper) {
		wrapper.setContent(trackerTable);
	}
	
	/**
	 * Register a handler for tracker deletion events
	 * 
	 * @param handler Target handler
	 */
	public final void onTrackerDeletionRequested(final Consumer<Collection<TrackerView>> handler) {	
		final Runnable deleter = () -> 
			handler.accept(getDeletableTrackers(trackerTable.getSelectionModel().getSelectedItems()));
		removeTrackerMenuItem.setOnAction(e -> deleter.run());
		trackerTable.setOnKeyReleased(e -> {
			if(e.getCode().equals(KeyCode.DELETE)) {
				deleter.run();
			}
		});
	}
	
	/**
	 * Register a handler for tracker updating events
	 * 
	 * @param handler Target handler
	 */
	public final void onTrackerUpdateRequested(final Consumer<Collection<TrackerView>> handler) {
		updateTrackerMenuItem.setOnAction(e -> {			
			final Collection<TrackerView> updatableTrackers =
					getUpdatableTrackers(trackerTable.getSelectionModel().getSelectedItems());
			handler.accept(updatableTrackers);
			final long currentTime = System.currentTimeMillis();
			updatableTrackers.forEach(tv -> tv.setLastTrackerResponse(currentTime));
		});
	}
	
	/**
	 * Register a handler for tracker additions
	 * 
	 * @param handler Target handler
	 * @param addTrackerWindowOwner Add torrent window owner
	 * @param disablementBinding When to disable the tracker addition
	 */
	public final void onTrackersAdded(final Consumer<Collection<String>> handler,
			final Window addTrackerWindowOwner, final BooleanBinding disablementBinding) {
		addTrackerMenuItem.disableProperty().bind(disablementBinding);
		addTrackerMenuItem.setOnAction(e -> {
			final AddTrackerWindow addTrackerWindow = new AddTrackerWindow(addTrackerWindowOwner);
			final Collection<String> trackerUrls = addTrackerWindow.showAndWait();
			handler.accept(trackerUrls);
		});
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
		trackerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		trackerTable.setTableMenuButtonVisible(false);
		
		addTrackerMenuItem.setId(ADD_TRACKER);
	}
	
	private void createContextMenu() {		
		final ContextMenu contextMenu = new ContextMenu();
		
		removeTrackerMenuItem.setId(REMOVE_TRACKER);
		removeTrackerMenuItem.setDisable(true);
		
		updateTrackerMenuItem.setId(UPDATE_TRACKER);
		updateTrackerMenuItem.setDisable(true);
		
		final CheckMenuItem enableDhtMenuItem = new CheckMenuItem(ENABLE_DHT);
		enableDhtMenuItem.setId(ENABLE_DHT);
		enableDhtMenuItem.setSelected(true);
		
		final CheckMenuItem enableLocalPeerDiscoveryMenuItem = new CheckMenuItem(ENABLE_LOCAL_PEER_DISCOVERY);
		enableLocalPeerDiscoveryMenuItem.setId(ENABLE_LOCAL_PEER_DISCOVERY);
		enableLocalPeerDiscoveryMenuItem.setSelected(true);
		
		final CheckMenuItem enablePeerExchangeMenuItem = new CheckMenuItem(ENABLE_PEER_EXCHANGE);
		enablePeerExchangeMenuItem.setId(ENABLE_PEER_EXCHANGE);
		enablePeerExchangeMenuItem.setSelected(true);
		
		contextMenu.getItems().addAll(updateTrackerMenuItem, removeTrackerMenuItem, new SeparatorMenuItem(),
				addTrackerMenuItem, enableDhtMenuItem, enableLocalPeerDiscoveryMenuItem,
				enablePeerExchangeMenuItem);
		contextMenu.showingProperty().addListener(obs -> {			
			removeTrackerMenuItem.setDisable(true);
			updateTrackerMenuItem.setDisable(true);
		});
		trackerTable.setContextMenu(contextMenu);		
		trackerTable.setRowFactory(table -> {			
			final TableRow<TrackerView> tableRow = new TableRow<>();
			tableRow.setContextMenu(contextMenu);			
			tableRow.setOnContextMenuRequested(cme -> {	
				final TrackerView trackerView = tableRow.getItem();
				if(trackerView == null) {
					return;
				}
				final Collection<TrackerView> deletableTrackers = getDeletableTrackers(
						trackerTable.getSelectionModel().getSelectedItems());
				removeTrackerMenuItem.setDisable(deletableTrackers.isEmpty());
				removeTrackerMenuItem.setText(deletableTrackers.size() > 1? REMOVE_TRACKER + "s" : REMOVE_TRACKER);
							
				final Collection<TrackerView> updatableTrackers = getUpdatableTrackers(
						trackerTable.getSelectionModel().getSelectedItems());
				updateTrackerMenuItem.setDisable(updatableTrackers.isEmpty());
				updateTrackerMenuItem.setText(updatableTrackers.size() > 1? UPDATE_TRACKER + "s" : UPDATE_TRACKER);
			});
			
			return tableRow;
		});
	}
	
	private Collection<TrackerView> getUpdatableTrackers(final Collection<TrackerView> selectedRows) {
		return selectedRows.stream().filter(tv -> {
			final long currentTime = System.currentTimeMillis();
			return tv.getTorrentStatus() == TorrentStatus.ACTIVE && tv.getNextUpdate() > 0 &&
					tv.getStatus().equals(Tracker.getStatusMessage(Tracker.Status.WORKING)) &&
					((currentTime - tv.getLastTrackerResponse()) >= tv.getMinInterval()) &&
					(currentTime - tv.getLastUserRequestedUpdate() > MIN_USER_REQUESTED_ANNOUNCE_DELAY);
		}).collect(Collectors.toList());			
	}
	
	private Collection<TrackerView> getDeletableTrackers(final Collection<TrackerView> selectedRows) {
		return selectedRows.stream().filter(tv ->
			!(tv.getTrackerName().equals("[DHT]") ||
					tv.getTrackerName().equals("[Local Peer Discovery]") ||
					tv.getTrackerName().equals("[Peer Exchange]"))
		).collect(Collectors.toList());
	}
	
	private void createColumns() {									
		final LinkedHashMap<String, TableColumn<TrackerView, ?>> columnMappings = buildColumnMappings();
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {				
			final TableColumn<TrackerView,?> tableColumn = columnMappings.get(columnId);						
			trackerTable.getColumns().add(tableColumn);
			trackerTable.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());			
		};
		final TableState<TrackerView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.TRACKER_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_VISIBILITIES,
				GuiProperties.TRACKER_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_SIZES,
				GuiProperties.TRACKER_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_TRACKER_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(trackerTable.getColumns(), columnState, columnResizer);
	}
	
	private LinkedHashMap<String, TableColumn<TrackerView, ?>> buildColumnMappings() {
		final Function<TrackerView, String> updateInValueConverter = tv -> {			
			if(tv.getTorrent().getStatus() == TorrentStatus.STOPPED) {
				return "";
			}
			final long nextUpdateValue = tv.getNextUpdate();			
			
			if(nextUpdateValue < 1000 || (nextUpdateValue > 0 && "".equals(tv.getStatus()))) {
				return Tracker.STATUS_UPDATING_MESSAGE;					
			}		
			else {
				return UnitConverter.formatMillisToTime(nextUpdateValue);
			}
		}; 		
		
		final Function<TrackerView, String> intervalValueConverter = tv -> {
			final long interval = tv.getInterval(); 
			return (tv.getTorrent().getStatus() != TorrentStatus.STOPPED) && (interval > 0)?
					UnitConverter.formatMillisToTime(interval) : "";
		};
		
		final Function<TrackerView, String> minIntervalValueConverter =
                tv -> tv.getTorrent().getStatus() != TorrentStatus.STOPPED?
					UnitConverter.formatMillisToTime(tv.getMinInterval()) : "";
		
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
				TrackerView::getTrackerName, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_NAME));
		columnMappings.put(STATUS_COLUMN_NAME, TableUtils.buildColumn(statusValueFactory,
				TrackerView::getStatus, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, STATUS_COLUMN_NAME));
		columnMappings.put(UPDATE_IN_COLUMN_NAME, TableUtils.buildColumn(nextUpdateValueFactory,
				updateInValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, UPDATE_IN_COLUMN_NAME));
		columnMappings.put(INTERVAL_COLUMN_NAME, TableUtils.buildColumn(intervalValueFactory,
				intervalValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, INTERVAL_COLUMN_NAME));
		columnMappings.put(MIN_INTERVAL_COLUMN_NAME, TableUtils.buildColumn(minIntervalValueFactory,
				minIntervalValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, MIN_INTERVAL_COLUMN_NAME));
        columnMappings.put(SEEDS_COLUMN_NAME, TableUtils.buildColumn(seedsValueFactory,
                val -> String.valueOf(val.getSeeds()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SEEDS_COLUMN_NAME));
        columnMappings.put(PEERS_COLUMN_NAME, TableUtils.buildColumn(peersValueFactory,
                val -> String.valueOf(val.getLeechers()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PEERS_COLUMN_NAME));
        columnMappings.put(DOWNLOADED_COLUMN_NAME, TableUtils.buildColumn(downloadedValueFactory, val ->
                String.valueOf(val.getDownloaded()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, DOWNLOADED_COLUMN_NAME));
		
		return columnMappings;
	}	
}