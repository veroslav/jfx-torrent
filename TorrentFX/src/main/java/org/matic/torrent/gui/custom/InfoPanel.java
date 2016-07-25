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
package org.matic.torrent.gui.custom;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.BitsView;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.queue.enums.QueueStatus;
import org.matic.torrent.utils.UnitConverter;

import java.util.TimeZone;

/**
 * A pane that shows connectivity and general info about a selected torrent.
 * 
 * @author Vedran Matic
 *
 */
public final class InfoPanel extends VBox {
	
	private static final Insets LABEL_PADDING = new Insets(3);
	
	//Progress bars
	private final AvailabilityBar availabilityBar = new AvailabilityBar();
	private final DownloadProgressBar downloadProgress = new DownloadProgressBar();
		
	//Transfer values labels
	private final Label downloadSpeedValueLabel = new Label();
	private final Label uploadSpeedValueLabel = new Label();
	private final Label timeElapsedValueLabel = new Label();
	private final Label downloadedValueLabel = new Label();	
	private final Label shareRatioValueLabel = new Label();
	private final Label downLimitValueLabel = new Label();
	private final Label remainingValueLabel = new Label();	
	private final Label uploadedValueLabel = new Label();	
	private final Label upLimitValueLabel = new Label();
	private final Label statusValueLabel = new Label();
	private final Label wastedValueLabel = new Label();	
	private final Label seedsValueLabel = new Label();
	private final Label peersValueLabel = new Label();
	
	//General values labels
	private final Label completedOnValueLabel = new Label();
	private final Label createdOnValueLabel = new Label();
	private final Label totalSizeValueLabel = new Label();
	private final Label createdByValueLabel = new Label();	
	private final Label addedOnValueLabel = new Label();
	private final Label commentValueLabel = new Label();
	private final Label piecesValueLabel = new Label();
	private final Label saveAsValueLabel = new Label();	
	private final Label hashValueLabel = new Label();		
	
	public InfoPanel() {
		initComponents();
	}
	
	public void setContent(final TorrentView torrentView) {
        if(torrentView != null) {
            final BitsView availabilityView = torrentView.getAvailabilityView();
            availabilityBar.update(availabilityView);
            downloadProgress.update(availabilityView);
        }
		updateValues(torrentView);
	}
	
	private void updateValues(final TorrentView torrentView) {
		updateTransferValues(torrentView);
		updateGeneralValues(torrentView);
	}
	
	private void updateGeneralValues(final TorrentView torrentView) {
        final boolean clear = torrentView == null;
		saveAsValueLabel.setText(clear? "" : torrentView.getSaveDirectory());
		totalSizeValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getTotalLength())
				+ " (" + UnitConverter.formatByteCount(torrentView.getDownloadedBytes()) + " done)");

        final Long creationTime = clear? 0 : torrentView.getCreationTime();
		createdOnValueLabel.setText(clear || creationTime == null? "" : UnitConverter.formatMillisToDate(
                creationTime, TimeZone.getDefault()));
		addedOnValueLabel.setText(clear? "" : UnitConverter.formatMillisToDate(
                torrentView.getAddedOnTime(), TimeZone.getDefault()));
		hashValueLabel.setText(clear? "" : torrentView.getInfoHash().toString().toUpperCase());
		commentValueLabel.setText(clear? "" : torrentView.getComment());
		piecesValueLabel.setText(clear? "" : torrentView.getTotalPieces() + " x "
				+ UnitConverter.formatByteCount(torrentView.getPieceLength()) + " (have "
				+ torrentView.getHavePieces() + ")");
		createdByValueLabel.setText(clear? "" : torrentView.getCreatedBy());
		completedOnValueLabel.setText(clear? "" : UnitConverter.formatMillisToTime(torrentView.getCompletionTime()));
	}
	
	private void updateTransferValues(final TorrentView torrentView) {
        final boolean clear = torrentView == null;
		timeElapsedValueLabel.setText(clear? "" : UnitConverter.formatMillisToTime(torrentView.getElapsedTime()));
		downloadedValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getDownloadedBytes()));

		final String averageDownloadSpeed = clear? "" : UnitConverter.formatByteCount((long)((double)torrentView
                .getDownloadedBytes()) / (System.currentTimeMillis() - torrentView.getAddedOnTime()));
		downloadSpeedValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getDownloadSpeed())
				+ "/s (avg. " + averageDownloadSpeed + "/s)");
		
		final long downLimit = clear? 0 : torrentView.getDownloadLimit();
		downLimitValueLabel.setText(clear? "" : downLimit == 0? "\u221E" :
			UnitConverter.formatByteCount(downLimit) + "/s");

		statusValueLabel.setText(clear? "" : buildStatusMessage(torrentView.getQueueStatus()));
		remainingValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getRemainingBytes()));
		uploadedValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getUploadedBytes()));

		final String averageUploadSpeed = clear? "" : UnitConverter.formatByteCount((long)((double)torrentView
                .getUploadedBytes()) / (System.currentTimeMillis() - torrentView.getAddedOnTime()));
		uploadSpeedValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getUploadSpeed())
				+ "/s (avg. " + averageUploadSpeed + "/s)");
		
		final long upLimit = clear? 0 : torrentView.getUploadLimit();
		upLimitValueLabel.setText(clear? "" : upLimit == 0? "\u221E" :
			UnitConverter.formatByteCount(upLimit) + "/s");
		
		wastedValueLabel.setText(clear? "" : UnitConverter.formatByteCount(torrentView.getWastedBytes())
				+ " (" + torrentView.getHashFailures() + " hashfails)");
		seedsValueLabel.setText(clear? "" : torrentView.getSeedsConnected() + " of " + torrentView.getSeedsAvailable()
				+ " connected (" + torrentView.getSeedsInSwarm() + " in swarm)");
		peersValueLabel.setText(clear? "" : torrentView.getPeersConnected() + " of " + torrentView.getPeersAvailable()
			+ " connected (" + torrentView.getPeersInSwarm() + " in swarm)");
		shareRatioValueLabel.setText(clear? "" : torrentView.getShareRatio());
	}

    private String buildStatusMessage(final QueueStatus queueStatus) {
        switch(queueStatus) {
            case ACTIVE:
                return "Active";
            case QUEUED:
                return "Queued";
            case INACTIVE:
                return "Stopped";
            case FORCED:
                return "Active (Forced)";
            default:
                return "";
        }
    }
	
	private void initComponents() {		
		final VBox progressPane = new VBox();
        progressPane.getChildren().add(buildProgressBarPanes());
		
		final VBox labelPane = new VBox(buildTransferSection(), buildGeneralSection());
		labelPane.getStyleClass().add("info-pane");

		super.getChildren().addAll(progressPane, labelPane);
		super.setMargin(labelPane, LABEL_PADDING);
		VBox.setVgrow(labelPane, Priority.ALWAYS);
	}
	
	private Node buildProgressBarPanes() {	
		final StackPane availabilityBarHolder = new StackPane(availabilityBar);
		availabilityBarHolder.getStyleClass().add("availability-bar");
		
		final Pane availabilityPane = new Pane();
		availabilityPane.getChildren().add(availabilityBarHolder);
		availabilityBar.widthProperty().bind(availabilityPane.widthProperty());
		availabilityBar.heightProperty().bind(availabilityPane.heightProperty());
		availabilityPane.setMinHeight(25);
		
		final StackPane downloadProgressBarHolder = new StackPane(downloadProgress);
		downloadProgressBarHolder.getStyleClass().add("availability-bar");
		
		final Pane downloadProgressPane = new Pane();
		downloadProgressPane.getChildren().add(downloadProgressBarHolder);
		downloadProgress.widthProperty().bind(downloadProgressPane.widthProperty());
		downloadProgress.heightProperty().bind(downloadProgressPane.heightProperty());
		downloadProgressPane.setMinHeight(25);

        final Insets labelInsets = new Insets(0, 8, 0, 8);
		
		final Label downloadedValueLabel = new Label("0.0 %");
		downloadedValueLabel.setPadding(labelInsets);
        downloadedValueLabel.setMinWidth(70);
		
		final Label availabilityValueLabel = new Label("0.000");
		availabilityValueLabel.setPadding(labelInsets);
        availabilityValueLabel.setMinWidth(70);

        final Label downloadedLabel = new Label("Downloaded: ");
        downloadedLabel.setPadding(GuiUtils.leftPadding());

        final Label availabilityLabel = new Label("Availability: ");
        availabilityLabel.setPadding(GuiUtils.leftPadding());

        final GridPane progressBarPane = new GridPane();
		progressBarPane.add(downloadedLabel, 0, 0);
		progressBarPane.add(downloadProgressPane, 1, 0);
		progressBarPane.add(downloadedValueLabel, 2, 0);
		
		progressBarPane.add(availabilityLabel, 0, 1);
		progressBarPane.add(availabilityPane, 1, 1);
		progressBarPane.add(availabilityValueLabel, 2, 1);
		
		progressBarPane.setPadding(new Insets(5, 3, 3, 3));
		progressBarPane.setVgap(4);
			
		GridPane.setHgrow(downloadProgressPane, Priority.ALWAYS);
		GridPane.setHgrow(availabilityPane, Priority.ALWAYS);		
		
		return progressBarPane;
	}
	
	private Node buildTransferSection() {
		final Node transferSectionPane = buildTransferSectionPane();
		
		final Label transferSectionLabel = new Label("Transfer");
		transferSectionLabel.getStyleClass().add("info-pane-category-label");
		transferSectionLabel.setPadding(GuiUtils.leftPadding());
		transferSectionLabel.setMaxWidth(Double.MAX_VALUE);
		
		final VBox transferPane = new VBox(5);
		transferPane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		transferPane.getChildren().addAll(transferSectionLabel, transferSectionPane);
		
		return transferPane;
	}
	
	private Node buildTransferSectionPane() {		
		final Label timeElapsedLabel = new Label("Time Elapsed:");
		timeElapsedLabel.setPadding(LABEL_PADDING);		
		final Label remainingLabel = new Label("Remaining:");
		remainingLabel.setPadding(LABEL_PADDING);		
		final Label wastedLabel = new Label("Wasted:");
		wastedLabel.setPadding(LABEL_PADDING);
		
		final Label downloadedLabel = new Label("Downloaded:");
		downloadedLabel.setPadding(LABEL_PADDING);
		final Label uploadedLabel = new Label("Uploaded:");
		uploadedLabel.setPadding(LABEL_PADDING);
		final Label seedsLabel = new Label("Seeds:");
		seedsLabel.setPadding(LABEL_PADDING);
		
		final Label downloadSpeedLabel = new Label("Download Speed:");
		downloadSpeedLabel.setPadding(LABEL_PADDING);
		final Label uploadSpeedLabel = new Label("Upload Speed:");
		uploadSpeedLabel.setPadding(LABEL_PADDING);
		final Label peersLabel = new Label("Peers:");
		peersLabel.setPadding(LABEL_PADDING);
		
		final Label downLimitLabel = new Label("Down Limit:");
		downLimitLabel.setPadding(LABEL_PADDING);
		final Label upLimitLabel = new Label("Up Limit:");
		upLimitLabel.setPadding(LABEL_PADDING);
		final Label shareRatioLabel = new Label("Share Ratio:");
		shareRatioLabel.setPadding(LABEL_PADDING);
		
		final Label statusLabel = new Label("Status:");
		statusLabel.setPadding(LABEL_PADDING);
		
		final GridPane transferSectionPane = new GridPane();
		transferSectionPane.add(timeElapsedLabel, 0, 0);
		transferSectionPane.add(timeElapsedValueLabel, 1, 0);
		transferSectionPane.add(remainingLabel, 2, 0);
		transferSectionPane.add(remainingValueLabel, 3, 0);
		transferSectionPane.add(wastedLabel, 4, 0);
		transferSectionPane.add(wastedValueLabel, 5, 0);
		
		transferSectionPane.add(downloadedLabel, 0, 1);
		transferSectionPane.add(downloadedValueLabel, 1, 1);
		transferSectionPane.add(uploadedLabel, 2, 1);
		transferSectionPane.add(uploadedValueLabel, 3, 1);
		transferSectionPane.add(seedsLabel, 4, 1);
		transferSectionPane.add(seedsValueLabel, 5, 1);
		
		transferSectionPane.add(downloadSpeedLabel, 0, 2);
		transferSectionPane.add(downloadSpeedValueLabel, 1, 2);
		transferSectionPane.add(uploadSpeedLabel, 2, 2);
		transferSectionPane.add(uploadSpeedValueLabel, 3, 2);
		transferSectionPane.add(peersLabel, 4, 2);
		transferSectionPane.add(peersValueLabel, 5, 2);
		
		transferSectionPane.add(downLimitLabel, 0, 3);
		transferSectionPane.add(downLimitValueLabel, 1, 3);
		transferSectionPane.add(upLimitLabel, 2, 3);
		transferSectionPane.add(upLimitValueLabel, 3, 3);
		transferSectionPane.add(shareRatioLabel, 4, 3);
		transferSectionPane.add(shareRatioValueLabel, 5, 3);
		
		transferSectionPane.add(statusLabel, 0, 4);
		transferSectionPane.add(statusValueLabel, 1, 4);
		
		final ColumnConstraints labelColumnConstraints = new ColumnConstraints();
		labelColumnConstraints.setPrefWidth(220);
		labelColumnConstraints.setMinWidth(120);
		final ColumnConstraints valueColumnConstraints = new ColumnConstraints();
		valueColumnConstraints.setPrefWidth(300);
		valueColumnConstraints.setMinWidth(120);
		transferSectionPane.getColumnConstraints().addAll(labelColumnConstraints, valueColumnConstraints,
				labelColumnConstraints, valueColumnConstraints, labelColumnConstraints, valueColumnConstraints);

        transferSectionPane.setVgap(0);

		return transferSectionPane;
	}
	
	private Node buildGeneralSection() {
		final Node generalSectionPane = buildGeneralSectionPane();
		
		final Label generalSectionLabel = new Label("General");
		generalSectionLabel.getStyleClass().add("info-pane-category-label");
		generalSectionLabel.setPadding(GuiUtils.leftPadding());
		generalSectionLabel.setMaxWidth(Double.MAX_VALUE);
		
		final VBox generalPane = new VBox(5);
		generalPane.getStyleClass().add(CssProperties.STATUS_BAR_LABEL);
		generalPane.getChildren().addAll(generalSectionLabel, generalSectionPane);
		
		return generalPane;
	}
	
	private Node buildGeneralSectionPane() {
		final Label saveAsLabel = new Label("Save As:");
		saveAsLabel.setPadding(LABEL_PADDING);	
		
		final Label totalSizeLabel = new Label("Total Size:");
		totalSizeLabel.setPadding(LABEL_PADDING);		
		final Label piecesLabel = new Label("Pieces:");
		piecesLabel.setPadding(LABEL_PADDING);
		
		final Label createdOnLabel = new Label("Created On:");
		createdOnLabel.setPadding(LABEL_PADDING);
		final Label createdByLabel = new Label("Created By:");
		createdByLabel.setPadding(LABEL_PADDING);
		
		final Label addedOnLabel = new Label("Added On:");
		addedOnLabel.setPadding(LABEL_PADDING);
		final Label completedOnLabel = new Label("Completed On:");
		completedOnLabel.setPadding(LABEL_PADDING);
		
		final Label hashLabel = new Label("Hash:");
		hashLabel.setPadding(LABEL_PADDING);
		final Label commentLabel = new Label("Comment:");
		commentLabel.setPadding(LABEL_PADDING);
		
		final GridPane generalSectionPane = new GridPane();
		generalSectionPane.add(saveAsLabel, 0, 0);
		generalSectionPane.add(saveAsValueLabel, 1, 0);
		
		generalSectionPane.add(totalSizeLabel, 0, 1);
		generalSectionPane.add(totalSizeValueLabel, 1, 1);
		generalSectionPane.add(piecesLabel, 2, 1);
		generalSectionPane.add(piecesValueLabel, 3, 1);
		
		generalSectionPane.add(createdOnLabel, 0, 2);
		generalSectionPane.add(createdOnValueLabel, 1, 2);
		generalSectionPane.add(createdByLabel, 2, 2);
		generalSectionPane.add(createdByValueLabel, 3, 2);
		
		generalSectionPane.add(addedOnLabel, 0, 3);
		generalSectionPane.add(addedOnValueLabel, 1, 3);
		generalSectionPane.add(completedOnLabel, 2, 3);
		generalSectionPane.add(completedOnValueLabel, 3, 3);
		
		generalSectionPane.add(hashLabel, 0, 4);
		generalSectionPane.add(hashValueLabel, 1, 4);
		
		generalSectionPane.add(commentLabel, 0, 5);
		generalSectionPane.add(commentValueLabel, 1, 5);
        generalSectionPane.setVgap(0);
		
		final ColumnConstraints labelColumnConstraints = new ColumnConstraints();
		labelColumnConstraints.setPrefWidth(180);
		final ColumnConstraints valueColumnConstraints = new ColumnConstraints();
		valueColumnConstraints.setPrefWidth(400);
		
		generalSectionPane.getColumnConstraints().addAll(labelColumnConstraints, valueColumnConstraints,
				labelColumnConstraints, valueColumnConstraints);
		
		return generalSectionPane;
	}
}