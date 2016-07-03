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
package org.matic.torrent.gui.action;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.model.TrackableView;
import org.matic.torrent.gui.table.TrackerTable;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.tracking.Tracker;
import org.matic.torrent.tracking.TrackerManager;

import java.util.Collection;
import java.util.Optional;

/**
 * A handler for events triggered from the trackers' context menu.
 *
 * @author vedran
 *
 */
public final class TrackerTableActionHandler {

    /**
     * Add a user entered list of tracker url:s for tracking.
     *
     * @param urls User entered tracker url:s to add.
     * @param torrentManager Torrent manager.
     * @param torrentView View of the target tracked torrent.
     * @param trackerTable Tracker table to which to add trackers views.
     */
    public void onTrackersAdded(final Collection<String> urls, final QueuedTorrentManager torrentManager,
                                final TorrentView torrentView, final TrackerTable trackerTable) {
        urls.forEach(url -> trackerTable.addTrackerViews(torrentManager.addTrackers(torrentView, urls)));
    }

    /**
     * Request a manual tracker update by a user
     *
     * @param trackerViews Tracker views to update
     * @param trackerManager Target tracker manager
     */
    public void onTrackerUpdate(final Collection<TrackableView> trackerViews, final TrackerManager trackerManager) {
        if(trackerViews.isEmpty()) {
            return;
        }

        trackerViews.forEach(tv -> trackerManager.issueAnnounce(tv.getName(), tv.getTorrentView(), Tracker.Event.UPDATE));
    }

    /**
     * Handle a tracker deletion by the user
     *
     * @param trackerViews Tracker views to delete
     * @param trackerManager Target tracker manager
     * @param trackableTable Tracker table from which to delete trackers views
     */
    public void onTrackerDeletion(final Collection<TrackableView> trackerViews, final TrackerManager trackerManager,
                                  final TrackerTable trackableTable, final Window owner) {
        if(trackerViews.isEmpty()) {
            return;
        }
        final boolean confirmTrackerDeletion = ApplicationPreferences.getProperty(
                GuiProperties.DELETE_TRACKER_CONFIRMATION, true);
        boolean shouldDeleteTracker = false;
        if(confirmTrackerDeletion) {
            final StringBuilder warningMessage = new StringBuilder("Do you really want to delete selected tracker");
            if(trackerViews.size() > 1) {
                warningMessage.append("s");
            }
            warningMessage.append("?");

            final Alert deleteTrackerAlert = new Alert(AlertType.WARNING, warningMessage.toString(),
                    ButtonType.OK, ButtonType.CANCEL);
            deleteTrackerAlert.initOwner(owner);
            deleteTrackerAlert.setTitle("Delete tracker");
            deleteTrackerAlert.setHeaderText(null);
            final Optional<ButtonType> selectedButton = deleteTrackerAlert.showAndWait();
            shouldDeleteTracker = selectedButton.isPresent() && selectedButton.get() == ButtonType.OK;
        }
        if(!confirmTrackerDeletion || shouldDeleteTracker) {
            trackerViews.forEach(tv -> {
                final boolean removed = trackerManager.removeTracker(tv.getName(), tv.getTorrentView());
                if(removed) {
                    trackableTable.removeTracker(tv);
                }
            });
        }
    }
}