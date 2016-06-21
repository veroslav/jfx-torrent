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

package org.matic.torrent.gui.action;

import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.matic.torrent.gui.window.preferences.PreferencesWindow;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;

import java.util.Optional;

public final class WindowActionHandler {
	
	/**
	 * Store position and size of the window, if changed
	 * 
	 * @param window Target window
	 */
	public final void storeWindowStateChanges(final Window window) {
		final Scene scene = window.getScene();
		final double oldWindowWidth = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_WINDOW_WIDTH,
				GuiProperties.DEFAULT_APPLICATION_WINDOW_WIDTH);
		final double newWindowWidth = scene.getWidth();
		if(newWindowWidth != oldWindowWidth) {			
			ApplicationPreferences.setProperty(GuiProperties.APPLICATION_WINDOW_WIDTH, newWindowWidth);
		}
		
		final double oldWindowHeight = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_WINDOW_HEIGHT,
				GuiProperties.DEFAULT_APPLICATION_WINDOW_HEIGHT);
		final double newWindowHeight = scene.getHeight();
		if(newWindowHeight != oldWindowHeight) {			
			ApplicationPreferences.setProperty(GuiProperties.APPLICATION_WINDOW_HEIGHT, newWindowHeight);
		}
		
		final double oldXPosition = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_WINDOW_POSITION_X,
				GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION);
		final double newXPosition = window.getX();
		if(newXPosition != oldXPosition) {			
			ApplicationPreferences.setProperty(GuiProperties.APPLICATION_WINDOW_POSITION_X, newXPosition);
		}
		
		final double oldYPosition = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_WINDOW_POSITION_Y,
				GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION);
		final double newYPosition = window.getY();
		if(newYPosition != oldYPosition) {						
			ApplicationPreferences.setProperty(GuiProperties.APPLICATION_WINDOW_POSITION_Y, newYPosition);
		}
	}
	
	/**
	 * Handle user opening the options dialog.
	 * 
	 * @param owner Owner window of the options dialog
	 */
	public final void onOptionsWindowShown(final Window owner,
			final FileActionHandler fileActionHandler) {
		final PreferencesWindow optionsWindow = new PreferencesWindow(owner, fileActionHandler);
		optionsWindow.showAndWait();
	}

	/**
	 * Handle user choosing to close the application.
	 * 
	 * @param event Originating window event
	 * @param owner Owner window of the close confirmation dialog
	 * @return Whether the user chose to really quit
	 */
	public boolean onWindowClose(final Event event, final Window owner) {
		final boolean isClosed = handleWindowClosing(owner);
        
        if(!isClosed) {
            //User cancelled quit action, don't do anything
            event.consume();
        }
        
        return isClosed;
	}
	
	private boolean handleWindowClosing(final Window owner) {
		final Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
		confirmAlert.initOwner(owner);
		confirmAlert.setContentText("Are you sure you want to exit " +
				ClientProperties.CLIENT_NAME + "?");
		confirmAlert.setTitle("Confirm Exit");				
		confirmAlert.setHeaderText(null);
		
		final Optional<ButtonType> result = confirmAlert.showAndWait();
		return result.get() == ButtonType.OK;
	}
}
