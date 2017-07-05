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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.matic.torrent.gui.action.FileActionHandler;
import org.matic.torrent.gui.action.TabActionHandler;
import org.matic.torrent.gui.action.TorrentJobActionHandler;
import org.matic.torrent.gui.action.TrackerTableActionHandler;
import org.matic.torrent.gui.action.WindowActionHandler;
import org.matic.torrent.gui.action.enums.ApplicationTheme;
import org.matic.torrent.gui.custom.FilterTorrentsComboBoxSkin;
import org.matic.torrent.gui.custom.InfoPanel;
import org.matic.torrent.gui.custom.StatusBar;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.gui.table.PeerTable;
import org.matic.torrent.gui.table.TableUtils;
import org.matic.torrent.gui.table.TorrentViewTable;
import org.matic.torrent.gui.table.TrackerTable;
import org.matic.torrent.gui.tree.FileTreeViewer;
import org.matic.torrent.gui.tree.TreeTableUtils;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.queue.QueuedTorrentProgress;
import org.matic.torrent.queue.TorrentTemplate;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.utils.PeriodicTask;
import org.matic.torrent.utils.PeriodicTaskRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.stream.Collectors;

/**
 * A main application window, showing all of the GUI components.
 *
 * @author vedran
 *
 */
public final class ApplicationWindow implements PreferenceChangeListener {

    private static final List<String> TAB_NAMES = Arrays.asList(GuiProperties.FILES_TAB_ID,
            GuiProperties.INFO_TAB_ID, GuiProperties.PEERS_TAB_ID, GuiProperties.TRACKERS_TAB_ID,
            GuiProperties.PIECES_TAB_ID, GuiProperties.SPEED_TAB_ID, GuiProperties.LOGGER_TAB_ID);

    //Action handlers
    private final WindowActionHandler windowActionHandler = new WindowActionHandler();
    private final FileActionHandler fileActionHandler = new FileActionHandler();

    private final TrackerTableActionHandler trackerTableActionHandler = new TrackerTableActionHandler();
    private final TorrentJobActionHandler torrentJobActionHandler = new TorrentJobActionHandler();
    private final TabActionHandler tabActionHandler = new TabActionHandler();

    //Split pane containing torrent job view and detailed info view
    private final SplitPane verticalSplitPane = new SplitPane();

    //Split pane containing filter view and vertical split pane
    private final SplitPane horizontalSplitPane = new SplitPane();

    //View for filtering torrents according to their status
    private final TreeView<Label> filterTreeView = new TreeView<>();

    //View for displaying all torrent jobs in a table
    private final TorrentViewTable torrentViewTable;

    //Container for active torrent's tree file view
    private final TreeTableView<TorrentFileEntry> fileContentTree = new TreeTableView<>();

    //Container for all of the torrents' file tree views
    private final FileTreeViewer fileTreeViewer = new FileTreeViewer(fileContentTree);

    //View for displaying selected torrent's trackers
    private final TrackerTable trackerTable = new TrackerTable();

    //View for displaying connected peers for the selected torrent
    private final PeerTable peerTable = new PeerTable();

    //View for displaying detailed info and a torrent's progress
    private final InfoPanel infoPanel = new InfoPanel();

    //Detailed torrent info tab pane
    private final TabPane detailedInfoTabPane = new TabPane();

    //Application status below at the bottom of the window
    private final StatusBar statusBar = new StatusBar();

    //Menu item for either showing or hiding the detailed info tab pane
    private final CheckMenuItem showDetailedInfoMenuItem = new CheckMenuItem("Show _Detailed Info");

    //Menu item for selecting either compact or expanded tool bar
    private final CheckMenuItem showCompactToolbarMenuItem = new CheckMenuItem("_Narrow Toolbar");

    //Menu item for showing or hiding the status bar
    private final CheckMenuItem showStatusBarMenuItem = new CheckMenuItem("Show _Status Bar");

    //Menu item for either showing or hiding the filter view
    private final CheckMenuItem showFilterViewMenuItem = new CheckMenuItem("Show Side_bar");

    //Menu item for showing or hiding the tool bar
    private final CheckMenuItem showToolbarMenuItem = new CheckMenuItem("Show _Toolbar");

    //Menu item for showing or hiding the tab icons
    private final CheckMenuItem showTabIconsMenuItem = new CheckMenuItem("_Icons on Tabs");

    //Mapping between toolbar's buttons and their icon paths
    private final Map<String, Button> toolbarButtonsMap = new HashMap<>();

    //Mapping between details tabs and their names
    private final Map<String, Tab> detailsTabMap = new HashMap<>();

    //A service that periodically executes actions (such as GUI update)
    private final PeriodicTaskRunner guiUpdateTaskRunner = new PeriodicTaskRunner();

    //The manager for handling queued torrents states
    private final QueuedTorrentManager queuedTorrentManager;

    //An object for managing all of the communication with the available trackers
    private final TrackerManager trackerManager;

    private final Stage stage;

    public ApplicationWindow(final Stage stage, final TrackerManager trackerManager,
                             final QueuedTorrentManager queuedTorrentManager) {
        this.stage = stage;
        this.trackerManager = trackerManager;
        this.queuedTorrentManager = queuedTorrentManager;
        this.torrentViewTable = new TorrentViewTable(this.queuedTorrentManager);

        initComponents();
    }

    @Override
    public void preferenceChange(final PreferenceChangeEvent changeEvent) {
        final String propName = changeEvent.getKey();
        if(propName.startsWith("gui.")) {
            switch(propName) {
                case GuiProperties.ALTERNATE_LIST_ROW_COLOR:
                    TableUtils.refresh(fileContentTree);
                    torrentViewTable.refresh();
                    trackerTable.refresh();
                    break;
            }
        }
    }

    private void initComponents() {
        setMenuAccelerators();
        initDetailedInfoTabPane();

        final BorderPane mainPane = new BorderPane();
        mainPane.setTop(buildMenuBarPane());
        mainPane.setCenter(buildContentPane());

        initStatusBar(mainPane);

        torrentViewTable.addSelectionListener(this::onTorrentJobSelection);
        trackerTable.onTrackerDeletionRequested(views ->
                trackerTableActionHandler.onTrackerDeletion(views, queuedTorrentManager, trackerTable, stage));
        trackerTable.onTrackableUpdateRequested(views ->
                trackerTableActionHandler.onTrackerUpdate(views, trackerManager));
        trackerTable.onTrackersAdded(urls -> trackerTableActionHandler.onTrackersAdded(urls, queuedTorrentManager,
                torrentViewTable.getSelectedJobs().get(0), trackerTable), stage,
                torrentViewTable.bindOnEmpty());
        ApplicationPreferences.addPreferenceChangeListener(this);

        TreeTableUtils.addFileListingViewColumns(fileContentTree, true);
        TreeTableUtils.setupFileListingView(fileContentTree, fileTreeViewer);

        loadStoredTorrents();

        stage.setScene(initScene(mainPane));
        stage.setOnCloseRequest(this::onShutdown);
        stage.setTitle(ClientProperties.CLIENT_NAME);
        stage.show();

        initPeriodicTaskRunner();
    }

    private void initStatusBar(final BorderPane contentPane) {
        statusBar.getStyleClass().add(CssProperties.STATUS_BAR);
        final boolean statusBarShown = ApplicationPreferences.getProperty(
                GuiProperties.STATUSBAR_VISIBLE, GuiProperties.DEFAULT_STATUSBAR_VISIBLE);
        showStatusBarMenuItem.setSelected(statusBarShown);
        showStatusBar(contentPane, showStatusBarMenuItem.isSelected());
        showStatusBarMenuItem.selectedProperty().addListener((obs, oldV, showStatusBar) ->
                showStatusBar(contentPane, showStatusBar));
    }

    private Scene initScene(final Pane contentPane) {
        final double windowWidth = ApplicationPreferences.getProperty(
                GuiProperties.APPLICATION_WINDOW_WIDTH, GuiProperties.DEFAULT_APPLICATION_WINDOW_WIDTH);
        final double windowHeight = ApplicationPreferences.getProperty(
                GuiProperties.APPLICATION_WINDOW_HEIGHT, GuiProperties.DEFAULT_APPLICATION_WINDOW_HEIGHT);

        final Scene scene = new Scene(contentPane, windowWidth, windowHeight);

        final String themeName = ApplicationPreferences.getProperty(
                GuiProperties.APPLICATION_THEME, ApplicationTheme.LIGHT.name().toLowerCase());
        scene.getStylesheets().add(GuiProperties.THEME_STYLESHEET_PATH_TEMPLATE.replace("?", themeName)
                + GuiProperties.THEME_UI_STYLE_CSS);
        scene.getStylesheets().addListener((ListChangeListener<String>) l -> onStylesheetChanged(l));

        final double windowXPosition = ApplicationPreferences.getProperty(
                GuiProperties.APPLICATION_WINDOW_POSITION_X, GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION);
        final double windowYPosition = ApplicationPreferences.getProperty(
                GuiProperties.APPLICATION_WINDOW_POSITION_Y, GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION);

        if(windowXPosition != GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION &&
                windowYPosition != GuiProperties.DEFAULT_APPLICATION_WINDOW_POSITION) {
            stage.setX(windowXPosition);
            stage.setY(windowYPosition);
        }
        else {
            stage.centerOnScreen();
        }

        return scene;
    }

    private void initDetailedInfoTabPane() {
        final String selectedTabId = ApplicationPreferences.getProperty(
                GuiProperties.SELECTED_TAB_ID, GuiProperties.DEFAULT_SELECTED_TAB_ID);

        final Collection<Tab> tabs = buildDetailedInfoTabs(selectedTabId);
        final ObservableList<Tab> tabList = detailedInfoTabPane.getTabs();
        tabList.addAll(tabs);

        tabActionHandler.onTabAction(tabList, TAB_NAMES, detailedInfoTabPane, detailsTabMap);
    }

    private void setMenuAccelerators() {
        showCompactToolbarMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        showFilterViewMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F7));
        showStatusBarMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F6));
        showDetailedInfoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        showToolbarMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F4));
    }

    private void initPeriodicTaskRunner() {
        final long guiUpdateInterval = ApplicationPreferences.getProperty(
                GuiProperties.GUI_UPDATE_INTERVAL, GuiProperties.DEFAULT_GUI_UPDATE_INTERVAL);

        final PeriodicTask periodicTask = new PeriodicTask(() -> Platform.runLater(this::updateGui), guiUpdateInterval);

        guiUpdateTaskRunner.addTask(periodicTask);
        guiUpdateTaskRunner.setPeriod(Duration.seconds(1));
        guiUpdateTaskRunner.start();
    }

    private void updateGui() {
        //Don't update the GUI if the window itself is minimized/hidden
        if(stage.isIconified()) {
            return;
        }

        final List<TorrentView> selectedTorrents = torrentViewTable.getSelectedJobs();
        if(!selectedTorrents.isEmpty()) {
            //Render Detailed Info pane contents only if it is visible
            if(showDetailedInfoMenuItem.isSelected()) {
                final Tab selectedTab = detailedInfoTabPane.getSelectionModel().getSelectedItem();
                switch(selectedTab.getText()) {
                    case GuiProperties.TRACKERS_TAB_ID:
                        //Update and render tracker statistics if Trackers tab is selected
                        trackerTable.updateContent();
                        trackerTable.sort();
                        break;
                    case GuiProperties.FILES_TAB_ID:
                        //Update and render file progress statistics if Files tab is selected
                        TreeTableUtils.sort(fileContentTree);
                        break;
                    case GuiProperties.INFO_TAB_ID:
                        infoPanel.setContent(selectedTorrents.get(selectedTorrents.size() - 1));
                        break;
                    case GuiProperties.PEERS_TAB_ID:
                        //Update and render peer statistics if Peers tab is selected
                        //TODO: Update peer table
                        peerTable.sort();
                        break;
                }
            }
        }
    }

    private void initFilterTreeView(final ComboBox<String> searchFilterCombo) {
        filterTreeView.getStyleClass().add(CssProperties.FILTER_LIST_VIEW);
        filterTreeView.setRoot(buildFilterTreeViewItems(searchFilterCombo));
        filterTreeView.setShowRoot(false);
        filterTreeView.getSelectionModel().select(0);
        filterTreeView.requestFocus();

        filterTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                torrentViewTable.filter(newV.getValue().getId(), searchFilterCombo.getEditor().getText()));

        final MenuItem addTorrentMenuItem = new MenuItem("_Add Torrent...");
        final MenuItem addRssFeedMenuItem = new MenuItem("A_dd RSS Feed...");

        addTorrentMenuItem.setOnAction(e ->
                onAddTorrent(fileActionHandler.onFileOpen(stage, fileTreeViewer)));

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(addTorrentMenuItem, addRssFeedMenuItem);
        filterTreeView.setContextMenu(contextMenu);
    }

    private TreeItem<Label> buildFilterTreeViewItems(final ComboBox<String> searchFilterCombo) {
        final String[] torrentLabelNames = {TorrentViewTable.DOWNLOADING_FILTER,
                TorrentViewTable.SEEDING_FILTER, TorrentViewTable.COMPLETED_FILTER, TorrentViewTable.ACTIVE_FILTER,
                TorrentViewTable.INACTIVE_FILTER};
        final Label[] torrentLabels = new Label[torrentLabelNames.length];

        for(int i = 0; i < torrentLabelNames.length; ++i) {
            torrentLabels[i] = new Label(torrentLabelNames[i] + " (0)");
            torrentLabels[i].setId(torrentLabelNames[i]);
            torrentLabels[i].getStyleClass().add(CssProperties.FILTER_LIST_CHILD_CELL);
        }

        final List<TreeItem<Label>> torrentNodeElements = Arrays.asList(torrentLabels).stream().map(
                TreeItem::new).collect(Collectors.toList());

        final List<TreeItem<Label>> labelsNodeElements = Arrays.asList(
                TorrentViewTable.NO_LABEL_FILTER).stream().map(labelName -> {
            final Label label = new Label(labelName + " (0)");
            label.setId(labelName);
            label.getStyleClass().add(CssProperties.FILTER_LIST_CHILD_CELL);
            return new TreeItem<>(label);
        }).collect(Collectors.toList());

        final String torrentsRootLabelId = TorrentViewTable.TORRENTS_FILTER;
        final Label torrentsRootLabel = new Label(torrentsRootLabelId + " (0)");

        torrentsRootLabel.setId(torrentsRootLabelId);
        torrentsRootLabel.getStyleClass().add(CssProperties.FILTER_LIST_ROOT_CELL);
        torrentsRootLabel.setGraphic(new ImageView(ImageUtils.DOWNLOADS_IMAGE));
        final TreeItem<Label> torrentsRootNode = new TreeItem<>(torrentsRootLabel);
        torrentsRootNode.setExpanded(true);
        torrentsRootNode.getChildren().addAll(torrentNodeElements);

        final String labelsRootLabelId = TorrentViewTable.LABELS_FILTER;
        final Label labelsRootLabel = new Label(labelsRootLabelId);
        labelsRootLabel.setId(labelsRootLabelId);
        labelsRootLabel.getStyleClass().add(CssProperties.FILTER_LIST_ROOT_CELL);
        labelsRootLabel.setGraphic(new ImageView(ImageUtils.LABEL_IMAGE));
        final TreeItem<Label> labelsRootNode = new TreeItem<>(labelsRootLabel);
        labelsRootNode.setExpanded(true);
        labelsRootNode.getChildren().addAll(labelsNodeElements);

        final String rssFeedsRootLabelId = TorrentViewTable.FEEDS_FILTER;
        final Label rssFeedsRootLabel = new Label(rssFeedsRootLabelId + " (0)");
        rssFeedsRootLabel.setId(rssFeedsRootLabelId);
        rssFeedsRootLabel.getStyleClass().add(CssProperties.FILTER_LIST_ROOT_CELL_WITHOUT_CHILDREN);
        rssFeedsRootLabel.setGraphic(new ImageView(ImageUtils.RSS_IMAGE));
        final TreeItem<Label> rssFeedsRootNode = new TreeItem<>(rssFeedsRootLabel);
        rssFeedsRootNode.setExpanded(true);

        //TODO: Extract property listeners' code to a common method
        torrentViewTable.totalTorrentsProperty().addListener((obs, oldV, newV) ->
                Platform.runLater(() -> {
                        torrentsRootLabel.setText(torrentsRootLabelId + " (" + newV + ")");
                        torrentViewTable.filter(
                                filterTreeView.getSelectionModel().getSelectedItem().getValue().getId(),
                                searchFilterCombo.getEditor().getText());
                }));

        torrentViewTable.activeTorrentsProperty().addListener((obs, oldV, newV) ->
                Platform.runLater(() -> {
                        torrentLabels[3].setText(torrentLabelNames[3] + " (" + newV + ")");
                        torrentViewTable.filter(
                            filterTreeView.getSelectionModel().getSelectedItem().getValue().getId(),
                            searchFilterCombo.getEditor().getText());
                }));

        torrentViewTable.inactiveTorrentsProperty().addListener((obs, oldV, newV) ->
                Platform.runLater(() -> {
                        torrentLabels[4].setText(torrentLabelNames[4] + " (" + newV + ")");
                        torrentViewTable.filter(
                                filterTreeView.getSelectionModel().getSelectedItem().getValue().getId(),
                                searchFilterCombo.getEditor().getText());
        }));

        final List<TreeItem<Label>> allNodes = Arrays.asList(torrentsRootNode, labelsRootNode, rssFeedsRootNode);

        final TreeItem<Label> rootNode = new TreeItem<>();
        rootNode.setExpanded(true);
        rootNode.getChildren().addAll(allNodes);

        return rootNode;
    }

    private Pane buildMenuBarPane() {
        final VBox northPane = new VBox();
        northPane.getChildren().add(buildMenuBar());

        return northPane;
    }

    private Pane buildContentPane() {
        final BorderPane mainPane = new BorderPane();

        final ComboBox<String> searchFilterCombo = buildSearchFilterComboBox();
        final ToolBar toolbar = buildToolbar(searchFilterCombo);

        verticalSplitPane.getStyleClass().add(CssProperties.SPLIT_PANE);
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.getItems().addAll(buildTorrentJobTable(), detailedInfoTabPane);

        final BorderPane centerPane = new BorderPane();
        centerPane.setCenter(verticalSplitPane);

        final boolean toolbarShown = ApplicationPreferences.getProperty(
                GuiProperties.TOOLBAR_VISIBLE, GuiProperties.DEFAULT_TOOLBAR_VISIBLE);
        showToolbarMenuItem.setSelected(toolbarShown);
        showToolbar(mainPane, centerPane, toolbar, showToolbarMenuItem.isSelected());
        showToolbarMenuItem.selectedProperty().addListener((obs, oldV, showToolbar) ->
                showToolbar(mainPane, centerPane, toolbar, showToolbar));

        final boolean compactToolbarShown = ApplicationPreferences.getProperty(
                GuiProperties.COMPACT_TOOLBAR, GuiProperties.DEFAULT_COMPACT_TOOLBAR);
        showCompactToolbarMenuItem.setSelected(compactToolbarShown);
        showCompactToolbar(mainPane, centerPane, toolbar, showCompactToolbarMenuItem.isSelected());
        showCompactToolbarMenuItem.selectedProperty().addListener((obs, oldV, showCompact) ->
                showCompactToolbar(mainPane, centerPane, toolbar, showCompact));
        initFilterTreeView(searchFilterCombo);

        horizontalSplitPane.getStyleClass().add(CssProperties.SPLIT_PANE);
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizontalSplitPane.getItems().addAll(filterTreeView, centerPane);

        final double horizontalDividerPosition = ApplicationPreferences.getProperty(
                GuiProperties.HORIZONTAL_DIVIDER_POSITION, GuiProperties.DEFAULT_HORIZONTAL_DIVIDER_POSITION);
        horizontalSplitPane.setDividerPosition(0, horizontalDividerPosition);

        final double verticalDividerPosition = ApplicationPreferences.getProperty(
                GuiProperties.VERTICAL_DIVIDER_POSITION, GuiProperties.DEFAULT_VERTICAL_DIVIDER_POSITION);
        verticalSplitPane.setDividerPosition(0, verticalDividerPosition);

        final boolean detailedInfoShown = ApplicationPreferences.getProperty(
                GuiProperties.DETAILED_INFO_VISIBLE, GuiProperties.DEFAULT_DETAILED_INFO_VISIBLE);
        showDetailedInfoMenuItem.setSelected(detailedInfoShown);
        showDetailedInfo(verticalSplitPane, showDetailedInfoMenuItem.isSelected());
        showDetailedInfoMenuItem.selectedProperty().addListener((obs, oldV, showDetailedInfo) ->
                showDetailedInfo(verticalSplitPane, showDetailedInfo));

        final boolean filterViewShown = ApplicationPreferences.getProperty(
                GuiProperties.FILTER_VIEW_VISIBLE, GuiProperties.DEFAULT_FILTER_VIEW_VISIBLE);
        showFilterViewMenuItem.setSelected(filterViewShown);
        showFilterView(horizontalSplitPane, showFilterViewMenuItem.isSelected());
        showFilterViewMenuItem.selectedProperty().addListener((obs, oldV, showFilterView) ->
                showFilterView(horizontalSplitPane, showFilterView));

        SplitPane.setResizableWithParent(filterTreeView, Boolean.FALSE);
        mainPane.setCenter(horizontalSplitPane);

        return mainPane;
    }

    private void showStatusBar(final BorderPane mainPane, final boolean showStatusBar) {
        mainPane.setBottom(showStatusBar? statusBar : null);
    }

    private void showDetailedInfo(final SplitPane verticalSplitPane, final boolean showDetailedInfo) {
        final ObservableList<Node> items = verticalSplitPane.getItems();
        if(showDetailedInfo && !items.contains(detailedInfoTabPane)) {
            items.add(1, detailedInfoTabPane);
            verticalSplitPane.setDividerPosition(0, detailedInfoTabPane.getPrefHeight());
        }
        else if(!showDetailedInfo) {
            detailedInfoTabPane.setPrefHeight(verticalSplitPane.getDividerPositions()[0]);
            items.remove(detailedInfoTabPane);
        }
    }

    private void showFilterView(final SplitPane horizontalSplitPane, final boolean showFilterView) {
        final ObservableList<Node> items = horizontalSplitPane.getItems();
        if(showFilterView && !items.contains(filterTreeView)) {
            items.add(0, filterTreeView);
            horizontalSplitPane.setDividerPosition(0, filterTreeView.getPrefWidth());
        }
        else if(!showFilterView) {
            filterTreeView.setPrefWidth(horizontalSplitPane.getDividerPositions()[0]);
            items.remove(filterTreeView);
        }
    }

    private void showCompactToolbar(final BorderPane mainPane, final BorderPane centerPane,
                                    final ToolBar toolbar, final boolean showCompact) {
        final boolean isToolbarHidden = !showToolbarMenuItem.isSelected();
        mainPane.setTop(showCompact || isToolbarHidden? null : toolbar);
        centerPane.setTop(!showCompact || isToolbarHidden? null : toolbar);
        toolbar.setId(showCompact? centerPane.getId() : mainPane.getId());

        final Button pauseButton = toolbarButtonsMap.get(ImageUtils.PAUSE_ICON_LOCATION);
        final Button rssButton = toolbarButtonsMap.get(ImageUtils.RSS_ICON_LOCATION);
        final ObservableList<Node> toolbarButtons = toolbar.getItems();

        if(showCompact) {
            toolbarButtons.removeAll(pauseButton, rssButton);
        }
        else if(!isExpandedToolbar(toolbar)) {
            //Add Pause button after Start Torrent and RSS button after Add from URL button respectively
            final int rssButtonIndex = toolbarButtons.indexOf(toolbarButtonsMap.get(ImageUtils.LINK_ICON_LOCATION));
            toolbarButtons.add(rssButtonIndex + 1, rssButton);

            final int downloadButtonIndex = toolbarButtons.indexOf(toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION));
            toolbarButtons.add(downloadButtonIndex + 1, pauseButton);
        }
    }

    private void showToolbar(final BorderPane mainPane, final BorderPane centerPane,
                             final ToolBar toolbar, final boolean showToolbar) {
        if(isExpandedToolbar(toolbar)) {
            mainPane.setTop(showToolbar? toolbar : null);
        }
        else {
            centerPane.setTop(showToolbar? toolbar : null);
        }
    }

    private boolean isExpandedToolbar(final ToolBar toolbar) {
        final ObservableList<Node> toolbarButtons = toolbar.getItems();
        return toolbarButtons.contains(toolbarButtonsMap.get(ImageUtils.PAUSE_ICON_LOCATION)) &&
                toolbarButtons.contains(toolbarButtonsMap.get(ImageUtils.RSS_ICON_LOCATION));
    }

    private ToolBar buildToolbar(final ComboBox<String> searchFilterCombo) {
        final String[] buttonUrls = new String[]{ImageUtils.ADD_ICON_LOCATION,
                ImageUtils.LINK_ICON_LOCATION, ImageUtils.RSS_ICON_LOCATION,
                ImageUtils.NEW_ICON_LOCATION, ImageUtils.DELETE_ICON_LOCATION,
                ImageUtils.DOWNLOAD_ICON_LOCATION, ImageUtils.PAUSE_ICON_LOCATION,
                ImageUtils.STOP_ICON_LOCATION, ImageUtils.UP_ICON_LOCATION,
                ImageUtils.DOWN_ICON_LOCATION, ImageUtils.LOCK_ICON_LOCATION,
                ImageUtils.REMOTE_ICON_LOCATION, ImageUtils.SETTINGS_ICON_LOCATION};

        final String[] buttonNames = {"Add Torrent", "Add Torrent from URL", "Add RSS Feed",
                "Create New Torrent", "Remove", "Start Torrent", "Pause Torrent",
                "Stop Torrent", "Move Up Queue", "Move Down Queue", "Unlock Bundle", "Remote", "Preferences"};

        final boolean[] buttonStates = {false, false, false, false, true, true, true, true, true, true, true, false, false};

        final Button[] toolbarButtons = new Button[buttonUrls.length];
        for(int i = 0; i < toolbarButtons.length; ++i) {
            toolbarButtons[i] = buildToolbarButton(buttonUrls[i], buttonNames[i], buttonStates[i]);
        }

        setupToolbarButtonActions();

        final String themeName = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_THEME, "light");
        refreshToolbarIcons(toolbarButtonsMap, themeName);

        final HBox separatorBox = new HBox();
        HBox.setHgrow(separatorBox, Priority.ALWAYS);

        final Node[] toolbarContents = {toolbarButtons[0], toolbarButtons[1], toolbarButtons[2],
                buildToolbarSeparator(), toolbarButtons[3], buildToolbarSeparator(),
                toolbarButtons[4], buildToolbarSeparator(), toolbarButtons[5], toolbarButtons[6],
                toolbarButtons[7], buildToolbarSeparator(), toolbarButtons[8], toolbarButtons[9],
                buildToolbarSeparator(), toolbarButtons[10], buildToolbarSeparator(), separatorBox,
                searchFilterCombo, buildToolbarSeparator(), toolbarButtons[11], toolbarButtons[12]};

        final ToolBar toolBar = new ToolBar(toolbarContents);
        return toolBar;
    }

    private void setupToolbarButtonActions() {
        toolbarButtonsMap.get(ImageUtils.ADD_ICON_LOCATION).setOnAction(
                event -> onAddTorrent(fileActionHandler.onFileOpen(stage, fileTreeViewer)));
        toolbarButtonsMap.get(ImageUtils.LINK_ICON_LOCATION).setOnAction(
                event -> onAddTorrent(fileActionHandler.onLoadUrl(stage, fileTreeViewer)));
        toolbarButtonsMap.get(ImageUtils.SETTINGS_ICON_LOCATION).setOnAction(
                event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));

        final Button deleteButton = toolbarButtonsMap.get(ImageUtils.DELETE_ICON_LOCATION);
        final ContextMenu deleteButtonContextMenu = buildRemoveButtonContextMenu();
        deleteButton.setOnMouseClicked(event -> {
            deleteButtonContextMenu.hide();
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                deleteButtonContextMenu.show(deleteButton, event.getScreenX(), event.getScreenY());
                return;
            }
            onDeleteTorrents(torrentViewTable.getSelectedJobs());
        });
        deleteButton.disableProperty().addListener((obs, oldV, newV) ->
                onDeleteButtonStateChanged(deleteButton, newV));

        toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION).setOnAction(
                event -> torrentJobActionHandler.onRequestTorrentStateChange(queuedTorrentManager,
                        torrentViewTable.getSelectedJobs(), TorrentStatus.ACTIVE,
                        toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION),
                        toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION)));
        toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION).setOnAction(
                event -> torrentJobActionHandler.onRequestTorrentStateChange(queuedTorrentManager,
                        torrentViewTable.getSelectedJobs(), TorrentStatus.STOPPED,
                        toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION),
                        toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION)));

        final Button prioUpButton = toolbarButtonsMap.get(ImageUtils.UP_ICON_LOCATION);
        final Button prioDownButton = toolbarButtonsMap.get(ImageUtils.DOWN_ICON_LOCATION);

        prioUpButton.setOnAction(event -> {
            final ObservableList<TorrentView> selectedTorrents = torrentViewTable.getSelectedJobs();
            torrentJobActionHandler.onRequestTorrentPriorityChange(queuedTorrentManager,
                   selectedTorrents, PriorityChange.HIGHER);
            torrentViewTable.sort();
            if(selectedTorrents.size() == 1) {
                prioUpButton.setDisable(selectedTorrents.get(0).getPriority() == 1);
                prioDownButton.setDisable(false);
            }
        });
        prioDownButton.setOnAction(event -> {
            final ObservableList<TorrentView> selectedTorrents = torrentViewTable.getSelectedJobs();
            torrentJobActionHandler.onRequestTorrentPriorityChange(queuedTorrentManager,
                    selectedTorrents, PriorityChange.LOWER);
            torrentViewTable.sort();

            if(selectedTorrents.size() == 1) {
                prioDownButton.setDisable(
                        selectedTorrents.get(0).getPriority() == queuedTorrentManager.getTorrentsOnQueue());
                prioUpButton.setDisable(false);
            }
        });
    }

    private ComboBox<String> buildSearchFilterComboBox() {
        final ComboBox<String> searchFilterCombo = new ComboBox<>();
        searchFilterCombo.setPrefWidth(250);

        //TODO: Extract CellFactory and ListCell to own classes
        searchFilterCombo.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(final ListView<String> listView) {
                final Text filterGraphic = new Text("Y");
                filterGraphic.setFill(Color.BROWN);
                filterGraphic.setId("Filter My Torrents");

                return new ListCell<String>() {
                    @Override
                    protected void updateItem(final String value, final boolean selected) {
                        super.setText(selected? "" : value);
                        if("Filter My Torrents".equals(value) && !selected) {
                            super.setGraphic(filterGraphic);
                        }
                    }
                };
            }
        });

        final String searchProvidersFilterName = "Manage Search Providers...";
        final String filterTorrentsFilterName = "Filter My Torrents";
        searchFilterCombo.setItems(FXCollections.observableArrayList(
                searchProvidersFilterName, filterTorrentsFilterName));
        searchFilterCombo.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            final String selectedSearchFilter = searchFilterCombo.getId();
            if(filterTorrentsFilterName.equals(selectedSearchFilter)) {
                //Filter shown torrents (possibly already filtered) to match the search term
                torrentViewTable.filter(filterTreeView.getSelectionModel().getSelectedItem().getValue().getId(), newV);
            }
        });

        final FilterTorrentsComboBoxSkin<String> comboSkin = new FilterTorrentsComboBoxSkin<>(searchFilterCombo);

        searchFilterCombo.getEditor().getStyleClass().add("filter-text-field");
        searchFilterCombo.setSkin(comboSkin);
        searchFilterCombo.setPromptText("<Filter Torrents>");
        searchFilterCombo.setEditable(true);

        return searchFilterCombo;
    }

    private void refreshToolbarIcons(final Map<String, Button> buttonMap, final String themeName) {
        final String iconPath = GuiProperties.THEME_STYLESHEET_PATH_TEMPLATE.replace("?", themeName);
        buttonMap.entrySet().forEach(buttonLocation -> {
            final ImageView imageView = ImageUtils.createImageView(new Image(
                            getClass().getResourceAsStream(iconPath + buttonLocation.getKey())),
                    ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, ImageUtils.ICON_SIZE_TOOLBAR,
                    ImageUtils.ICON_SIZE_TOOLBAR);
            buttonLocation.getValue().setGraphic(imageView);
        });
        final Button deleteButton = buttonMap.get(ImageUtils.DELETE_ICON_LOCATION);
        if(!deleteButton.isDisabled()) {
            ImageUtils.colorize((ImageView)deleteButton.getGraphic(), ImageUtils.BUTTON_COLOR_DELETE);
        }
    }

    private Button buildToolbarButton(final String iconPath, final String tooltip, final boolean disabled) {
        final Button button = new Button();
        button.getStyleClass().add(CssProperties.TOOLBAR_BUTTON);
        button.setTooltip(new Tooltip(tooltip));
        button.setDisable(disabled);

        toolbarButtonsMap.put(iconPath, button);
        return button;
    }

    private Separator buildToolbarSeparator() {
        final Separator separator = new Separator(Orientation.VERTICAL);
        separator.getStyleClass().add(CssProperties.TOOLBAR_SEPARATOR);
        separator.setDisable(true);
        return separator;
    }

    private ScrollPane buildTorrentJobTable() {
        final ScrollPane torrentJobTableScroll = new ScrollPane();
        torrentJobTableScroll.setFitToWidth(true);
        torrentJobTableScroll.setFitToHeight(true);
        torrentViewTable.wrapWith(torrentJobTableScroll);

        return torrentJobTableScroll;
    }

    private Collection<Tab> buildDetailedInfoTabs(final String selectedTabId) {
        final Map<Tab, ImageView> tabImageViews = new LinkedHashMap<>();
        final boolean tabIconsShown = ApplicationPreferences.getProperty(
                GuiProperties.TAB_ICONS_VISIBLE, GuiProperties.DEFAULT_TAB_ICONS_VISIBLE);
        showTabIconsMenuItem.setSelected(tabIconsShown);

        final String[] tabIconPaths = {ImageUtils.TAB_FILES_ICON_LOCATION,
                ImageUtils.TAB_INFO_ICON_LOCATION, ImageUtils.TAB_PEERS_ICON_LOCATION,
                ImageUtils.TAB_TRACKERS_ICON_LOCATION, ImageUtils.TAB_PIECES_ICON_LOCATION,
                ImageUtils.TAB_SPEED_ICON_LOCATION, ImageUtils.TAB_LOGGER_ICON_LOCATION};

        for(int i = 0; i < TAB_NAMES.size(); ++i) {
            final Image tabImage = new Image(getClass().getResourceAsStream(tabIconPaths[i]));
            final String tabName = TAB_NAMES.get(i);
            final Tab tab = new Tab(tabName);
            tab.setId(tabName);
            tab.setClosable(false);
            if(selectedTabId.equals(tab.getId())) {
                detailedInfoTabPane.getSelectionModel().select(tab);
            }
            tab.setOnSelectionChanged(event -> {
                final Color tabImageColor = tab.isSelected()? ImageUtils.ICON_COLOR : ImageUtils.INACTIVE_TAB_COLOR;

                final ImageView imageView = ImageUtils.createImageView(tabImage,
                        ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, ImageUtils.ICON_SIZE_TAB, ImageUtils.ICON_SIZE_TAB);
                ImageUtils.colorize(imageView, tabImageColor);

                tabImageViews.put(tab, imageView);
                if(showTabIconsMenuItem.isSelected()) {
                    tab.setGraphic(imageView);
                }
                updateGui();
            });
            final ImageView imageView = ImageUtils.createImageView(tabImage,
                    ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, ImageUtils.ICON_SIZE_TAB, ImageUtils.ICON_SIZE_TAB);
            ImageUtils.colorize(imageView, Color.rgb(162, 170, 156));

            tabImageViews.put(tab, imageView);
            detailsTabMap.put(tabName, tab);
        }

        final Consumer<Boolean> setTabGraphic = set ->
                tabImageViews.keySet().forEach(tab -> tab.setGraphic(set? tabImageViews.get(tab) : null));
        setTabGraphic.accept(tabIconsShown);
        showTabIconsMenuItem.selectedProperty().addListener((obs, oldV, showTabIcons) ->
                setTabGraphic.accept(showTabIcons));

        final ScrollPane trackerTableScroll = new ScrollPane();
        trackerTable.wrapWith(trackerTableScroll);
        trackerTableScroll.setFitToHeight(true);
        trackerTableScroll.setFitToWidth(true);

        final ScrollPane torrentContentTreeScroll = new ScrollPane();
        torrentContentTreeScroll.setContent(fileContentTree);
        torrentContentTreeScroll.setFitToHeight(true);
        torrentContentTreeScroll.setFitToWidth(true);

        final ScrollPane infoPaneScroll = new ScrollPane();
        infoPaneScroll.setContent(infoPanel);
        infoPaneScroll.setFitToHeight(true);
        infoPaneScroll.setFitToWidth(true);

        final ScrollPane peerTableScroll = new ScrollPane();
        peerTable.wrapWith(peerTableScroll);
        peerTableScroll.setFitToHeight(true);
        peerTableScroll.setFitToWidth(true);

        detailsTabMap.get(GuiProperties.FILES_TAB_ID).setContent(torrentContentTreeScroll);
        detailsTabMap.get(GuiProperties.INFO_TAB_ID).setContent(infoPaneScroll);
        detailsTabMap.get(GuiProperties.TRACKERS_TAB_ID).setContent(trackerTableScroll);
        detailsTabMap.get(GuiProperties.PEERS_TAB_ID).setContent(peerTableScroll);

        return tabImageViews.keySet();
    }

    private MenuBar buildMenuBar() {
        final MenuBar menuBar = new MenuBar();

        menuBar.getMenus().addAll(buildFileMenu(), buildOptionsMenu(), buildHelpMenu());

        return menuBar;
    }

    private Menu buildFileMenu() {
        final Menu fileMenu = new Menu("_File");
        fileMenu.setMnemonicParsing(true);

        final MenuItem addTorrentMenuItem = new MenuItem("_Add Torrent...");
        addTorrentMenuItem.setOnAction(event -> onAddTorrent(fileActionHandler.onFileOpen(stage, fileTreeViewer)));
        addTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));

        final MenuItem addTorrentAndChooseDirMenuItem = new MenuItem("A_dd Torrent (choose save dir)...");
        addTorrentAndChooseDirMenuItem.setOnAction(event -> fileActionHandler.onFileOpenAndChooseSaveLocation(stage));
        addTorrentAndChooseDirMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));

        final MenuItem addTorrentFromUrlMenuItem = new MenuItem("Add Torrent from _URL...");
        addTorrentFromUrlMenuItem.setOnAction(event -> onAddTorrent(fileActionHandler.onLoadUrl(stage, fileTreeViewer)));
        addTorrentFromUrlMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+U"));

        final MenuItem addRssFeedMenuItem = new MenuItem("Add RSS Feed...");

        final MenuItem createTorrentMenuItem = new MenuItem("_Create New Torrent...");
        createTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));

        final MenuItem exitMenuItem = new MenuItem("E_xit");
        exitMenuItem.setOnAction(this::onShutdown);

        fileMenu.getItems().addAll(addTorrentMenuItem, addTorrentAndChooseDirMenuItem,
                addTorrentFromUrlMenuItem, addRssFeedMenuItem, new SeparatorMenuItem(),
                createTorrentMenuItem, new SeparatorMenuItem(), exitMenuItem);

        return fileMenu;
    }

    private Menu buildOptionsMenu() {
        final Menu optionsMenu = new Menu("_Options");
        optionsMenu.setMnemonicParsing(true);

        final MenuItem preferencesMenuItem = new MenuItem("_Preferences...");
        preferencesMenuItem.setOnAction(event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));
        preferencesMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+P"));

        final RadioMenuItem[] shutdownOptionsMenuItems = {new RadioMenuItem("Disabled"),
                new RadioMenuItem("Quit when Downloads Complete"),
                new RadioMenuItem("Quit when Everything Completes"),
                new RadioMenuItem("Reboot when Downloads Complete"),
                new RadioMenuItem("Reboot when Everything Completes"),
                new RadioMenuItem("Shutdown when Downloads Complete"),
                new RadioMenuItem("Shutdown when Everything Completes")};

        final ToggleGroup shutdownOptionsToggle = new ToggleGroup();
        Arrays.stream(shutdownOptionsMenuItems).forEach(i -> i.setToggleGroup(shutdownOptionsToggle));
        shutdownOptionsMenuItems[0].setSelected(true);

        final Menu autoShutdownMenu = new Menu("_Auto Shutdown");
        autoShutdownMenu.getItems().addAll(shutdownOptionsMenuItems[0], new SeparatorMenuItem(),
                shutdownOptionsMenuItems[1], shutdownOptionsMenuItems[2], new SeparatorMenuItem(),
                shutdownOptionsMenuItems[3],shutdownOptionsMenuItems[4], new SeparatorMenuItem(),
                shutdownOptionsMenuItems[5], shutdownOptionsMenuItems[6]);

        optionsMenu.getItems().addAll(preferencesMenuItem, new SeparatorMenuItem(), showToolbarMenuItem,
                showDetailedInfoMenuItem, showStatusBarMenuItem, showFilterViewMenuItem, showCompactToolbarMenuItem,
                new SeparatorMenuItem(), showTabIconsMenuItem, new SeparatorMenuItem(), autoShutdownMenu);

        return optionsMenu;
    }

    private Menu buildHelpMenu() {
        final Menu helpMenu = new Menu("_Help");
        helpMenu.setMnemonicParsing(true);

        return helpMenu;
    }

    private ContextMenu buildRemoveButtonContextMenu() {
        final ContextMenu removeOptionsMenu = new ContextMenu();

        final List<RadioMenuItem> removeOptionMenuItems = Arrays.asList(new RadioMenuItem[]{
                new RadioMenuItem("Remove"), new RadioMenuItem("Remove and delete .torrent"),
                new RadioMenuItem("Remove and delete .torrent + Data"),
                new RadioMenuItem("Remove and delete Data")});

        final ToggleGroup removeOptionsToggle = new ToggleGroup();
        removeOptionMenuItems.forEach(i -> i.setToggleGroup(removeOptionsToggle));
        removeOptionMenuItems.get(0).setSelected(true);

        removeOptionsMenu.getItems().addAll(removeOptionMenuItems);
        removeOptionsMenu.getItems().addAll(new SeparatorMenuItem(),
                new CheckMenuItem("Move to trash if possible"));

        return removeOptionsMenu;
    }

    private void onAddTorrent(final AddedTorrentOptions torrentOptions) {
        if(torrentOptions == null) {
            return;
        }

        final QueuedTorrentMetaData metaData = torrentOptions.getMetaData();
        final InfoHash infoHash = metaData.getInfoHash();

        final Optional<TorrentView> torrentMatch = torrentViewTable.find(infoHash);

        if(torrentMatch.isPresent()) {
            final Alert existingTorrentAlert = new Alert(AlertType.ERROR,
                    "The torrent already exists.\n" +
                            "Would you like to load trackers from it?",
                    ButtonType.OK, ButtonType.CANCEL);
            existingTorrentAlert.initOwner(stage);
            existingTorrentAlert.setTitle("Existing torrent file");
            existingTorrentAlert.setHeaderText(null);
            final Optional<ButtonType> addTrackersAnswer = existingTorrentAlert.showAndWait();
            if(!addTrackersAnswer.isPresent() || !(addTrackersAnswer.get() == ButtonType.OK)) {
                return;
            }
            queuedTorrentManager.addTrackers(torrentMatch.get(), torrentOptions.getProgress().getTrackerUrls());
        }
        else {
            final QueuedTorrentProgress torrentProgress = torrentOptions.getProgress();
            if(torrentOptions.shouldAddToTopQueue()) {
                torrentProgress.setTorrentPriority(QueuedTorrent.TOP_PRIORITY);
            }
            final List<TorrentView> torrentViews = queuedTorrentManager.addTorrents(
                    Arrays.asList(new TorrentTemplate(metaData, torrentOptions.getProgress())));

            torrentViews.forEach(tv -> loadTorrent(tv, torrentOptions.getTorrentContents()));
            //fileTreeViewer.show(infoHash);

        }
        updateGui();
    }

    private void loadStoredTorrents() {
        final List<TorrentView> loadedTorrents = queuedTorrentManager.loadPersisted();

        loadedTorrents.forEach(tv -> {
            final TreeItem<TorrentFileEntry> fileEntry = fileTreeViewer.createTreeView(fileContentTree, tv.getFileTree());
            loadTorrent(tv, fileEntry);
        });

        if(!loadedTorrents.isEmpty()) {
            torrentViewTable.selectJob(loadedTorrents.get(0));
        }
    }

    private void loadTorrent(final TorrentView torrentView, final TreeItem<TorrentFileEntry> contents) {
        torrentView.selectedLengthProperty().bind(contents.getValue().selectionLengthProperty());

        trackerTable.setContent(torrentView.getTrackerViews());
        torrentViewTable.addJob(torrentView);
        peerTable.setContent(torrentView.getPeerViews());
        fileTreeViewer.attach(torrentView.getInfoHash(), contents);
    }

    private void onStylesheetChanged(final ListChangeListener.Change<? extends String> change) {
        if(change.next()) {
            final Optional<? extends String> selectedTheme = change.getAddedSubList().stream().filter(
                    s -> s.startsWith("/themes/")).findFirst();
            if(selectedTheme.isPresent()) {
                final String stylesheetName = selectedTheme.get();
                refreshToolbarIcons(toolbarButtonsMap, stylesheetName.substring(
                        "/themes".length() + 1, stylesheetName.lastIndexOf(GuiProperties.THEME_UI_STYLE_CSS)));
            }
        }
    }

    private void onDeleteTorrents(final List<TorrentView> torrentsToDelete) {
        if(!torrentsToDelete.isEmpty()) {
            final boolean showConfirmAlert = ApplicationPreferences.getProperty(
                    GuiProperties.DELETE_TORRENT_CONFIRMATION, true);
            boolean torrentDeletionConfirmed = false;
            if(showConfirmAlert) {
                final Alert confirmDeleteAlert = new Alert(AlertType.WARNING,
                        "Are you sure you want to delete selected torrent(s)?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirmDeleteAlert.initOwner(stage);
                confirmDeleteAlert.setTitle("Delete torrent");
                confirmDeleteAlert.setHeaderText(null);
                final Optional<ButtonType> answer = confirmDeleteAlert.showAndWait();
                torrentDeletionConfirmed = answer.isPresent() && answer.get() == ButtonType.OK;
            }
            if(!showConfirmAlert || torrentDeletionConfirmed) {
                torrentsToDelete.stream().forEach(t -> {
                    try {
                        queuedTorrentManager.remove(t);
                    } catch (final IOException ioe) {
                        final Alert deleteErrorAlert = new Alert(AlertType.ERROR,
                                "Failed to delete torrent from the disk.", ButtonType.OK);
                        deleteErrorAlert.initOwner(stage);
                        deleteErrorAlert.setTitle("Delete torrent");
                        deleteErrorAlert.setHeaderText(null);
                        deleteErrorAlert.showAndWait();
                    }
                });
                torrentViewTable.deleteJobs(torrentsToDelete);
                final ObservableList<TorrentView> newSelection = torrentViewTable.getSelectedJobs();
                if(newSelection.isEmpty()) {
                    fileTreeViewer.hide();
                    infoPanel.setContent(null);
                    trackerTable.setContent(FXCollections.emptyObservableSet());
                    peerTable.setContent(FXCollections.emptyObservableList());
                }
                else {
                    final TorrentView selectedJob = newSelection.get(0);
                    fileTreeViewer.show(selectedJob.getInfoHash());
                    torrentViewTable.selectJob(selectedJob);
                    infoPanel.setContent(selectedJob);
                }
            }
        }
    }

	/* TODO: Extract action handling methods to a GuiActionHandler class or similar for each GUI component,
	 i.e. TrackerTableActionHandler, FileTreeActionHandler, TorrentJobActionHandler and so on */

    private void onTorrentJobSelection(final TorrentView selectedTorrentView) {
        final boolean torrentSelected = selectedTorrentView != null;
        if(torrentSelected) {
            fileTreeViewer.show(selectedTorrentView.getInfoHash());
        } else {
            fileTreeViewer.hide();
        }
        trackerTable.setContent(torrentSelected? selectedTorrentView.getTrackerViews() :
                FXCollections.emptyObservableSet());
        peerTable.setContent(torrentSelected? selectedTorrentView.getPeerViews() :
                FXCollections.emptyObservableList());
        infoPanel.setContent(torrentSelected? selectedTorrentView : null);
        updateGui();

        toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION).setDisable(!torrentSelected ||
                selectedTorrentView.getStatus() == TorrentStatus.ACTIVE ||
                selectedTorrentView.getQueueType() == QueueType.QUEUED);
        toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION).setDisable(!torrentSelected ||
                selectedTorrentView.getStatus() == TorrentStatus.STOPPED
                        && selectedTorrentView.getQueueType() != QueueType.QUEUED);

        toolbarButtonsMap.get(ImageUtils.UP_ICON_LOCATION).setDisable(!torrentSelected
                || selectedTorrentView.getPriority() == 1);
        toolbarButtonsMap.get(ImageUtils.DOWN_ICON_LOCATION).setDisable(!torrentSelected
                || selectedTorrentView.getPriority() == queuedTorrentManager.getTorrentsOnQueue());

        toolbarButtonsMap.get(ImageUtils.DELETE_ICON_LOCATION).setDisable(!torrentSelected);
    }

    private void onDeleteButtonStateChanged(final Button deleteButton, final boolean disabled) {
        final ImageView deleteButtonIcon = (ImageView)deleteButton.getGraphic();
        if(disabled) {
            deleteButtonIcon.setEffect(null);
        }
        else {
            ImageUtils.colorize(deleteButtonIcon, ImageUtils.BUTTON_COLOR_DELETE);
        }
    }

    private void storeWindowChanges() {
        windowActionHandler.storeWindowStateChanges(stage);
        storeDetailedInfoChanges();
        storeFilterViewChanges();
        storeStatusBarChanges();
        storeDividerLocations();
        storeToolBarChanges();
    }

    private void storeDividerLocations() {
        //Store horizontal divider position, if changed
        final double oldHorizontalDividerPosition = ApplicationPreferences.getProperty(
                GuiProperties.HORIZONTAL_DIVIDER_POSITION, GuiProperties.DEFAULT_HORIZONTAL_DIVIDER_POSITION);
        final double newHorizontalDividerPosition = showFilterViewMenuItem.isSelected()?
                horizontalSplitPane.getDividerPositions()[0] : filterTreeView.getPrefWidth();
        if(newHorizontalDividerPosition != oldHorizontalDividerPosition) {
            ApplicationPreferences.setProperty(GuiProperties.HORIZONTAL_DIVIDER_POSITION, newHorizontalDividerPosition);
        }

        //Store vertical divider position, if changed
        final double oldVerticalDividerPosition = ApplicationPreferences.getProperty(
                GuiProperties.VERTICAL_DIVIDER_POSITION, GuiProperties.DEFAULT_VERTICAL_DIVIDER_POSITION);
        final double newVerticalDividerPosition = showDetailedInfoMenuItem.isSelected()?
                verticalSplitPane.getDividerPositions()[0] : detailedInfoTabPane.getPrefHeight();
        if(newVerticalDividerPosition != oldVerticalDividerPosition) {
            ApplicationPreferences.setProperty(GuiProperties.VERTICAL_DIVIDER_POSITION, newVerticalDividerPosition);
        }
    }

    private void storeFilterViewChanges() {
        //Store filter view visibility, if changed
        final boolean wasFilterViewShown = ApplicationPreferences.getProperty(
                GuiProperties.FILTER_VIEW_VISIBLE, GuiProperties.DEFAULT_FILTER_VIEW_VISIBLE);
        final boolean isFilterViewShown = showFilterViewMenuItem.isSelected();
        if(isFilterViewShown != wasFilterViewShown) {
            ApplicationPreferences.setProperty(GuiProperties.FILTER_VIEW_VISIBLE, isFilterViewShown);
        }
    }

    private void storeStatusBarChanges() {
        //Store status bar visibility, if changed
        final boolean wasStatusBarShown = ApplicationPreferences.getProperty(
                GuiProperties.STATUSBAR_VISIBLE, GuiProperties.DEFAULT_STATUSBAR_VISIBLE);
        final boolean isStatusBarShown = showStatusBarMenuItem.isSelected();
        if(isStatusBarShown != wasStatusBarShown) {
            ApplicationPreferences.setProperty(GuiProperties.STATUSBAR_VISIBLE, isStatusBarShown);
        }
    }

    private void storeToolBarChanges() {
        //Store tool bar visibility, if changed
        final boolean wasToolBarShown = ApplicationPreferences.getProperty(
                GuiProperties.TOOLBAR_VISIBLE, GuiProperties.DEFAULT_TOOLBAR_VISIBLE);
        final boolean isToolBarShown = showToolbarMenuItem.isSelected();
        if(isToolBarShown != wasToolBarShown) {
            ApplicationPreferences.setProperty(GuiProperties.TOOLBAR_VISIBLE, isToolBarShown);
        }
        //Store compact tool bar property, if changed
        final boolean wasCompactToolBar = ApplicationPreferences.getProperty(
                GuiProperties.COMPACT_TOOLBAR, GuiProperties.DEFAULT_COMPACT_TOOLBAR);
        final boolean isCompactToolBar = showCompactToolbarMenuItem.isSelected();
        if(isCompactToolBar != wasCompactToolBar) {
            ApplicationPreferences.setProperty(GuiProperties.COMPACT_TOOLBAR, isCompactToolBar);
        }
    }

    private void storeDetailedInfoChanges() {
        //Store currently selected tab, if changed
        final Tab selectedTab = detailedInfoTabPane.getSelectionModel().getSelectedItem();
        if(!ApplicationPreferences.getProperty(GuiProperties.SELECTED_TAB_ID,
                GuiProperties.DEFAULT_SELECTED_TAB_ID).equals(selectedTab.getId())) {
            ApplicationPreferences.setProperty(GuiProperties.SELECTED_TAB_ID, selectedTab.getId());
        }

        //Store tab visibilities, if changed
        final String oldTabVisibility = ApplicationPreferences.getProperty(
                GuiProperties.TAB_VISIBILITY, GuiProperties.DEFAULT_TAB_VISIBILITY);
        final String newTabVisibility = detailedInfoTabPane.getTabs().stream().map(
                t -> t.getId()).collect(Collectors.joining(GuiProperties.COMPOSITE_PROPERTY_VALUE_SEPARATOR));

        if(!newTabVisibility.equals(oldTabVisibility)) {
            ApplicationPreferences.setProperty(GuiProperties.TAB_VISIBILITY, newTabVisibility);
        }

        //Store tab icons visibility, if changed
        final boolean wereTabIconsShown = ApplicationPreferences.getProperty(
                GuiProperties.TAB_ICONS_VISIBLE, GuiProperties.DEFAULT_TAB_ICONS_VISIBLE);
        final boolean areTabIconsShown = showTabIconsMenuItem.isSelected();
        if(areTabIconsShown != wereTabIconsShown) {
            ApplicationPreferences.setProperty(GuiProperties.TAB_ICONS_VISIBLE, areTabIconsShown);
        }

        //Store detailed info visibility, if changed
        final boolean wasDetailedInfoShown = ApplicationPreferences.getProperty(
                GuiProperties.DETAILED_INFO_VISIBLE, GuiProperties.DEFAULT_DETAILED_INFO_VISIBLE);
        final boolean isDetailedInfoShown = showDetailedInfoMenuItem.isSelected();
        if(isDetailedInfoShown != wasDetailedInfoShown) {
            ApplicationPreferences.setProperty(GuiProperties.DETAILED_INFO_VISIBLE, isDetailedInfoShown);
        }

        trackerTable.storeColumnStates();
        peerTable.storeColumnStates();
        torrentViewTable.storeColumnStates();
        storeColumnStates();
    }

    private void storeColumnStates() {
        TableUtils.storeColumnStates(fileContentTree.getColumns(), GuiProperties.INFO_TAB_COLUMN_VISIBILITY,
                GuiProperties.DEFAULT_INFO_TAB_COLUMN_VISIBILITIES, GuiProperties.INFO_TAB_COLUMN_SIZE,
                GuiProperties.DEFAULT_INFO_TAB_COLUMN_SIZES, GuiProperties.INFO_TAB_COLUMN_ORDER,
                GuiProperties.DEFAULT_INFO_TAB_COLUMN_ORDER);
    }

    private void onShutdown(final Event event) {
        final boolean isShuttingDown = windowActionHandler.onWindowClose(event, stage);

        if(isShuttingDown) {
            //Stop listening for preference changes
            ApplicationPreferences.removePreferenceChangeListener(this);

            //Stop updating periodic tasks
            guiUpdateTaskRunner.cancel();

            //Store any changes to window components
            storeWindowChanges();

            //User chose to close the application, quit
            Platform.exit();
        }
    }
}