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

import java.util.Optional;

import org.matic.torrent.gui.window.preferences.PreferencesWindow;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class WindowActionHandler {
	
	/**
	 * Handle user opening the options dialog.
	 * 
	 * @param owner Owner window of the options dialog
	 */
	public final void onOptionsWindowShown(final Window owner) {
		final PreferencesWindow optionsWindow = new PreferencesWindow(owner);
		optionsWindow.showAndWait();
	}

	/**
	 * Handle user choosing to close the application.
	 * 
	 * @param event Originating window event
	 * @param owner Owner window of the close confirmation dialog
	 */
	public final void onWindowClose(final Event event, final Window owner) {
		final boolean isClosed = handleWindowClosing(owner);
        if(isClosed) {
            //User chose to close the application, quit
            Platform.exit();
        }
        else {
            //User cancelled quit action, don't do anything
            event.consume();
        }
	}
	
	private boolean handleWindowClosing(final Window owner) {
		final Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
		confirmAlert.initOwner(owner);
		confirmAlert.setContentText("Are you sure you want to exit jfxTorrent?");
		confirmAlert.setTitle("Confirm Exit");				
		confirmAlert.setHeaderText(null);
		
		final Optional<ButtonType> result = confirmAlert.showAndWait();
		return result.get() == ButtonType.OK;
	}
}
