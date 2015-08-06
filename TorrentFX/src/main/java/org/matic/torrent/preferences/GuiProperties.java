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
	
	public static final String TAB_ICONS_VISIBLE = "gui.tab.icons.visible"; 
	public static final boolean DEFAULT_TAB_ICONS_VISIBLE = true;
	
	//Positions of window components and the window itself
	public static final String APPLICATION_WINDOW_WIDTH = "gui.window.width";
	public static final double DEFAULT_APPLICATION_WINDOW_WIDTH = 900;
	
	public static final String APPLICATION_WINDOW_HEIGHT = "gui.window.height";
	public static final double DEFAULT_APPLICATION_WINDOW_HEIGHT = 550;
	
	public static final String APPLICATION_WINDOW_POSITION_X = "gui.window.x";	
	public static final String APPLICATION_WINDOW_POSITION_Y = "gui.window.y";
	public static final double DEFAULT_WINDOW_POSITION = Double.MIN_VALUE;
	
	public static final String HORIZONTAL_DIVIDER_POSITION = "gui.horizontal.divider.position";
	public static final double DEFAULT_HORIZONTAL_DIVIDER_POSITION = 0.20;
	
	public static final String VERTICAL_DIVIDER_POSITION = "gui.vertical.divider.position";
	public static final double DEFAULT_VERTICAL_DIVIDER_POSITION = 0.6;
	
	//How often to update the window with latest values
	public static final String GUI_UPDATE_INTERVAL = "gui.update.interval";		
	public static final long DEFAULT_GUI_UPDATE_INTERVAL = 1000;	//1 second
}