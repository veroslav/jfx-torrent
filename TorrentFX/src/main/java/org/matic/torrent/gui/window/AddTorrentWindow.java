/*
* This file is part of Trabos, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015-2017 Vedran Matic
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
package org.matic.torrent.gui.window;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.gui.model.FileTree;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.table.TableUtils;
import org.matic.torrent.gui.tree.FileTreeViewer;
import org.matic.torrent.gui.tree.TreeTableUtils;
import org.matic.torrent.io.DiskUtilities;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.utils.UnitConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.TimeZone;

/**
 * A window showing contents of a torrent to be opened and added to a list of torrents.
 *
 * @author Vedran Matic
 *
 */
public final class AddTorrentWindow {

    private final CheckBox createSubFolderCheckbox;
    private final CheckBox dontShowAgainCheckbox;
    private final CheckBox addToTopQueueCheckbox;
    private final CheckBox startTorrentCheckbox;
    private final CheckBox skipHashCheckbox;

    private final ComboBox<String> savePathCombo;
    private final ComboBox<String> labelCombo;

    private final TextField nameTextField;

    private final Button collapseAllButton;
    private final Button expandAllButton;
    private final Button selectNoneButton;
    private final Button selectAllButton;
    private final Button advancedButton;
    private final Button browseButton;

    private final TextFlow fileSizeLabel;
    private final Text diskSpaceText;
    private final Text fileSizeText;

    private final TreeTableView<TorrentFileEntry> fileView = new TreeTableView<>();

    private final BinaryEncodedDictionary state;
    private final QueuedTorrentMetaData metaData;
    private final QueuedTorrentProgress progress;
    private final FileTreeViewer fileTreeViewer;

    private final Dialog<ButtonType> window;

    private Optional<Long> availableDiskSpace;

    public AddTorrentWindow(final Window owner, final QueuedTorrentMetaData metaData,
                            final FileTreeViewer fileTreeViewer) {
        this.metaData = metaData;
        this.fileTreeViewer = fileTreeViewer;

        final Path savePath = Paths.get(System.getProperty("user.home"));
        availableDiskSpace = DiskUtilities.getAvailableDiskSpace(savePath);

        savePathCombo = new ComboBox<>();
        savePathCombo.getItems().add(savePath.toString());

        window = new Dialog<>();
        window.initOwner(owner);

        dontShowAgainCheckbox = new CheckBox("Don't show this again");
        addToTopQueueCheckbox = new CheckBox("Add to top of queue");
        createSubFolderCheckbox = new CheckBox("Create subfolder");
        startTorrentCheckbox = new CheckBox("Start torrent");
        skipHashCheckbox = new CheckBox("Skip hash check");

        diskSpaceText = new Text();
        fileSizeText = new Text();
        fileSizeText.getStyleClass().add("text");

        fileSizeLabel = new TextFlow();
        fileSizeLabel.getChildren().addAll(fileSizeText, diskSpaceText);

        labelCombo = new ComboBox<>();
        advancedButton = new Button("Advanced...");

        nameTextField = new TextField(metaData.getName());

        collapseAllButton = new Button("Collapse All");
        expandAllButton = new Button("Expand All");
        selectNoneButton = new Button("Select None");
        selectAllButton = new Button("Select All");
        browseButton = new Button("...");

        this.state = createInitialProgressState();
        this.progress = new QueuedTorrentProgress(state);
        fileView.setRoot(fileTreeViewer.createTreeView(fileView, new FileTree(metaData, progress)));

        updateDiskUsageLabel();
        fileView.getRoot().getValue().selectionLengthProperty().addListener((obs, oldV, newV) -> updateDiskUsageLabel());

        initComponents();
    }

    public AddedTorrentOptions showAndWait() {
        final Optional<ButtonType> result = window.showAndWait();
        storeColumnStates();
        fileTreeViewer.restoreDefault();

        if(result.isPresent() && result.get() == ButtonType.OK) {
            final QueueType targetStatus = startTorrentCheckbox.isSelected()?
                    QueueType.ACTIVE : QueueType.INACTIVE;
            progress.setQueueType(targetStatus);

            final String savePath = savePathCombo.getEditor().getText();
            progress.setSavePath(savePath != null? Paths.get(savePath) : Paths.get(System.getProperty("user.home")));

            return new AddedTorrentOptions(metaData, progress, fileView.getRoot(),
                    createSubFolderCheckbox.isSelected(),
                    addToTopQueueCheckbox.isSelected(), skipHashCheckbox.isSelected());
        }
        return null;
    }

    private BinaryEncodedDictionary createInitialProgressState() {
        final BinaryEncodedDictionary state = new BinaryEncodedDictionary();
        state.put(BinaryEncodingKeys.KEY_NAME, new BinaryEncodedString(nameTextField.getText()));

        //state.put(BinaryEncodingKeys.KEY_PATH, new BinaryEncodedString(savePathCombo.getSelectionModel().getSelectedItem()));
        //state.put(BinaryEncodingKeys.STATE_KEY_LABEL, new BinaryEncodedString(labelCombo.getSelectionModel().getSelectedItem()));

        final QueueType targetStatus = startTorrentCheckbox.isSelected()? QueueType.ACTIVE : QueueType.INACTIVE;
        state.put(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME, new BinaryEncodedString(targetStatus.name()));

        final BinaryEncodedList trackerList = new BinaryEncodedList();
        metaData.getAnnounceList().stream().flatMap(l ->
                ((BinaryEncodedList)l).stream()).forEach(trackerList::add);

        final String announceUrl = metaData.getAnnounceUrl();
        if(announceUrl != null) {
            trackerList.add(new BinaryEncodedString(announceUrl));
        }

        state.put(BinaryEncodingKeys.KEY_ANNOUNCE_LIST, trackerList);
        return state;
    }

    private void initComponents() {
        createSubFolderCheckbox.setSelected(true);
        startTorrentCheckbox.setSelected(true);
        labelCombo.setEditable(true);

        savePathCombo.setMinWidth(400);
        savePathCombo.setMaxWidth(400);
        savePathCombo.setEditable(true);
        savePathCombo.getSelectionModel().selectFirst();
        savePathCombo.setOnAction(event -> {
            final Path targetDownloadPath = Paths.get(savePathCombo.getSelectionModel().getSelectedItem());
            availableDiskSpace = DiskUtilities.getAvailableDiskSpace(targetDownloadPath);
            updateDiskUsageLabel();
        });

        browseButton.setTooltip(new Tooltip("Browse for download directory"));
        browseButton.setOnAction(event -> onBrowseForTargetDirectory());

        selectAllButton.setOnAction(event -> {
            fileTreeViewer.selectAllEntries();
            selectAllButton.requestFocus();
        });

        selectNoneButton.setOnAction(event -> {
            fileTreeViewer.unselectAllEntries();
            selectNoneButton.requestFocus();
        });

        collapseAllButton.setOnAction(event -> fileTreeViewer.collapseAll());
        expandAllButton.setOnAction(event -> fileTreeViewer.expandAll());

        TreeTableUtils.addFileListingViewColumns(fileView, false);
        TreeTableUtils.setupFileListingView(fileView, fileTreeViewer);

        window.setHeaderText(null);
        window.setTitle(metaData.getName() + " - Add New Torrent");

        window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        window.setResizable(true);
        window.getDialogPane().setContent(layoutContent());
    }

    private void updateDiskUsageLabel() {
        final TorrentFileEntry fileEntry = fileView.getRoot().getValue();
        final long totalFilesLength = fileEntry.getLength();
        final StringBuilder fileSizeBuilder = new StringBuilder();
        final long fileSelectionLength = fileEntry.getSelectionLength();
        fileSizeBuilder.append(UnitConverter.formatByteCount(fileSelectionLength));

        if(fileSelectionLength < totalFilesLength) {
            fileSizeBuilder.append(" (of ");
            fileSizeBuilder.append(UnitConverter.formatByteCount(totalFilesLength));
            fileSizeBuilder.append(")");
        }

        final StringBuilder diskSpaceBuilder = new StringBuilder();
        diskSpaceBuilder.append(" (disk space: ");

        fileSizeText.setText(fileSizeBuilder.toString());

        try {
            final long availableDiskSpaceValue = availableDiskSpace.orElseThrow(() ->
                    new IOException("Disk usage can't be calculated"));
            final long remainingDiskSpace = availableDiskSpaceValue - fileSelectionLength;

            if(remainingDiskSpace < 0) {
                diskSpaceBuilder.append(UnitConverter.formatByteCount(Math.abs(remainingDiskSpace)));
                diskSpaceBuilder.append(" too short)");
                diskSpaceText.setText(diskSpaceBuilder.toString());
                diskSpaceText.setStyle(CssProperties.ERROR_TEXT_COLOR);
            }
            else {
                diskSpaceBuilder.append(UnitConverter.formatByteCount(remainingDiskSpace));
                diskSpaceBuilder.append(")");
                diskSpaceText.setText(diskSpaceBuilder.toString());
                diskSpaceText.getStyleClass().add("text");
            }
        } catch(final IOException ioe) {
            diskSpaceBuilder.append("can't be calculated)");
            diskSpaceText.setText(diskSpaceBuilder.toString());
            diskSpaceText.setStyle(CssProperties.ERROR_TEXT_COLOR);
        }
    }

    private void onBrowseForTargetDirectory() {
        final StringBuilder chooserTitle = new StringBuilder("Choose where to download '");
        chooserTitle.append(metaData.getName());
        chooserTitle.append("' to:");

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        directoryChooser.setTitle(chooserTitle.toString());

        final File selectedDirectory = directoryChooser.showDialog(window.getOwner());

        if(selectedDirectory != null) {
            final String selectedTargetDirectory = selectedDirectory.getAbsolutePath();
            if(!savePathCombo.getItems().contains(selectedTargetDirectory)) {
                savePathCombo.getItems().add(selectedTargetDirectory);
            }
            savePathCombo.getSelectionModel().select(selectedTargetDirectory);
        }
    }

    private Node layoutContent() {
        final BorderPane mainPane = new BorderPane();
        mainPane.setPrefHeight(500);
        mainPane.setPrefWidth(1100);

        final Node torrentContentsPane = buildTorrentContentsPane();

        BorderPane.setMargin(torrentContentsPane, new Insets(0, 0, 0, 10));

        mainPane.setLeft(buildLeftPane());
        mainPane.setCenter(torrentContentsPane);

        return mainPane;
    }

    private Pane buildLeftPane() {
        final VBox northPane = new VBox();
        northPane.getChildren().addAll(buildSaveOptionsPane(), buildNamePane(), buildTorrentOptionsPane());

        final HBox advancedPane = new HBox(10);
        advancedPane.setAlignment(Pos.BOTTOM_LEFT);
        advancedPane.getChildren().addAll(advancedButton, dontShowAgainCheckbox);

        final BorderPane centerPane = new BorderPane();
        centerPane.setBottom(advancedPane);

        final BorderPane leftPane = new BorderPane();
        leftPane.setTop(northPane);
        leftPane.setCenter(centerPane);

        return leftPane;
    }

    private Node buildTorrentContentsPane() {
        final GridPane labelPane = new GridPane();

        final ColumnConstraints nameColumnConstraints = new ColumnConstraints(100);
        final ColumnConstraints valueColumnConstraints = new ColumnConstraints();

        labelPane.getColumnConstraints().addAll(nameColumnConstraints, valueColumnConstraints);

        labelPane.add(new Label("Name:"), 0, 0);
        labelPane.add(new Label(metaData.getName()), 1, 0);

        labelPane.add(new Label("Comment:"), 0, 1);
        labelPane.add(new Label(metaData.getComment()), 1, 1);

        labelPane.add(new Label("Size:"), 0, 2);
        labelPane.add(fileSizeLabel, 1, 2);

        labelPane.add(new Label("Date:"), 0, 3);

        final Long creationDateInSeconds = metaData.getCreationDate();
        final String creationDate = creationDateInSeconds != null? UnitConverter.formatMillisToDate(
                creationDateInSeconds * 1000, TimeZone.getDefault()) : "";
        labelPane.add(new Label(creationDate), 1, 3);

        final HBox expandCollapseButtonsPane = new HBox(10);
        expandCollapseButtonsPane.setAlignment(Pos.CENTER_LEFT);
        expandCollapseButtonsPane.getChildren().addAll(expandAllButton, collapseAllButton);

        final HBox selectionButtonsPane = new HBox(10);
        selectionButtonsPane.setAlignment(Pos.CENTER_RIGHT);
        selectionButtonsPane.getChildren().addAll(selectAllButton, selectNoneButton);

        final BorderPane buttonsPane = new BorderPane();
        buttonsPane.setLeft(expandCollapseButtonsPane);
        buttonsPane.setRight(selectionButtonsPane);

        final VBox northPane = new VBox(10);
        northPane.getChildren().addAll(labelPane, buttonsPane);

        final ScrollPane torrentContentsScroll = new ScrollPane();
        torrentContentsScroll.setContent(fileView);
        torrentContentsScroll.setFitToWidth(true);
        torrentContentsScroll.setFitToHeight(true);

        final BorderPane torrentContentsPane = new BorderPane();
        torrentContentsPane.setPrefWidth(450);
        torrentContentsPane.setTop(northPane);
        torrentContentsPane.setCenter(torrentContentsScroll);

        BorderPane.setMargin(northPane, new Insets(0, 0, 10, 0));

        final TitledBorderPane borderedTorrentContentsPane = new TitledBorderPane("Torrent Contents",
                torrentContentsPane, BorderStyle.AMPLE, TitledBorderPane.PRIMARY_BORDER_COLOR_STYLE);

        return borderedTorrentContentsPane;
    }

    private Node buildSaveOptionsPane() {
        final BorderPane saveLocationPane = new BorderPane();
        saveLocationPane.setCenter(savePathCombo);
        saveLocationPane.setRight(browseButton);

        BorderPane.setMargin(browseButton, new Insets(0, 0, 0, 10));

        final VBox saveOptionsPane = new VBox(10);
        saveOptionsPane.getChildren().addAll(saveLocationPane, createSubFolderCheckbox);

        final TitledBorderPane borderedSaveOptionsPane = new TitledBorderPane("Save In",
                saveOptionsPane, BorderStyle.AMPLE, TitledBorderPane.PRIMARY_BORDER_COLOR_STYLE);

        return borderedSaveOptionsPane;
    }

    private Node buildNamePane() {
        final StackPane namePane = new StackPane(nameTextField);
        final TitledBorderPane borderedNamePane = new TitledBorderPane("Name",
                namePane, BorderStyle.AMPLE, TitledBorderPane.PRIMARY_BORDER_COLOR_STYLE);

        return borderedNamePane;
    }

    private Node buildTorrentOptionsPane() {
        final HBox labelPane = new HBox(5);
        labelPane.getChildren().addAll(new Label("Label: "), labelCombo);
        labelPane.setAlignment(Pos.CENTER);
        HBox.setHgrow(labelCombo, Priority.ALWAYS);
        labelCombo.setMaxWidth(Double.MAX_VALUE);

        final GridPane torrentOptionsPane = new GridPane();
        torrentOptionsPane.setVgap(5);

        final ColumnConstraints nameColumnConstraints = new ColumnConstraints(200);
        final ColumnConstraints valueColumnConstraints = new ColumnConstraints();
        valueColumnConstraints.setFillWidth(true);
        valueColumnConstraints.setMaxWidth(Double.MAX_VALUE);
        valueColumnConstraints.setHalignment(HPos.LEFT);

        torrentOptionsPane.getColumnConstraints().addAll(nameColumnConstraints, valueColumnConstraints);

        torrentOptionsPane.add(skipHashCheckbox, 0, 0);
        torrentOptionsPane.add(labelPane, 1, 0);

        torrentOptionsPane.add(startTorrentCheckbox, 0, 1);
        torrentOptionsPane.add(addToTopQueueCheckbox, 1, 1);

        GridPane.setHgrow(labelPane, Priority.ALWAYS);

        final TitledBorderPane borderedTorrentOptionsPane = new TitledBorderPane("Torrent Options",
                torrentOptionsPane, BorderStyle.AMPLE, TitledBorderPane.PRIMARY_BORDER_COLOR_STYLE);

        return borderedTorrentOptionsPane;
    }

    private void storeColumnStates() {
        TableUtils.storeColumnStates(fileView.getColumns(), GuiProperties.INFO_COLUMN_VISIBILITY,
                GuiProperties.DEFAULT_INFO_COLUMN_VISIBILITIES, GuiProperties.INFO_COLUMN_SIZE,
                GuiProperties.DEFAULT_INFO_COLUMN_SIZES, GuiProperties.INFO_COLUMN_ORDER,
                GuiProperties.DEFAULT_INFO_COLUMN_ORDER);
    }
}