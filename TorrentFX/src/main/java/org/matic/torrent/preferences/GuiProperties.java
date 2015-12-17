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

package org.matic.torrent.preferences;

public final class GuiProperties {
	
	//Theme properties
	public static final String THEME_STYLESHEET_PATH_TEMPLATE = "/themes/?";
	public static final String THEME_UI_STYLE_CSS = "/ui-style.css";
	public static final String APPLICATION_THEME = "gui.theme";

	//CSS properties for Options window
	public static final String OPTION_CATEGORY_INDENTATION = "option-category-indentation";
	public static final String HORIZONTAL_LAYOUT_SPACING = "layout-horizontal-spacing";
	public static final String VERTICAL_LAYOUT_SPACING = "layout-vertical-spacing";
	
	//Confirmation options
	public static final String EXIT_CONFIRMATION_CRITICAL_SEEDER = "gui.confirm.criticalexit";	
	public static final String DELETE_TORRENT_CONFIRMATION = "gui.confirm.torrentdelete";
	public static final String DELETE_TRACKER_CONFIRMATION = "gui.confirm.trackerdelete";	
	public static final String EXIT_CONFIRMATION = "gui.confirm.exit";
	
	//Various status and color visibilities
	public static final String SHOW_SPEED_LIMITS_IN_STATUSBAR = "gui.statusbar.speedlimits";
	public static final String SHOW_SPEED_IN_TITLEBAR = "gui.titlebar.showspeed";	
	public static final String ALTERNATE_LIST_ROW_COLOR = "gui.color.alternate";
	
	//System Tray options
	public static final String MINIMIZE_APPLICATION_TO_TRAY = "gui.systemtray.minimize";
	public static final String BALLOON_NOTIFICATIONS_ON_TRAY = "gui.systemtray.balloon";
	public static final String ACTIVATE_ON_TRAY_CLICK = "gui.systemtray.clickactivate";
	public static final String SINGLE_CLICK_ON_TRAY_TO_OPEN = "gui.systemtray.open";
	public static final String CLOSE_APPLICATION_TO_TRAY = "gui.systemtray.close";	
	public static final String ALWAYS_SHOW_TRAY = "gui.systemtray.show";		
	
	//Torrent addition options
	public static final String ALLOW_NAME_AND_LOCATION_CHANGE = "gui.addtorrent.namelocationchange";
	public static final String ACTIVATE_WINDOW_ON_TORRENT_ADDITION = "gui.addtorrent.windowactive";
	
	//Mouse action options
	public static final String CLICK_ON_DOWNLOADING_TORRENT_ACTION = "gui.mouse.click.downloading";
	public static final String CLICK_ON_SEEDING_TORRENT_ACTION = "gui.mouse.click.seeding";
	
	//Tab ID:s	
	public static final String TRACKERS_TAB_ID = "Trackers";
	public static final String PIECES_TAB_ID = "Pieces";
	public static final String LOGGER_TAB_ID = "Logger";
	public static final String FILES_TAB_ID = "Files";
	public static final String PEERS_TAB_ID = "Peers";
	public static final String SPEED_TAB_ID = "Speed";
	public static final String INFO_TAB_ID = "Info";
	
	public static final String COMPOSITE_PROPERTY_VALUE_SEPARATOR = ":";

	//Visibility of the main window components
	public static final String TOOLBAR_VISIBLE = "gui.toolbar.visible"; 
	public static final boolean DEFAULT_TOOLBAR_VISIBLE = true;
	
	public static final String STATUSBAR_VISIBLE = "gui.statusbar.visible"; 
	public static final boolean DEFAULT_STATUSBAR_VISIBLE = true;
	
	public static final String COMPACT_TOOLBAR = "gui.toolbar.compact"; 
	public static final boolean DEFAULT_COMPACT_TOOLBAR = true;
	
	public static final String DETAILED_INFO_VISIBLE = "gui.detailed.info.visible"; 
	public static final boolean DEFAULT_DETAILED_INFO_VISIBLE = true;
	
	public static final String FILTER_VIEW_VISIBLE = "gui.filter.view.visible"; 
	public static final boolean DEFAULT_FILTER_VIEW_VISIBLE = true;
	
	//Visibility and sizes of tables' headers
	public static final String TORRENT_JOBS_TAB_COLUMN_VISIBILITY = "gui.columns.visibility.jobs";	
	public static final String TORRENT_JOBS_TAB_COLUMN_ORDER = "gui.columns.order.jobs";		
	public static final String TORRENT_JOBS_TAB_COLUMN_SIZE = "gui.columns.size.jobs";
	public static final String DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES = "true:true";
	public static final String DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER = "#:Name";
	public static final String DEFAULT_TORRENT_JOBS_COLUMN_SIZES = "40:200";
	
	public static final String DEFAULT_INFO_COLUMN_ORDER = "Name:Path:Size:Priority";
	public static final String DEFAULT_INFO_COLUMN_SIZES = "230:150:140:90";
	public static final String DEFAULT_INFO_COLUMN_VISIBILITIES = "true:false:true:true";
	public static final String INFO_COLUMN_ORDER = "gui.columns.order.info";
	public static final String INFO_COLUMN_VISIBILITY = "gui.columns.visibility.info";
	public static final String INFO_COLUMN_SIZE = "gui.columns.size.info";
	
	public static final String DEFAULT_INFO_TAB_COLUMN_ORDER = "Name:Path:Size:Priority:Done:First Piece:#Pieces:Progress";
	public static final String DEFAULT_INFO_TAB_COLUMN_SIZES = "230:150:140:90:90:70:70:70";
	public static final String DEFAULT_INFO_TAB_COLUMN_VISIBILITIES = "true:false:true:true:true:true:true:true";
	public static final String INFO_TAB_COLUMN_ORDER = "gui.columns.order.info.tab";
	public static final String INFO_TAB_COLUMN_VISIBILITY = "gui.columns.visibility.info.tab";
	public static final String INFO_TAB_COLUMN_SIZE = "gui.columns.size.info.tab";
		
	public static final String DEFAULT_TRACKER_TAB_COLUMN_ORDER = "Name:Status:Update In:Interval:Min Interval:Seeds:Peers:Downloaded";
	public static final String DEFAULT_TRACKER_TAB_COLUMN_SIZES = "230:140:90:90:70:70:70:110";
	public static final String DEFAULT_TRACKER_TAB_COLUMN_VISIBILITIES = "true:true:true:false:false:true:true:true";	
	public static final String TRACKER_TAB_COLUMN_ORDER = "gui.columns.order.trackers";
	public static final String TRACKER_TAB_COLUMN_VISIBILITY = "gui.columns.visibility.trackers";
	public static final String TRACKER_TAB_COLUMN_SIZE = "gui.columns.size.trackers";			
	
	//Visibility of the detailed info tab pane components
	public static final String TAB_ICONS_VISIBLE = "gui.tab.icons.visible"; 
	public static final boolean DEFAULT_TAB_ICONS_VISIBLE = true;
	
	public static final String SELECTED_TAB_ID = "gui.tab.selected";
	public static final String DEFAULT_SELECTED_TAB_ID = FILES_TAB_ID;
		
	public static final String TAB_VISIBILITY = "gui.tab.visibility";	
	public static final String DEFAULT_TAB_VISIBILITY = "Files:Info:Peers:Trackers:Speed";
	
	//Positions of window components and the window itself
	public static final String APPLICATION_WINDOW_WIDTH = "gui.window.width";
	public static final double DEFAULT_APPLICATION_WINDOW_WIDTH = 900;
	
	public static final String APPLICATION_WINDOW_HEIGHT = "gui.window.height";
	public static final double DEFAULT_APPLICATION_WINDOW_HEIGHT = 550;
	
	public static final String APPLICATION_WINDOW_POSITION_X = "gui.window.x";	
	public static final String APPLICATION_WINDOW_POSITION_Y = "gui.window.y";
	public static final double DEFAULT_APPLICATION_WINDOW_POSITION = Double.MIN_VALUE;
	
	public static final String HORIZONTAL_DIVIDER_POSITION = "gui.horizontal.divider.position";
	public static final double DEFAULT_HORIZONTAL_DIVIDER_POSITION = 0.20;
	
	public static final String VERTICAL_DIVIDER_POSITION = "gui.vertical.divider.position";
	public static final double DEFAULT_VERTICAL_DIVIDER_POSITION = 0.6;
	
	//How often to update the window with latest values
	public static final String GUI_UPDATE_INTERVAL = "gui.update.interval";		
	public static final long DEFAULT_GUI_UPDATE_INTERVAL = 1000;	//1 second
}