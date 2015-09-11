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

import java.util.Collection;
import java.util.Optional;

import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.gui.table.TrackerTable;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class TrackerTableActionHandler {

	public final void onTrackerUpdate(final Collection<TrackerView> trackerViews, final TrackerManager trackerManager) {
		if(trackerViews.isEmpty()) {
			return;
		}
		
		trackerViews.forEach(tv -> trackerManager.issueAnnounce(tv.getTrackerName(), tv.getTorrent(), Tracker.Event.UPDATE));
	}
	
	public final void onTrackerDeletion(final Collection<TrackerView> trackerViews, final TrackerManager trackerManager,
			final TrackerTable trackerTable) {		
		if(trackerViews.isEmpty()) {
			return;
		}
		
		final StringBuilder warningMessage = new StringBuilder("Do you really want to delete selected tracker");
		if(trackerViews.size() > 1) {
			warningMessage.append("s");
		}
		warningMessage.append("?");
		
		final Alert deleteTrackerAlert = new Alert(AlertType.WARNING, warningMessage.toString(),
						ButtonType.OK, ButtonType.CANCEL);
		deleteTrackerAlert.setTitle("Delete tracker");
		deleteTrackerAlert.setHeaderText(null);
		final Optional<ButtonType> selectedButton = deleteTrackerAlert.showAndWait();
		if(selectedButton.isPresent() && selectedButton.get() == ButtonType.OK) {			
			trackerViews.forEach(tv -> {
				final boolean removed = trackerManager.removeTracker(tv.getTrackerName(), tv.getTorrent());
				if(removed) {
					trackerTable.removeTracker(tv);
				}
			});
		}
	}
}