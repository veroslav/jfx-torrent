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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;

import javafx.application.Platform;
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
public final class TableUtils {

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
	    columnHeaderNode.prefWidthProperty().bind(column.widthProperty().subtract(20));
	    columnNameLabel.prefWidthProperty().bind(columnHeaderNode.prefWidthProperty());
	    
	    return columnHeaderNode;
	}
	
	/**
	 * Store any changes to column order, visibility, and size
	 * 
	 * @param <T> Type of the view object stored in the table
	 * @param columns Target columns
	 * @param columnVisibilityProperty
	 * @param defaultColumnVisibilities
	 * @param columnSizeProperty
	 * @param defaultColumnSizes
	 * @param columnOrderProperty
	 * @param defaultColumnOrder
	 */
	public static <T> void storeColumnStates(final List<? extends TableColumnBase<T, ?>> columns,
			final String columnVisibilityProperty, final String defaultColumnVisibilities,
			final String columnSizeProperty, final String defaultColumnSizes,
			final String columnOrderProperty, final String defaultColumnOrder) {		
		final String visibility = columns.stream().map(c -> String.valueOf(c.isVisible())).collect(
				Collectors.joining(GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR));
		final String sizes = columns.stream().map(c -> String.valueOf(c.getWidth())).collect(
				Collectors.joining(GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR));
		final String order = columns.stream().map(c -> c.getId()).collect(
				Collectors.joining(GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR));
		
		final String oldVisibleColumns = ApplicationPreferences.getProperty(
				columnVisibilityProperty, defaultColumnVisibilities);
		final String oldColumnSizes = ApplicationPreferences.getProperty(
				columnSizeProperty, defaultColumnSizes);
		final String oldColumnOrder = ApplicationPreferences.getProperty(
				columnOrderProperty, defaultColumnOrder);
		
		if(!visibility.equals(oldVisibleColumns)) {
			ApplicationPreferences.setProperty(columnVisibilityProperty, visibility);
		}		
		if(!sizes.equals(oldColumnSizes)) {
			ApplicationPreferences.setProperty(columnSizeProperty, sizes);
		}
		if(!order.equals(oldColumnOrder)) {
			ApplicationPreferences.setProperty(columnOrderProperty, order);
		}		
	}
	
	/**
	 * Load and apply previously stored column changes, including order, visibility, and size 
	 * 
	 * @param <T> Type of the view object stored in the table
	 * @param columnMappings Mapping between columns and their id:s
	 * @param columnResizer Restorer of column sizes and order
	 * @param columnVisibilityProperty
	 * @param defaultColumnVisibilityValues
	 * @param columnSizeProperty
	 * @param defaultColumnSizeValues
	 * @param columnOrderProperty
	 * @return Loaded column states
	 */
	public static <T> TableState<T> loadColumnStates(final LinkedHashMap<String, ? extends TableColumnBase<T, ?>> columnMappings,
			final BiConsumer<String, Double> columnResizer,
			final String columnVisibilityProperty, final String defaultColumnVisibilityValues,
			final String columnSizeProperty, final String defaultColumnSizeValues,
			final String columnOrderProperty, final String defaultColumnOrderValues) {
		
		//Previously stored column state changes
		final List<String> columnVisibility = ApplicationPreferences.getCompositePropertyValues(
				columnVisibilityProperty, defaultColumnVisibilityValues);
		final List<Double> columnSizes = ApplicationPreferences.getCompositePropertyValues(
				columnSizeProperty, defaultColumnSizeValues)
				.stream().map(Double::parseDouble).collect(Collectors.toList());
		final List<String> columnOrder = ApplicationPreferences.getCompositePropertyValues(
				columnOrderProperty, defaultColumnOrderValues);
		
		//Default column states
		final List<String> defaultColumnOrder = Arrays.stream(defaultColumnOrderValues.split(
				GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR)).collect(Collectors.toList());
		
		final List<Boolean> defaultColumnsVisibilities = Arrays.stream(defaultColumnVisibilityValues.split(
				GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR)).map(Boolean::parseBoolean).collect(Collectors.toList());
		
		final List<Double> defaultColumnSizes = Arrays.stream(defaultColumnSizeValues.split(
				GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR)).map(Double::parseDouble).collect(Collectors.toList());
		
		final Map<String, Boolean> columnVisibilityMapping = new HashMap<>();
		final Map<String, Double> defaultColumnSizeMapping = new HashMap<>();		
		final Set<String> defaultVisibleColumnNames = new HashSet<>();
		
		for(int i = 0; i < defaultColumnOrder.size(); ++i) {
			final String columnId = defaultColumnOrder.get(i);
			if(defaultColumnsVisibilities.get(i)) {
				defaultVisibleColumnNames.add(columnId);
			}
			defaultColumnSizeMapping.put(columnId, defaultColumnSizes.get(i));
			columnMappings.get(columnId).setPrefWidth(defaultColumnSizes.get(i));
		}
		
		for(int i = 0; i < columnOrder.size(); ++i) {
			final String columnId = columnOrder.get(i);			
			columnResizer.accept(columnId, columnSizes.get(i));			
			columnVisibilityMapping.put(columnId, Boolean.parseBoolean(columnVisibility.get(i)));
		}
		
		final TableState<T> columnStates = new TableState<T>(columnMappings, columnVisibilityMapping,
				defaultVisibleColumnNames, defaultColumnSizeMapping, defaultColumnOrderValues);
		
		return columnStates;
	}
	
	/**
	 * Add context menu to table columns
	 * 
	 * @param <T> Type of the view object stored in the table
	 * @param columns Table columns
	 * @param tableState Loaded column states
	 * @param columnResizer Resetter of columns' widths and order
	 */
	public static <T> void addTableHeaderContextMenus(final ObservableList<? extends TableColumnBase<T, ?>> columns,
			final TableState<T> tableState, final BiConsumer<String, Double> columnResizer) {		
		final ContextMenu headerContextMenu = new ContextMenu();			
		final Map<String, CheckMenuItem> menuItemMapping = new HashMap<>();
		
		columns.forEach(c -> {			
			final CheckMenuItem columnVisibleMenuItem = new CheckMenuItem(c.getText());
			columnVisibleMenuItem.setId(c.getId());
			columnVisibleMenuItem.setSelected(true);
			menuItemMapping.put(c.getId(), columnVisibleMenuItem);
			c.setContextMenu(headerContextMenu);			
			
			columnVisibleMenuItem.selectedProperty().addListener((obs, oldV, selected) -> {
				c.setVisible(selected);
				final List<? extends TableColumnBase<T, ?>> visibleColumnHeaders = 
						TableUtils.getVisibleColumnHeaders(columns);
				
				if(!selected && visibleColumnHeaders.size() == 1) {					
					final Optional<MenuItem> lastSelectedMenuItem = headerContextMenu.getItems().stream().filter(
							mi -> ((CheckMenuItem)mi).isSelected()).findFirst();
					if(lastSelectedMenuItem.isPresent()) {
						lastSelectedMenuItem.get().setDisable(true);
					}
				}
				else if(selected && visibleColumnHeaders.size() == 2) {
					headerContextMenu.getItems().stream().filter(mi -> mi instanceof CheckMenuItem &&
						((CheckMenuItem)mi).isSelected()).forEach(mi -> mi.setDisable(false));
				}
			});
			
			Platform.runLater(() -> {
				final Optional<? extends TableColumnBase<T, ?>> columnId = columns.stream().filter(
						tc -> tc.getId().equals(c.getId())).findFirst();				
				columnVisibleMenuItem.setSelected(columnId.isPresent()?
						tableState.getColumnVisibilityMapping().get(columnId.get().getId()) : false);
			});
		});

		tableState.getColumnMappings().values().forEach(c -> 
			headerContextMenu.getItems().add(menuItemMapping.get(c.getId())));		

		final List<CheckMenuItem> checkMenuItems = headerContextMenu.getItems().stream().map(
				mi -> mi instanceof CheckMenuItem? (CheckMenuItem)mi : null).filter(Objects::nonNull).collect(Collectors.toList());
		
		final MenuItem resetHeadersMenuItem = new MenuItem("_Reset");
		resetHeadersMenuItem.setOnAction(e -> TableUtils.resetTableHeader(columns, checkMenuItems, tableState, columnResizer));		
		headerContextMenu.getItems().addAll(new SeparatorMenuItem(), resetHeadersMenuItem);
	}
	
	/**
	 * Build a table column
	 * 
	 * @param <T> Type of the view object stored in the table
	 * @param <U> Type of data stored in the table cells
	 * @param cellValueFactory How to set the cell values
	 * @param valueConverter Function that converts T to string representation
	 * @param alignmentStyle Column content alignment
	 * @param columnName Column name displayed in the column header
	 * @return Built column
	 */
	public static <T, U> TableColumn<T, U> buildColumn(
			final Callback<CellDataFeatures<T, U>, ObservableValue<U>> cellValueFactory,
			final Function<T, String> valueConverter, final String alignmentStyle,
			final String columnName) {
		final TableColumn<T, U> builtColumn = new TableColumn<>(columnName);
		builtColumn.setId(columnName);
		builtColumn.setGraphic(TableUtils.buildColumnHeader(builtColumn, alignmentStyle));		
		builtColumn.setCellValueFactory(cellValueFactory);
		builtColumn.setCellFactory(column -> new TableCell<T, U>() {
			final Label valueLabel = new Label();
			
			@Override
			protected final void updateItem(final U value, final boolean empty) {
				super.updateItem(value, empty);
				if(empty) {
					setText(null);
					setGraphic(null);
				}
				else {
					/*if(this.getTableRow().getItem() == null) {
						return;
					}*/
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
		return builtColumn;
	}
	
	private static <T> void resetTableHeader(final ObservableList<? extends TableColumnBase<T, ?>> columns,
			final List<CheckMenuItem> columnCheckItems, final TableState<T> columnState,
			final BiConsumer<String, Double> columnResizer) {
		
		final Map<String, Double> defaultColumnSizeMapping = columnState.getDefaultColumnSizeMapping();
		columns.clear();		
		columnCheckItems.forEach(ci -> {
			final String columnId = ci.getId();
			ci.setDisable(false);			
			columnResizer.accept(columnId, defaultColumnSizeMapping.get(columnId));
		});	
		columnCheckItems.forEach(ci -> ci.setSelected(columnState.getDefaultVisibleColumnNames().contains(ci.getId())));
	}
	
	private static final <T> List<? extends TableColumnBase<T, ?>> getVisibleColumnHeaders(
			final ObservableList<? extends TableColumnBase<T, ?>> tableColumns) {
		return tableColumns.stream().filter(TableColumnBase::isVisible).collect(Collectors.toList());
	}
}