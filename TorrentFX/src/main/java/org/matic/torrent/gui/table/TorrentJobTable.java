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
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import org.matic.torrent.codec.InfoHash;
import org.matic.torrent.gui.model.TorrentJobView;

/**
 * This is a graphical view (represented as a table) of current torrent jobs
 * 
 * @author vedran
 *
 */
public final class TorrentJobTable {

	private final TableView<TorrentJobView> torrentJobTable = new TableView<>();
	
	public TorrentJobTable() {		
		initComponents();
	}
	
	public void addSelectionListener(final Consumer<TorrentJobView> handler) {
		torrentJobTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
			handler.accept(newV));
	}
	
	public boolean contains(final InfoHash torrentInfoHash) {
		return torrentJobTable.getItems().stream().filter(
				tj -> torrentInfoHash.equals(tj.getInfoHash())).count() > 0;
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
	
	private void initComponents() {
		torrentJobTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		final Text emptyTorrentListPlaceholder = new Text("Go to 'File->Add Torrent...' to add torrents.");
		emptyTorrentListPlaceholder.getStyleClass().add("empty-torrent-list-text");
		emptyTorrentListPlaceholder.visibleProperty().bind(Bindings.isEmpty(torrentJobTable.getItems()));
		
		final BorderPane placeholderPane = new BorderPane();
		placeholderPane.setPadding(new Insets(15, 0, 0, 40));
		placeholderPane.setLeft(emptyTorrentListPlaceholder);
		
		torrentJobTable.setPlaceholder(placeholderPane);		
		addColumns();
		
	}
	
	private void addColumns() {
		torrentJobTable.getColumns().addAll(Arrays.asList(buildFileNameColumn()));
	}
	
	private TableColumn<TorrentJobView, String> buildFileNameColumn() {
		final TableColumn<TorrentJobView, String> fileNameColumn = new TableColumn<>("Name");
		fileNameColumn.setPrefWidth(350);
		fileNameColumn.setCellValueFactory(tj -> {
			return new ReadOnlyObjectWrapper<String>(tj.getValue().getFileName());
		});
		return fileNameColumn;
	}
}