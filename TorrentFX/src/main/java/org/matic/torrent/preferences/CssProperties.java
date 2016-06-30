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

package org.matic.torrent.preferences;

/**
 * Custom CSS properties
 * 
 * @author Vedran Matic
 *
 */
public final class CssProperties {
	
	private CssProperties() {}

	public static final String TORRENT_LIST_EMPTY_TEXT = "empty-torrent-list-text";
	public static final String ALTERNATE_LIST_ROW_EVEN = "table-row-even";
	public static final String ALTERNATE_LIST_ROW_ODD = "table-row-odd";
	
	public static final String STATUS_BAR_LABEL = "status-bar-label";
	public static final String STATUS_BAR = "status-bar";
	
	public static final String BORDER_TITLE = "titled-border-title";
	
	public static final String OPTION_CATEGORY_LIST__ROOT_CELL = "option-category-selection-root-cell";
	public static final String OPTION_CATEGORY_LIST_CELL = "option-category-selection-cell";
	public static final String OPTION_CATEGORY_LIST = "option-category-selection-list";
	public static final String OPTION_CATEGORY_TITLE = "option-category-label";
	
	public static final String PLACEHOLDER_EMPTY = "empty-placeholder";
	
	public static final String PROGRESSBAR_STOPPED = "progress-bar-stopped";
	
	public static final String TOOLBAR_SEPARATOR = "toolbar-separator";
	public static final String TOOLBAR_BUTTON = "toolbar-button";	

	public static final String SPLIT_PANE = "borderless-split-pane";
	
	public static final String ERROR_TEXT_COLOR = "text-error-color";
	
	public static final String FILTER_LIST_ROOT_CELL_WITHOUT_CHILDREN = "filter-list-root-cell-no-children";
	public static final String FILTER_LIST_CHILD_CELL = "filter-list-child-cell";
	public static final String FILTER_LIST_ROOT_CELL = "filter-list-root-cell";
	public static final String FILTER_LIST_VIEW = "filter-list-view";	
}