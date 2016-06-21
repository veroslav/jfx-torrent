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

import javafx.scene.control.TableColumnBase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bean holding a table's state during the initialization
 * 
 * @author vedran
 *
 * @param <T> Type of the view object contained by the table's columns
 */
public final class TableState<T> {
	
	private final LinkedHashMap<String, ? extends TableColumnBase<T, ?>> columnMappings;
	private final Map<String, Boolean> columnVisibilityMapping;
	private final Set<String> defaultVisibleColumnNames;
	
	private final Map<String, Double> defaultColumnSizeMapping;
	private final String defaultColumnOrder;	
	
	public TableState(final LinkedHashMap<String, ? extends TableColumnBase<T, ?>> columnMappings,
			final Map<String, Boolean> columnVisibilityMapping,
			final Set<String> defaultVisibleColumnNames,
			final Map<String, Double> defaultColumnSizeMapping, final String defaultColumnOrder) {
		this.columnMappings = columnMappings;
		this.columnVisibilityMapping = columnVisibilityMapping;
		this.defaultVisibleColumnNames = defaultVisibleColumnNames;
		this.defaultColumnSizeMapping = defaultColumnSizeMapping;
		this.defaultColumnOrder = defaultColumnOrder;		
	}
	
	public final String getDefaultColumnOrder() {
		return defaultColumnOrder;
	}

	public final Map<String, Double> getDefaultColumnSizeMapping() {
		return defaultColumnSizeMapping;
	}

	public final LinkedHashMap<String, ? extends TableColumnBase<T, ?>> getColumnMappings() {
		return columnMappings;
	}

	public final Map<String, Boolean> getColumnVisibilityMapping() {
		return columnVisibilityMapping;
	}

	public final Set<String> getDefaultVisibleColumnNames() {
		return defaultVisibleColumnNames;
	}

	@Override
	public String toString() {
		return "TableState [columnMappings=" + columnMappings + ", columnVisibilityMapping=" + columnVisibilityMapping
				+ ", defaultVisibleColumnNames=" + defaultVisibleColumnNames + ", defaultColumnSizeMapping="
				+ defaultColumnSizeMapping + ", defaultColumnOrder=" + defaultColumnOrder + "]";
	}		
}