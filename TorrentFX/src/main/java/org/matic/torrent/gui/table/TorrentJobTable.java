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

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TorrentJobView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.preferences.GuiProperties;

import javafx.beans.binding.Bindings;
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

/**
 * This is a graphical view (represented as a table) of current torrent jobs
 * 
 * @author vedran
 *
 */
public final class TorrentJobTable {
	
	private static final String PRIORITY_COLUMN_NAME = "#";
	private static final String NAME_COLUMN_NAME = "Name";

	private final TableView<TorrentJobView> torrentJobTable = new TableView<>();
	
	public TorrentJobTable() {		
		initComponents();
	}
	
	public void addSelectionListener(final Consumer<TorrentJobView> handler) {
		torrentJobTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
			handler.accept(newV));
	}
	
	public boolean contains(final InfoHash torrentInfoHash) {
		return torrentJobTable.getItems().contains(torrentInfoHash);
	}
	
	public void addJob(final TorrentJobView torrentJob) {
		torrentJobTable.getItems().add(torrentJob);
		torrentJobTable.getSelectionModel().clearSelection();
		torrentJobTable.getSelectionModel().select(torrentJob);
	}
	
	public void deleteJobs(final ObservableList<TorrentJobView> torrentJobs) {
		torrentJobTable.getItems().removeAll(torrentJobs);		
	}
	
	public ObservableList<TorrentJobView> getSelectedJobs() {
		return torrentJobTable.getSelectionModel().getSelectedItems();
	}
	
	public void selectJob(final TorrentJobView torrentJob) {
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
		
		final Text emptyTorrentListPlaceholder = new Text("Go to 'File->Add Torrent...' to add torrents.");
		emptyTorrentListPlaceholder.getStyleClass().add("empty-torrent-list-text");
		emptyTorrentListPlaceholder.visibleProperty().bind(Bindings.isEmpty(torrentJobTable.getItems()));
		
		final BorderPane placeholderPane = new BorderPane();
		placeholderPane.setPadding(new Insets(15, 0, 0, 40));
		placeholderPane.setLeft(emptyTorrentListPlaceholder);
		
		torrentJobTable.setPlaceholder(placeholderPane);		
		createColumns();		
	}
	
	private void createColumns() {
		final LinkedHashMap<String, TableColumn<TorrentJobView, ?>> columnMappings = buildColumnMappings();
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {
			final TableColumn<TorrentJobView,?> tableColumn = columnMappings.get(columnId);
			torrentJobTable.getColumns().add(tableColumn);
			torrentJobTable.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());
		};
		final TableState<TorrentJobView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_TORRENT_JOBS_COLUMN_SIZES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(torrentJobTable.getColumns(), columnState, columnResizer);
	}
		
	private LinkedHashMap<String, TableColumn<TorrentJobView, ?>> buildColumnMappings() {			
		final Callback<CellDataFeatures<TorrentJobView, Number>, ObservableValue<Number>> priorityValueFactory = 
				tj -> tj.getValue().priorityProperty();
		final Callback<CellDataFeatures<TorrentJobView, String>, ObservableValue<String>> nameValueFactory =
				tj -> new ReadOnlyObjectWrapper<String>(tj.getValue().getFileName());
		
		final LinkedHashMap<String, TableColumn<TorrentJobView, ?>> columnMappings = new LinkedHashMap<>();
		columnMappings.put(PRIORITY_COLUMN_NAME, TableUtils.buildColumn(priorityValueFactory, 
				val -> String.valueOf(val.getPriority()), GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PRIORITY_COLUMN_NAME));
		columnMappings.put(NAME_COLUMN_NAME, TableUtils.buildColumn(nameValueFactory, tj -> tj.getFileName(),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_NAME));
		return columnMappings;
	}
}