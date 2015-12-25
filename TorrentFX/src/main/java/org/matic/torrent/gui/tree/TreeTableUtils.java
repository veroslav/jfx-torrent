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

package org.matic.torrent.gui.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.table.TableState;
import org.matic.torrent.gui.table.TableUtils;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.FilePriority;
import org.matic.torrent.utils.UnitConverter;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.ProgressBarTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class TreeTableUtils {

	private static final String FIRST_PIECE_COLUMN_NAME = "First Piece";
	private static final String PIECE_COUNT_COLUMN_NAME = "#Pieces";
	private static final String PROGRESS_COLUMN_NAME = "Progress";
	private static final String PRIORITY_COLUMN_NAME = "Priority";
	private static final String PATH_COLUMN_NAME = "Path";
	private static final String NAME_COLUMN_NAME = "Name";
	private static final String SIZE_COLUMN_NAME = "Size";
	private static final String DONE_COLUMN_NAME = "Done";
	
	public static <T> void sort(final TreeTableView<T> treeTableView) {
		final List<TreeTableColumn<T, ?>> sortOrder = new ArrayList<>(treeTableView.getSortOrder());
		treeTableView.getSortOrder().clear();
		treeTableView.getSortOrder().addAll(sortOrder);
	}
	
	public static void setupFileListingView(final TreeTableView<TorrentFileEntry> fileView,
			final FileTreeViewer fileTreeViewer) {
		fileView.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		fileView.setTableMenuButtonVisible(false);
		fileView.setShowRoot(false);
		fileView.setEditable(true);
		fileView.setRowFactory(table -> new TorrentContentTreeRow(fileTreeViewer));
	}
	
	public static void addFileListingViewColumns(final TreeTableView<TorrentFileEntry> treeTableView, final boolean expandedMode) {
		final LinkedHashMap<String, TreeTableColumn<TorrentFileEntry, ?>> columnMappings = new LinkedHashMap<>();
		
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = buildFileNameColumn();
		columnMappings.put(NAME_COLUMN_NAME, fileNameColumn);
		columnMappings.put(PATH_COLUMN_NAME, buildPathColumn());
		columnMappings.put(SIZE_COLUMN_NAME, buildSimpleLongValueColumn(SIZE_COLUMN_NAME, "size", 
						GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(), 
						tfe -> UnitConverter.formatByteCount(tfe.sizeProperty().get())));
		columnMappings.put(PRIORITY_COLUMN_NAME, buildPriorityColumn());
		
		if(expandedMode) {
			columnMappings.put(DONE_COLUMN_NAME, buildSimpleLongValueColumn(DONE_COLUMN_NAME, "done",
					GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(),
							tfe -> UnitConverter.formatByteCount(tfe.doneProperty().get())));
			columnMappings.put(FIRST_PIECE_COLUMN_NAME,buildSimpleLongValueColumn(FIRST_PIECE_COLUMN_NAME, "firstPiece",
							GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, GuiUtils.rightPadding(),
							tfe -> String.valueOf(tfe.firstPieceProperty().get())));
			columnMappings.put(PIECE_COUNT_COLUMN_NAME, buildSimpleLongValueColumn(
							PIECE_COUNT_COLUMN_NAME, "pieceCount", GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME,
							GuiUtils.rightPadding(),tfe -> String.valueOf(tfe.pieceCountProperty().get())));
			columnMappings.put(PROGRESS_COLUMN_NAME, buildProgressColumn(treeTableView));
		}		
		
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {				
			final TreeTableColumn<TorrentFileEntry,?> tableColumn = columnMappings.get(columnId);						
			treeTableView.getColumns().add(tableColumn);
			treeTableView.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());			
		};
		
		final TableState<TreeItem<TorrentFileEntry>> columnState = expandedMode?
				TableUtils.loadColumnStates(columnMappings, columnResizer,
					GuiProperties.INFO_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_INFO_TAB_COLUMN_VISIBILITIES,
					GuiProperties.INFO_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_INFO_TAB_COLUMN_SIZES,
					GuiProperties.INFO_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_INFO_TAB_COLUMN_ORDER) :					
				TableUtils.loadColumnStates(columnMappings, columnResizer,
						GuiProperties.INFO_COLUMN_VISIBILITY, GuiProperties.DEFAULT_INFO_COLUMN_VISIBILITIES,
						GuiProperties.INFO_COLUMN_SIZE, GuiProperties.DEFAULT_INFO_COLUMN_SIZES,
						GuiProperties.INFO_COLUMN_ORDER, GuiProperties.DEFAULT_INFO_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(treeTableView.getColumns(), columnState, columnResizer);		
		treeTableView.getSortOrder().add(fileNameColumn);
	}
	
	private static TreeTableColumn<TorrentFileEntry, Long> buildSimpleLongValueColumn(
			final String columnName, final String propertyName, final String style, final Insets padding,
			final Function<TorrentFileEntry, String> valueGetter) {
		final TreeTableColumn<TorrentFileEntry, Long> longValueColumn = new TreeTableColumn<TorrentFileEntry, Long>(columnName);
		longValueColumn.setId(columnName);
		longValueColumn.setGraphic(TableUtils.buildColumnHeader(longValueColumn, style));
		longValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Long>(propertyName));
		longValueColumn.setCellFactory(column -> new TreeTableCell<TorrentFileEntry, Long>() {
			final Label valueLabel = new Label();			
			
			@Override
			protected final void updateItem(final Long value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}
					
					final String formattedValue = valueGetter.apply(fileContent);					
					valueLabel.setText(formattedValue);
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.CENTER_RIGHT);
	                super.setPadding(padding);
				}
			}			
		});
		return longValueColumn;
	}
	
	private static TreeTableColumn<TorrentFileEntry, Integer> buildPriorityColumn() {		
		final TreeTableColumn<TorrentFileEntry, Integer> priorityColumn =
				new TreeTableColumn<TorrentFileEntry, Integer>(PRIORITY_COLUMN_NAME);
		priorityColumn.setId(PRIORITY_COLUMN_NAME);
		priorityColumn.setGraphic(TableUtils.buildColumnHeader(priorityColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		priorityColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Integer>("priority"));
		priorityColumn.setCellFactory(column -> new TreeTableCell<TorrentFileEntry, Integer>() {
			final Label valueLabel = new Label();			
			@Override
			protected final void updateItem(final Integer value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}

					valueLabel.setText(FilePriority.valueOf(fileContent.priorityProperty().get()));
	                this.setGraphic(valueLabel);
	                this.setAlignment(Pos.BASELINE_LEFT);
	                super.setPadding(GuiUtils.leftPadding());
				}
			}		
		});
		return priorityColumn;
	}
	
	private static TreeTableColumn<TorrentFileEntry, String> buildPathColumn() {
		final TreeTableColumn<TorrentFileEntry, String> pathColumn =
				new TreeTableColumn<TorrentFileEntry, String>(PATH_COLUMN_NAME);
		pathColumn.setId(PATH_COLUMN_NAME);		
		pathColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, String>("path"));
		pathColumn.setGraphic(TableUtils.buildColumnHeader(pathColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));		
		
		return pathColumn;
	}
	
	private static TreeTableColumn<TorrentFileEntry, Double> buildProgressColumn(
			final TreeTableView<TorrentFileEntry> treeTableView) {
		final TreeTableColumn<TorrentFileEntry, Double> progressColumn = 
				new TreeTableColumn<TorrentFileEntry, Double>(PROGRESS_COLUMN_NAME);
		progressColumn.setId(PROGRESS_COLUMN_NAME);
		progressColumn.setCellValueFactory(new TreeItemPropertyValueFactory<TorrentFileEntry, Double>("progress"));
		progressColumn.setGraphic(TableUtils.buildColumnHeader(progressColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		progressColumn.setCellFactory(column -> new ProgressBarTreeTableCell<TorrentFileEntry>() {			
			@Override
			public final void updateItem(final Double value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					super.setText(null);
					super.setGraphic(null);
				}
				else {
					final TorrentFileEntry fileContent = this.getTreeTableRow().getItem();
					
					if(fileContent == null) {
						return;
					}
					
					super.addEventFilter(MouseEvent.MOUSE_CLICKED, evt ->
						treeTableView.getSelectionModel().select(super.getTreeTableRow().getIndex()));
					
					super.getStyleClass().add("progress-bar-stopped");
					super.setItem(fileContent.progressProperty().doubleValue());
					super.setPadding(GuiUtils.noPadding());
				}
			}		
		});	
		
		return progressColumn;
	}
	
	private static TreeTableColumn<TorrentFileEntry, FileNameColumnModel> buildFileNameColumn() {
		final TreeTableColumn<TorrentFileEntry, FileNameColumnModel> fileNameColumn = 
				new TreeTableColumn<TorrentFileEntry, FileNameColumnModel>(NAME_COLUMN_NAME);
		fileNameColumn.setId(NAME_COLUMN_NAME);
		fileNameColumn.setGraphic(TableUtils.buildColumnHeader(fileNameColumn, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME));
		fileNameColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
		fileNameColumn.setEditable(true);
		fileNameColumn.setPrefWidth(GuiUtils.NAME_COLUMN_PREFERRED_SIZE);
		fileNameColumn.setCellValueFactory(p -> {
			final TreeItem<TorrentFileEntry> treeItem = p.getValue();
			final TorrentFileEntry fileEntry = p.getValue().getValue();
			final FileNameColumnModel columnModel = new FileNameColumnModel(
					treeItem.isLeaf(), fileEntry.nameProperty().get());
			return new ReadOnlyObjectWrapper<FileNameColumnModel>(columnModel);
		});			
		fileNameColumn.setCellFactory(column -> new CheckBoxTreeTableCell<
				TorrentFileEntry, FileNameColumnModel>() {	
			final Label fileNameLabel = new Label();
			
			@Override
			public final void updateItem(final FileNameColumnModel item, final boolean empty) {				
				super.updateItem(item, empty);			
				
				if(empty) {
					this.setText(null);
					this.setGraphic(null);
				}
				else {
					final TorrentFileEntry fileEntry = this.getTreeTableRow().getItem();					
					
					if(fileEntry == null) {
						return;
					}			
					
					final CheckBoxTreeItem<TorrentFileEntry> treeItem = 
							(CheckBoxTreeItem<TorrentFileEntry>)this.getTreeTableRow().getTreeItem();
					
					final Image image = treeItem.isLeaf()? fileEntry.getImage() : (treeItem.isExpanded()? 
							ImageUtils.FOLDER_OPENED_IMAGE: ImageUtils.FOLDER_CLOSED_IMAGE);
					
					final ImageView imageView = ImageUtils.createImageView(image,
							ImageUtils.CROPPED_MARGINS_IMAGE_VIEW,
							ImageUtils.FILE_TYPE_IMAGE_SIZE, ImageUtils.FILE_TYPE_IMAGE_SIZE);
					
					final CheckBox selectionCheckBox = new CheckBox();					
					selectionCheckBox.setFocusTraversable(false);
					selectionCheckBox.setSelected(fileEntry.selectedProperty().get());					
					selectionCheckBox.selectedProperty().bindBidirectional(fileEntry.selectedProperty());
					selectionCheckBox.setIndeterminate(treeItem.isIndeterminate());
					
					treeItem.indeterminateProperty().bindBidirectional(
							selectionCheckBox.indeterminateProperty());																					
					
					fileNameLabel.setText(fileEntry.nameProperty().get());
					ImageUtils.colorize(imageView, Color.DARKOLIVEGREEN);
					fileNameLabel.setGraphic(imageView);
					
					final HBox checkBoxPane = new HBox();			
					checkBoxPane.getChildren().addAll(selectionCheckBox, fileNameLabel);					
	                setGraphic(checkBoxPane);
				}
			}			
		});
		fileNameColumn.setComparator((m, o) -> {
			if(!m.isLeaf() && o.isLeaf()) {
				return 1;
			}
			if(m.isLeaf() && !o.isLeaf()) {
				return -1;
			}
			return o.getName().compareTo(m.getName());
		});
		return fileNameColumn;
	}
}
