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

package org.matic.torrent.gui.window;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.matic.torrent.gui.action.FileActionHandler;
import org.matic.torrent.gui.action.TabActionHandler;
import org.matic.torrent.gui.action.TorrentJobActionHandler;
import org.matic.torrent.gui.action.TrackerTableActionHandler;
import org.matic.torrent.gui.action.WindowActionHandler;
import org.matic.torrent.gui.action.enums.ApplicationTheme;
import org.matic.torrent.gui.custom.StatusBar;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentFileEntry;
import org.matic.torrent.gui.model.TorrentJobView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.gui.table.TableUtils;
import org.matic.torrent.gui.table.TorrentJobTable;
import org.matic.torrent.gui.table.TrackerTable;
import org.matic.torrent.gui.tree.FileTreeViewer;
import org.matic.torrent.gui.tree.TreeTableUtils;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.io.StateKeeper;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.queue.QueuedTorrentMetaData;
import org.matic.torrent.tracking.TrackerManager;
import org.matic.torrent.tracking.beans.TrackerSessionViewBean;
import org.matic.torrent.utils.PeriodicTask;
import org.matic.torrent.utils.PeriodicTaskRunner;

/**
 * A main application window, showing all of the GUI components.
 * 
 * @author vedran
 *
 */
public final class ApplicationWindow {
	
	private static final List<String> TAB_NAMES = Arrays.asList(GuiProperties.FILES_TAB_ID,
			GuiProperties.INFO_TAB_ID, GuiProperties.PEERS_TAB_ID, GuiProperties.TRACKERS_TAB_ID,
			GuiProperties.PIECES_TAB_ID, GuiProperties.SPEED_TAB_ID, GuiProperties.LOGGER_TAB_ID);            
		
	private static final Color TOOLBAR_BUTTON_COLOR = Color.rgb(46, 46, 46);
	private static final Color REMOVE_BUTTON_COLOR = Color.rgb(165,57,57);
	
	private static final Color TAB_SELECTED_IMAGE_COLOR = Color.rgb(102, 162, 54);
	private static final Color TAB_DEFAULT_IMAGE_COLOR = Color.rgb(162, 170, 156);
	
	private static final int TAB_ICON_SIZE = 14;
	
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
	private final TreeView<Node> filterTreeView = new TreeView<>();
	
	//View for displaying all torrent jobs in a table
	private final TorrentJobTable torrentJobTable = new TorrentJobTable();	
	
	//Container for active torrent's tree file view
	private final TreeTableView<TorrentFileEntry> fileContentTree = new TreeTableView<>();
	
	//Container for all of the torrents' file tree views
	private final FileTreeViewer fileTreeViewer = new FileTreeViewer(fileContentTree);
	
	//View for displaying selected torrent's trackers
	private final TrackerTable trackerTable = new TrackerTable();
	
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
	
	//Mapping between a torrent and it's tracker views
	private final Map<QueuedTorrent, List<TrackerView>> trackerViewMappings = new HashMap<>();
	
	//Mapping between toolbar's buttons and their icon paths
	private final Map<String, Button> toolbarButtonsMap = new HashMap<>();
	
	//Mapping between details tabs and their names
	private final Map<String, Tab> detailsTabMap = new HashMap<>();
	
	//A service that periodically executes actions (such as GUI update)
	private final PeriodicTaskRunner periodicTaskRunner = new PeriodicTaskRunner();
	
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
		
		initComponents();
	}
	
	private void initComponents() {		
		setMenuAccelerators();				
		initDetailedInfoTabPane();
		
		final BorderPane mainPane = new BorderPane();		
		mainPane.setTop(buildMenuBarPane());
		mainPane.setCenter(buildContentPane());
		
		initStatusBar(mainPane);
		
		torrentJobTable.addSelectionListener(this::onTorrentJobSelection);
		trackerTable.onTrackerDeletionRequested(views ->
			trackerTableActionHandler.onTrackerDeletion(views, trackerManager, trackerTable));
		trackerTable.onTrackerUpdateRequested(views ->
			trackerTableActionHandler.onTrackerUpdate(views, trackerManager));
		trackerTable.onTrackersAdded(urls -> trackerTableActionHandler.onTrackersAdded(urls, trackerManager,
			torrentJobTable.getSelectedJobs().get(0).getQueuedTorrent(), trackerTable), stage,
			torrentJobTable.bindOnEmpty());
		
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
		statusBar.getStyleClass().add("status-bar");
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
		
		periodicTaskRunner.addTask(periodicTask);		
		periodicTaskRunner.setPeriod(Duration.seconds(1));		
		periodicTaskRunner.start();
	}
	
	private void updateGui() {					
		if(stage.isIconified()) {
			return;
		}
		
		final List<TorrentJobView> selectedTorrents = torrentJobTable.getSelectedJobs();
		
		if(!selectedTorrents.isEmpty()) {
			//Render Detailed Info pane contents only if it is visible
			if(showDetailedInfoMenuItem.isSelected()) {
				//Update and render tracker statistics if Trackers tab is selected
				if(detailsTabMap.get(GuiProperties.TRACKERS_TAB_ID).isSelected()) {					
					trackerTable.updateContent();
					trackerTable.sort();
				}
				//Update and render file progress statistics if Info tab is selected
				else if(detailsTabMap.get(GuiProperties.FILES_TAB_ID).isSelected()) {
					//TODO: Implement
					TreeTableUtils.sort(fileContentTree);
				}
			}			
		}
	}
	
	private void initFilterTreeView() {
		filterTreeView.getStyleClass().add("filter-list-view");
		filterTreeView.setRoot(buildFilterTreeViewItems());
		filterTreeView.setShowRoot(false);
		filterTreeView.getSelectionModel().select(0);
		filterTreeView.requestFocus();		
	}
	
	private TreeItem<Node> buildFilterTreeViewItems() {		
		final List<TreeItem<Node>> torrentNodeElements = Arrays.asList("Downloading (0)",
				"Seeding (0)", "Completed (0)", "Active (0)", "Inactive (0)").stream().map(labelName -> {
			final Label label = new Label(labelName);
			label.getStyleClass().add("filter-list-child-cell");
			return new TreeItem<Node>(label);
		}).collect(Collectors.toList());
		
		final List<TreeItem<Node>> labelsNodeElements = Arrays.asList("No Label (0)").stream().map(labelName -> {
			final Label label = new Label(labelName);
			label.getStyleClass().add("filter-list-child-cell");
			return new TreeItem<Node>(label);
		}).collect(Collectors.toList());
		
		final Label torrentsRootLabel = new Label("Torrents (0)");
		torrentsRootLabel.getStyleClass().add("filter-list-root-cell");
		torrentsRootLabel.setGraphic(new ImageView(ImageUtils.DOWNLOADS_IMAGE));
		final TreeItem<Node> torrentsRootNode = new TreeItem<>(torrentsRootLabel);
		torrentsRootNode.setExpanded(true);
		torrentsRootNode.getChildren().addAll(torrentNodeElements);
		
		final Label labelsRootLabel = new Label("Labels");
		labelsRootLabel.getStyleClass().add("filter-list-root-cell");
		labelsRootLabel.setGraphic(new ImageView(ImageUtils.LABEL_IMAGE));
		final TreeItem<Node> labelsRootNode = new TreeItem<>(labelsRootLabel);
		labelsRootNode.setExpanded(true);
		labelsRootNode.getChildren().addAll(labelsNodeElements);
		
		final Label rssFeedsRootLabel = new Label("Feeds (0)");
		rssFeedsRootLabel.getStyleClass().add("filter-list-root-cell-no-children");
		rssFeedsRootLabel.setGraphic(new ImageView(ImageUtils.RSS_IMAGE));
		final TreeItem<Node> rssFeedsRootNode = new TreeItem<>(rssFeedsRootLabel);
		rssFeedsRootNode.setExpanded(true);
		
		final List<TreeItem<Node>> allNodes = Arrays.asList(torrentsRootNode, labelsRootNode, rssFeedsRootNode);
		
		final TreeItem<Node> rootNode = new TreeItem<>();		
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
		final String borderlessSplitPaneStyle = "borderless-split-pane";
		final BorderPane mainPane = new BorderPane();		
		final ToolBar toolbar = buildToolbar();				
		
		verticalSplitPane.getStyleClass().add(borderlessSplitPaneStyle);
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
        initFilterTreeView();
         
        horizontalSplitPane.getStyleClass().add(borderlessSplitPaneStyle);
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
	
	private ToolBar buildToolbar() {				
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
		
		toolbarButtonsMap.get(ImageUtils.ADD_ICON_LOCATION).setOnAction(
				event -> onAddTorrent(fileActionHandler.onFileOpen(stage, fileTreeViewer)));
		toolbarButtonsMap.get(ImageUtils.LINK_ICON_LOCATION).setOnAction(
				event -> onAddTorrent(fileActionHandler.onLoadUrl(stage, fileTreeViewer)));
		toolbarButtonsMap.get(ImageUtils.SETTINGS_ICON_LOCATION).setOnAction(
				event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));
		toolbarButtonsMap.get(ImageUtils.DELETE_ICON_LOCATION).setOnAction(
				event -> onRemoveTorrent());
		toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION).setOnAction(
				event -> torrentJobActionHandler.onChangeTorrentState(QueuedTorrent.State.ACTIVE,
						toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION),
						toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION), torrentJobTable));
		toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION).setOnAction(
				event -> torrentJobActionHandler.onChangeTorrentState(QueuedTorrent.State.STOPPED,
						toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION),
						toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION), torrentJobTable));

		final String themeName = ApplicationPreferences.getProperty(GuiProperties.APPLICATION_THEME, "light");
		updateToolbarIcons(toolbarButtonsMap, GuiProperties.THEME_STYLESHEET_PATH_TEMPLATE.replace("?", themeName));
		
		final HBox separatorBox = new HBox();		
		HBox.setHgrow(separatorBox, Priority.ALWAYS);
		
		final Node[] toolbarContents = {toolbarButtons[0], toolbarButtons[1], toolbarButtons[2],
				buildToolbarSeparator(), toolbarButtons[3], buildToolbarSeparator(), 
				toolbarButtons[4], buildToolbarSeparator(), toolbarButtons[5], toolbarButtons[6],
				toolbarButtons[7], buildToolbarSeparator(), toolbarButtons[8], toolbarButtons[9],
				buildToolbarSeparator(), toolbarButtons[10], buildToolbarSeparator(), separatorBox,
				buildToolbarSeparator(), toolbarButtons[11], toolbarButtons[12]};
		
		final ToolBar toolBar = new ToolBar(toolbarContents);		
		return toolBar;
	}
	
	private void updateToolbarIcons(final Map<String, Button> buttonMap, final String iconPath) {
		buttonMap.entrySet().forEach(buttonLocation -> {			
			final ImageView imageView = ImageUtils.createImageView(new Image(
					getClass().getResourceAsStream(iconPath + buttonLocation.getKey())),
					ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, ImageUtils.TOOLBAR_BUTTON_IMAGE_SIZE,
					ImageUtils.TOOLBAR_BUTTON_IMAGE_SIZE);			
			buttonLocation.getValue().setGraphic(imageView);
		});
	}
	
	private Button buildToolbarButton(final String iconPath, final String tooltip, final boolean disabled) {			
		final Button button = new Button();
		button.getStyleClass().add("toolbar-button");		
		button.setTooltip(new Tooltip(tooltip));		
		button.setDisable(disabled);		

		toolbarButtonsMap.put(iconPath, button);				
		return button;
	}
	
	private Separator buildToolbarSeparator() {
		final Separator separator = new Separator(Orientation.VERTICAL);
		separator.getStyleClass().add("toolbar-separator");
		separator.setDisable(true);
		return separator;
	}
	
	private ScrollPane buildTorrentJobTable() {				
		final ScrollPane torrentJobTableScroll = new ScrollPane();				
		torrentJobTableScroll.setFitToWidth(true);
		torrentJobTableScroll.setFitToHeight(true);
		torrentJobTable.wrapWith(torrentJobTableScroll);
		
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
        		final Color tabImageColor = tab.isSelected()? TAB_SELECTED_IMAGE_COLOR : TAB_DEFAULT_IMAGE_COLOR;
        		
        		final ImageView imageView = ImageUtils.createImageView(tabImage,
        				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE);        		
        		ImageUtils.colorize(imageView, tabImageColor);
        		
        		tabImageViews.put(tab, imageView);    
        		if(showTabIconsMenuItem.isSelected()) {
        			tab.setGraphic(imageView);
        		}
        		updateGui();
        	});       	
        	final ImageView imageView = ImageUtils.createImageView(tabImage,
        			ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE);        	        	
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
        
        detailsTabMap.get(GuiProperties.FILES_TAB_ID).setContent(torrentContentTreeScroll);
        detailsTabMap.get(GuiProperties.TRACKERS_TAB_ID).setContent(trackerTableScroll);

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
		
		optionsMenu.getItems().addAll(preferencesMenuItem, new SeparatorMenuItem(), showToolbarMenuItem,
				showDetailedInfoMenuItem, showStatusBarMenuItem, showFilterViewMenuItem, showCompactToolbarMenuItem,
				new SeparatorMenuItem(), showTabIconsMenuItem);
		
		return optionsMenu;
	}
	
	private Menu buildHelpMenu() {
		final Menu helpMenu = new Menu("_Help");
		helpMenu.setMnemonicParsing(true);
		
		return helpMenu;
	}
	
	private void onAddTorrent(final AddedTorrentOptions torrentOptions) {
		if(torrentOptions == null) {
			return;
		}
		
		final QueuedTorrentMetaData metaData = torrentOptions.getMetaData();
		final InfoHash infoHash = metaData.getInfoHash();
		if(queuedTorrentManager.find(infoHash).isPresent()) {
			final Alert existingTorrentAlert = new Alert(AlertType.ERROR,
					"The torrent already exists.\n" +
							"Would you like to load trackers from it?",
							ButtonType.OK, ButtonType.CANCEL);
			existingTorrentAlert.setTitle("Existing torrent file");
			existingTorrentAlert.setHeaderText(null);
			existingTorrentAlert.showAndWait();			
			return;
		}

		final QueuedTorrent queuedTorrent = new QueuedTorrent(metaData, torrentOptions.getProgress());		
		StateKeeper.store(queuedTorrent);
		
		loadTorrent(queuedTorrent, torrentOptions.getTorrentContents());
		fileTreeViewer.show(infoHash);
		
		updateGui();
	}
	
	private void loadStoredTorrents() {		
		final Set<QueuedTorrent> loadedTorrents = StateKeeper.loadAll();
				
		loadedTorrents.forEach(t -> {
			final TreeItem<TorrentFileEntry> fileEntry = fileTreeViewer.createView(
					fileContentTree, t.getMetaData(), t.getProgress());
			loadTorrent(t, fileEntry);
			fileTreeViewer.show(t.getMetaData().getInfoHash());
		});
	}
	
	private void loadTorrent(final QueuedTorrent queuedTorrent, final TreeItem<TorrentFileEntry> contents) {
		final TorrentJobView jobView = new TorrentJobView(queuedTorrent,
				queuedTorrent.getProgress().getName());		
		final Set<TrackerSessionViewBean> trackerSessionViewBeans = queuedTorrentManager.add(queuedTorrent);
				
		final ObservableList<TrackerView> trackerViews = FXCollections.observableArrayList(
				trackerSessionViewBeans.stream().map(TrackerView::new).collect(Collectors.toList()));
					
		trackerViewMappings.put(queuedTorrent, trackerViews);
		trackerTable.setContent(trackerViews);		
		torrentJobTable.addJob(jobView);			
		fileTreeViewer.attach(queuedTorrent.getMetaData().getInfoHash(), contents);
	}
	
	private void onStylesheetChanged(final ListChangeListener.Change<? extends String> change) {		
		if(change.next()) {
			final Optional<? extends String> selectedTheme = change.getAddedSubList().stream().filter(
					s -> s.startsWith("/themes/")).findFirst();
			if(selectedTheme.isPresent()) {				
				final String stylesheetName = selectedTheme.get();
				updateToolbarIcons(toolbarButtonsMap, stylesheetName.substring(
						0, stylesheetName.lastIndexOf(GuiProperties.THEME_UI_STYLE_CSS)));
			}
		}
	}
	
	private void onRemoveTorrent() {
		final ObservableList<TorrentJobView> selectedTorrentJobs = torrentJobTable.getSelectedJobs();
		
		if(selectedTorrentJobs.size() > 0) {
			final Alert confirmDeleteAlert = new Alert(AlertType.WARNING,
					"Are you sure you want to delete selected torrent(s)?",
							ButtonType.OK, ButtonType.CANCEL);
			confirmDeleteAlert.setTitle("Delete torrent");
			confirmDeleteAlert.setHeaderText(null);
			final Optional<ButtonType> answer = confirmDeleteAlert.showAndWait();
			if(answer.isPresent() && answer.get() == ButtonType.OK) {			
				selectedTorrentJobs.stream().map(
						TorrentJobView::getQueuedTorrent).forEach(t -> {
							queuedTorrentManager.remove(t);
							try {
								StateKeeper.delete(t);
							} catch (final IOException ioe) {
								final Alert deleteErrorAlert = new Alert(AlertType.ERROR,
										"Failed to delete torrent from the disk.", ButtonType.OK);
								deleteErrorAlert.setTitle("Delete torrent");
								deleteErrorAlert.setHeaderText(null);
								deleteErrorAlert.showAndWait();
							}
						});
				torrentJobTable.deleteJobs(selectedTorrentJobs);
				final ObservableList<TorrentJobView> newSelection = torrentJobTable.getSelectedJobs();
				if(newSelection.isEmpty()) {
					fileTreeViewer.clear();
					trackerTable.setContent(FXCollections.emptyObservableList());
				}
				else {
					final TorrentJobView selectedJob = newSelection.get(0);
					fileTreeViewer.show(selectedJob.getQueuedTorrent().getMetaData().getInfoHash());
					torrentJobTable.selectJob(selectedJob);
				}
			}
		}
	}
	
	/* TODO: Extract action handling methods to a GuiActionHandler class or similar for each GUI component,
	 i.e. TrackerTableActionHandler, FileTreeActionHandler, TorrentJobActionHandler and so on */
	
	private void onTorrentJobSelection(final TorrentJobView selectedTorrentJob) {
		final boolean torrentSelected = selectedTorrentJob != null;
		if(torrentSelected) {		
			fileTreeViewer.show(selectedTorrentJob.getQueuedTorrent().getMetaData().getInfoHash());
			trackerTable.setContent(trackerViewMappings.get(selectedTorrentJob.getQueuedTorrent()));			
			updateGui();
		}
		toolbarButtonsMap.get(ImageUtils.DOWNLOAD_ICON_LOCATION).setDisable(!torrentSelected ||
				selectedTorrentJob.getQueuedTorrent().getProgress().getState() == QueuedTorrent.State.ACTIVE);
		toolbarButtonsMap.get(ImageUtils.STOP_ICON_LOCATION).setDisable(!torrentSelected ||
				selectedTorrentJob.getQueuedTorrent().getProgress().getState() == QueuedTorrent.State.STOPPED);
		
		final Button removeButton = toolbarButtonsMap.get(ImageUtils.DELETE_ICON_LOCATION);	
		final ImageView buttonImageView = (ImageView)removeButton.getGraphic();
		
		removeButton.setDisable(!torrentSelected);
		final Color buttonImageColor = removeButton.isDisabled()? 
				TOOLBAR_BUTTON_COLOR : REMOVE_BUTTON_COLOR; 
		ImageUtils.colorize(buttonImageView, buttonImageColor);		
		removeButton.setGraphic(buttonImageView);		
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
		torrentJobTable.storeColumnStates();
		storeColumnStates();
	}
	
	private void storeColumnStates() {
		TableUtils.storeColumnStates(fileContentTree.getColumns(), GuiProperties.INFO_TAB_COLUMN_VISIBILITY,
				GuiProperties.DEFAULT_INFO_TAB_COLUMN_VISIBILITIES, GuiProperties.INFO_TAB_COLUMN_SIZE,
				GuiProperties.DEFAULT_INFO_TAB_COLUMN_SIZES, GuiProperties.INFO_TAB_COLUMN_ORDER,
				GuiProperties.DEFAULT_INFO_TAB_COLUMN_ORDER);
	}
	
	private void storeTorrentProgress() {
		//TODO: Implement method
	}
	
	private void onShutdown(final Event event) {
		final boolean isShuttingDown = windowActionHandler.onWindowClose(event, stage);
		
		if(isShuttingDown) {
			//Stop updating periodic tasks
			periodicTaskRunner.cancel();
			
			//Save progress state for all torrents
			storeTorrentProgress();
			
			//Store any changes to window components
			storeWindowChanges();
			
			//User chose to close the application, quit
	        Platform.exit();
		}
	}
}