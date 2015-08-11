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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matic.torrent.gui.GuiUtils;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

/**
 * This class is used for building a table's columns and headers
 * 
 * @author vedran
 *
 */
public final class TableFactory {

	/**
	 * Build a styled column header
	 * 
	 * @param column Target column
	 * @param style Target header style
	 * @return Built header
	 */
	public static Node buildColumnHeader(final TableColumnBase<?, ?> column, final String style) {
	    final Label columnNameLabel = new Label(column.getText());
	    columnNameLabel.getStyleClass().add(style);
	  
	    final StackPane columnHeaderNode = new StackPane();
	    columnHeaderNode.getChildren().add(columnNameLabel);
	    columnHeaderNode.prefWidthProperty().bind(column.widthProperty().subtract(5));
	    columnNameLabel.prefWidthProperty().bind(columnHeaderNode.prefWidthProperty());
	    
	    return columnHeaderNode;
	}
	
	/**
	 * Add context menu to table columns
	 * 
	 * @param tableColumns Target table columns for context menu addition
	 */
	public static <T> void addHeaderContextMenus(final ObservableList<? extends TableColumnBase<T, ?>> tableColumns) {		
		final ContextMenu headerContextMenu = new ContextMenu();		
		tableColumns.forEach(c -> {
			final CheckMenuItem columnVisibleMenuItem = new CheckMenuItem(c.getText());			
			headerContextMenu.getItems().add(columnVisibleMenuItem);
			c.visibleProperty().bind(columnVisibleMenuItem.selectedProperty());
			columnVisibleMenuItem.setSelected(true);
			c.setContextMenu(headerContextMenu);
			
			columnVisibleMenuItem.selectedProperty().addListener((obs, oldV, selected) -> {
				final List<? extends TableColumnBase<T, ?>> visibleColumnHeaders = TableFactory.getVisibleColumnHeaders(tableColumns);
				if(!selected && visibleColumnHeaders.size() == 1) {
					final Optional<MenuItem> lastSelectedMenuItem = headerContextMenu.getItems().stream().filter(
							mi -> ((CheckMenuItem)mi).isSelected()).findFirst();
					if(lastSelectedMenuItem.isPresent()) {
						lastSelectedMenuItem.get().setDisable(true);
					}
				}
				else if(selected && visibleColumnHeaders.size() == 2) {
					headerContextMenu.getItems().stream().filter(mi ->
						((CheckMenuItem)mi).isSelected()).forEach(mi -> mi.setDisable(false));
				}
			});
		});
		//TODO: Implement Reset functionality
		headerContextMenu.getItems().addAll(new SeparatorMenuItem(), new MenuItem("Reset"));
	}
	
	//TODO: Combine buildSimple[Number | String]Column to a single method (U = String, Number)
	
	/**
	 * Build a table column that will contain a simple string value
	 * 
	 * @param <T> Type of data represented by the table
	 * @param cellValueFactory How to set the cell values
	 * @param valueConverter Function that converts T to string representation
	 * @param columnWidth Preferred column width
	 * @param alignmentStyle Column content alignment
	 * @param columnName Column name displayed in the column header
	 * @return Built column
	 */
	public static <T> TableColumn<T, String> buildSimpleStringColumn(
			final Callback<CellDataFeatures<T, String>, ObservableValue<String>> cellValueFactory,
			final Function<T, String> valueConverter, final int columnWidth, final String alignmentStyle,
			final String columnName) {
		final TableColumn<T, String> stringColumn = new TableColumn<>(columnName);
		stringColumn.setGraphic(TableFactory.buildColumnHeader(
				stringColumn, alignmentStyle));
		stringColumn.setPrefWidth(columnWidth);
		stringColumn.setCellValueFactory(cellValueFactory);
		stringColumn.setCellFactory(column -> new TableCell<T, String>() {
			final Label valueLabel = new Label();
			
			@Override
			protected final void updateItem(final String value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					final T item = this.getTableView().getItems().get(this.getTableRow().getIndex());					
					
					valueLabel.setText(valueConverter.apply(item));					
	                this.setGraphic(valueLabel);
	                
	                if(GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME.equals(alignmentStyle)) {
	                	this.setAlignment(Pos.BASELINE_RIGHT);
		                super.setPadding(GuiUtils.rightPadding());
	                }
	                else {
		                this.setAlignment(Pos.BASELINE_LEFT);
		                super.setPadding(GuiUtils.leftPadding());
	                }
				}
			}
		});
		return stringColumn;
	}
	
	/**
	 * Build a table column that will contain a simple number value
	 * 
	 * @param <T> Type of data represented by the table
	 * @param cellValueFactory How to set the cell values
	 * @param valueConverter Function that converts T to string representation
	 * @param columnWidth Preferred column width
	 * @param alignmentStyle Column content alignment
	 * @param columnName Column name displayed in the column header
	 * @return Built column
	 */
	public static <T> TableColumn<T, Number> buildSimpleNumberColumn(
			final Callback<CellDataFeatures<T, Number>, ObservableValue<Number>> cellValueFactory,
			final Function<T, String> valueConverter, final int columnWidth, 
			final String alignmentStyle, final String columnName) {
		final TableColumn<T, Number> numberColumn = new TableColumn<T, Number>(columnName);
		numberColumn.setGraphic(TableFactory.buildColumnHeader(numberColumn, alignmentStyle));
		numberColumn.setPrefWidth(columnWidth);
		numberColumn.setCellValueFactory(cellValueFactory);
		numberColumn.setCellFactory(column -> new TableCell<T, Number>() {
			final Label valueLabel = new Label();			
			
			@Override
			protected final void updateItem(final Number value, final boolean empty) {
				super.updateItem(value, empty);
				
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					if(this.getTableRow().getItem() == null) {
						return;
					}	
					
					final T item = this.getTableView().getItems().get(this.getTableRow().getIndex());
					
					valueLabel.setText(valueConverter.apply(item));
	                this.setGraphic(valueLabel);
	                
	                if(GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME.equals(alignmentStyle)) {
		                this.setAlignment(Pos.CENTER_RIGHT);
		                super.setPadding(GuiUtils.rightPadding());
	                }
	                else {
	                	this.setAlignment(Pos.BASELINE_LEFT);
		                super.setPadding(GuiUtils.leftPadding());
	                }
				}
			}			
		});
		return numberColumn;
	}
	
	private static final <T> List<? extends TableColumnBase<T, ?>> getVisibleColumnHeaders(
			final ObservableList<? extends TableColumnBase<T, ?>> tableColumns) {
		return tableColumns.stream().filter(TableColumnBase::isVisible).collect(Collectors.toList());
	}
}