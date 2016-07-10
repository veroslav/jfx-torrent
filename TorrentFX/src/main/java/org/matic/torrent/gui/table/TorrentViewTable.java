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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.utils.UnitConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This is a graphical view (represented as a table) of current torrent jobs
 * 
 * @author vedran
 *
 */
public final class TorrentViewTable {

	private static final String PRIORITY_COLUMN_LABEL = "#";
	private static final String NAME_COLUMN_LABEL = "Name";
	private static final String SIZE_COLUMN_LABEL = "Size";
	private static final String SELECTED_SIZE_COLUMN_LABEL = "Selected Size";
	private static final String ADDED_COLUMN_LABEL = "Added";
	private static final String TRACKER_COLUMN_LABEL = "Tracker";

	private final TableView<TorrentView> torrentJobTable = new TableView<>();
	
	public TorrentViewTable() {
		initComponents();
	}
	
	public void addSelectionListener(final Consumer<TorrentView> handler) {
		torrentJobTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
			handler.accept(newV));
	}
	
	public boolean contains(final InfoHash torrentInfoHash) {
		return torrentJobTable.getItems().contains(torrentInfoHash);
	}

    public void refresh() {
        TableUtils.refresh(torrentJobTable);
    }

    /**
     * Sort the table based on the current sort order and latest table entry values
     */
    public void sort() {
        final List<TableColumn<TorrentView, ?>> sortOrder = new ArrayList<>(torrentJobTable.getSortOrder());
        torrentJobTable.getSortOrder().clear();
        torrentJobTable.getSortOrder().addAll(sortOrder);
    }
	
	/**
	 * Create a binding that updates the target when this table becomes empty 
	 * 
	 * @return Generated boolean binding
	 */
	public BooleanBinding bindOnEmpty() {
		return Bindings.size(torrentJobTable.getItems()).isEqualTo(0);
	}
	
	public void addJob(final TorrentView torrentJob) {
		torrentJobTable.getItems().add(torrentJob);
		torrentJobTable.getSelectionModel().clearSelection();
		torrentJobTable.getSelectionModel().select(torrentJob);
	}
	
	public void deleteJobs(final ObservableList<TorrentView> torrentJobs) {
		torrentJobTable.getItems().removeAll(torrentJobs);		
	}
	
	public ObservableList<TorrentView> getSelectedJobs() {
		return torrentJobTable.getSelectionModel().getSelectedItems();
	}
	
	public void selectJob(final TorrentView torrentJob) {
        torrentJobTable.getSelectionModel().clearSelection();
		torrentJobTable.getSelectionModel().select(torrentJob);
	}
	
	public void wrapWith(final ScrollPane wrapper) {
		wrapper.setContent(torrentJobTable);
	}
	
	/**
	 * Store any changes to column order, visibility, and/or size
	 */
	public void storeColumnStates() {		
		TableUtils.storeColumnStates(torrentJobTable.getColumns(), GuiProperties.TORRENT_JOBS_TAB_COLUMN_VISIBILITY,
			GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES, GuiProperties.TORRENT_JOBS_TAB_COLUMN_SIZE,
			GuiProperties.DEFAULT_TORRENT_JOBS_COLUMN_SIZES, GuiProperties.TORRENT_JOBS_TAB_COLUMN_ORDER,
			GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER);		
	}
	
	private void initComponents() {
		torrentJobTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		torrentJobTable.setTableMenuButtonVisible(false);
		torrentJobTable.setRowFactory(t -> new TorrentViewTableRow<>());
		
		final Text emptyTorrentListPlaceholder = new Text("Go to 'File->Add Torrent...' to add torrents.");
		emptyTorrentListPlaceholder.getStyleClass().add(CssProperties.TORRENT_LIST_EMPTY_TEXT);
		emptyTorrentListPlaceholder.visibleProperty().bind(Bindings.isEmpty(torrentJobTable.getItems()));
		
		final BorderPane placeholderPane = new BorderPane();
		placeholderPane.getStyleClass().add(CssProperties.PLACEHOLDER_EMPTY);
		placeholderPane.setPadding(new Insets(15, 0, 0, 40));
		placeholderPane.setLeft(emptyTorrentListPlaceholder);
		
		torrentJobTable.setPlaceholder(placeholderPane);		
		createColumns();		
	}
	
	private void createColumns() {
		final LinkedHashMap<String, TableColumn<TorrentView, ?>> columnMappings = buildColumnMappings();
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {
			final TableColumn<TorrentView,?> tableColumn = columnMappings.get(columnId);
			torrentJobTable.getColumns().add(tableColumn);
			torrentJobTable.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());
		};
		final TableState<TorrentView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_TORRENT_JOBS_COLUMN_SIZES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(torrentJobTable.getColumns(), columnState, columnResizer);
	}
		
	private LinkedHashMap<String, TableColumn<TorrentView, ?>> buildColumnMappings() {
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> priorityValueFactory =
				tj -> tj.getValue().priorityProperty();
		final Callback<CellDataFeatures<TorrentView, String>, ObservableValue<String>> nameValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getFileName());
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> sizeValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getTotalLength());
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> selectedSizeValueFactory =
				tj -> tj.getValue().selectedLengthProperty();
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> addedValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getAddedOnTime());
		final Callback<CellDataFeatures<TorrentView, String>, ObservableValue<String>> trackerValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getTrackerUrl());

        final TableColumn<TorrentView, ?> priorityColumn = TableUtils.buildColumn(priorityValueFactory,
                val -> String.valueOf(val.getPriority()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PRIORITY_COLUMN_LABEL);
        torrentJobTable.getSortOrder().add(priorityColumn);

		final LinkedHashMap<String, TableColumn<TorrentView, ?>> columnMappings = new LinkedHashMap<>();
		columnMappings.put(PRIORITY_COLUMN_LABEL, priorityColumn);
		columnMappings.put(NAME_COLUMN_LABEL, TableUtils.buildColumn(nameValueFactory, tj -> tj.getFileName(),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_LABEL));
		columnMappings.put(SIZE_COLUMN_LABEL, TableUtils.buildColumn(sizeValueFactory, tj -> 
			UnitConverter.formatByteCount(tj.getTotalLength()),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SIZE_COLUMN_LABEL));
		columnMappings.put(SELECTED_SIZE_COLUMN_LABEL, TableUtils.buildColumn(selectedSizeValueFactory, tj -> 
			UnitConverter.formatByteCount(tj.getSelectedLength()),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SELECTED_SIZE_COLUMN_LABEL));
		columnMappings.put(ADDED_COLUMN_LABEL, TableUtils.buildColumn(addedValueFactory, tj -> 
			UnitConverter.formatMillisToDate(tj.getAddedOnTime(), TimeZone.getDefault()),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, ADDED_COLUMN_LABEL));
		columnMappings.put(TRACKER_COLUMN_LABEL, TableUtils.buildColumn(trackerValueFactory, tj -> tj.getTrackerUrl(),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, TRACKER_COLUMN_LABEL));
		
		return columnMappings;
	}
}