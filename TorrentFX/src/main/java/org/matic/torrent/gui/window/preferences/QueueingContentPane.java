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
package org.matic.torrent.gui.window.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.preferences.TransferProperties;

public final class QueueingContentPane extends CategoryContentPane {
	
	private static final String QUEUEING_CONTENT_PANE_NAME = "Queueing";
	
	private final TextField activeTorrentsLimitField = new TextField();
	private final TextField activeDownloadsLimitField = new TextField();
	private final TextField activeUploadsLimitField = new TextField();
	
	public QueueingContentPane(final BooleanProperty preferencesChanged) {
		super(QUEUEING_CONTENT_PANE_NAME, preferencesChanged);
		initComponents(preferencesChanged);
	}

	@Override
	public void onSaveContentChanges() {
		if(super.preferencesChanged.get()) {
			//Apply changed values
            final String activeTorrentsLimit = String.valueOf(ApplicationPreferences.getProperty(
                    TransferProperties.ACTIVE_TORRENTS_LIMIT, TransferProperties.DEFAULT_ACTIVE_TORRENTS_LIMIT));
            final String typedActiveTorrentsLimit = activeTorrentsLimitField.getText();
            if(!activeTorrentsLimit.equals(typedActiveTorrentsLimit)) {
                ApplicationPreferences.setProperty(
                        TransferProperties.ACTIVE_TORRENTS_LIMIT, typedActiveTorrentsLimit);
            }

            final String activeDownloadsLimit = String.valueOf(ApplicationPreferences.getProperty(
                    TransferProperties.DOWNLOADING_TORRENTS_LIMIT, TransferProperties.DEFAULT_DOWNLOADING_TORRENTS_LIMIT));
            final String typedActiveDownloadsLimit = activeDownloadsLimitField.getText();
            if(!activeDownloadsLimit.equals(typedActiveDownloadsLimit)) {
                ApplicationPreferences.setProperty(
                        TransferProperties.DOWNLOADING_TORRENTS_LIMIT, typedActiveDownloadsLimit);
            }

            final String activeUploadsLimit = String.valueOf(ApplicationPreferences.getProperty(
                    TransferProperties.UPLOADING_TORRENTS_LIMIT, TransferProperties.DEFAULT_UPLOADING_TORRENTS_LIMIT));
            final String typedActiveUploadsLimit = activeUploadsLimitField.getText();
            if(!activeUploadsLimit.equals(typedActiveUploadsLimit)) {
                ApplicationPreferences.setProperty(
                        TransferProperties.UPLOADING_TORRENTS_LIMIT, typedActiveUploadsLimit);
            }
		}		
	}

	@Override
	protected Node build() {
		final VBox content = new VBox();
		content.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		content.getChildren().addAll(
				new TitledBorderPane("Queue Settings", buildQueueSettingsPane(), BorderStyle.COMPACT,
						TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE));
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		return contentScroll;
	}	
	
	protected void initComponents(final BooleanProperty preferencesChanged) {
		activeTorrentsLimitField.setText(String.valueOf(ApplicationPreferences.getProperty(
				TransferProperties.ACTIVE_TORRENTS_LIMIT, TransferProperties.DEFAULT_ACTIVE_TORRENTS_LIMIT)));
		activeDownloadsLimitField.setText(String.valueOf(ApplicationPreferences.getProperty(
				TransferProperties.DOWNLOADING_TORRENTS_LIMIT, TransferProperties.DEFAULT_DOWNLOADING_TORRENTS_LIMIT)));
		activeUploadsLimitField.setText(String.valueOf(ApplicationPreferences.getProperty(
				TransferProperties.UPLOADING_TORRENTS_LIMIT, TransferProperties.DEFAULT_UPLOADING_TORRENTS_LIMIT)));

        addListeners(preferencesChanged);
	}

    private void addListeners(final BooleanProperty preferencesChanged) {
        activeDownloadsLimitField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
        activeTorrentsLimitField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
        activeUploadsLimitField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
    }
	
	private Node buildQueueSettingsPane() {		
		final GridPane queueSettingsPane = new GridPane();
		
		queueSettingsPane.add(new Label("Maximum number of active torrents (upload or download): "), 0, 0);
		queueSettingsPane.add(activeTorrentsLimitField, 1, 0);
		queueSettingsPane.add(new Label("Maximum number of active downloads: "), 0, 1);
		queueSettingsPane.add(activeDownloadsLimitField, 1, 1);
		queueSettingsPane.add(new Label("Maximum number of active uploads: "), 0, 2);
		queueSettingsPane.add(activeUploadsLimitField, 1, 2);
		
		final ColumnConstraints firstColumn = new ColumnConstraints();
		firstColumn.setHgrow(Priority.ALWAYS);
		
		final ColumnConstraints secondColumn = new ColumnConstraints();		
		secondColumn.setPrefWidth(60);
        secondColumn.setMinWidth(60);
		
		queueSettingsPane.getColumnConstraints().addAll(firstColumn, secondColumn);
		queueSettingsPane.setVgap(5);
		
		return queueSettingsPane;
	}
}