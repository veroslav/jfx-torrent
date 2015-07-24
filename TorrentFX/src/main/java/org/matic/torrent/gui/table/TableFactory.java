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

import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import org.matic.torrent.gui.GuiUtils;

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
	 * Build a table column that will contain a simple string value
	 * 
	 * @param cellValueFactory How to set the cell values
	 * @param columnWidth Preferred column width
	 * @param alignmentStyle Column content alignment
	 * @param columnName Column name displayed in the column header
	 * @return Built column
	 */
	public static <T> TableColumn<T, String> buildSimpleStringColumn(
			final Callback<CellDataFeatures<T, String>, ObservableValue<String>> cellValueFactory,
			final int columnWidth, final String alignmentStyle, final String columnName) {
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
					valueLabel.setText(value);
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
	 * @param cellValueFactory How to set the cell values
	 * @param valueConverter Function that converts number to string representation
	 * @param columnWidth Preferred column width
	 * @param alignmentStyle Column content alignment
	 * @param columnName Column name displayed in the column header
	 * @return Built column
	 */
	public static <T> TableColumn<T, Number> buildSimpleNumberColumn(
			final Callback<CellDataFeatures<T, Number>, ObservableValue<Number>> cellValueFactory,
			final Function<Number, String> valueConverter, final int columnWidth, 
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
					valueLabel.setText(valueConverter.apply(value));
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
}