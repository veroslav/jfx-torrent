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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.controlsfx.control.StatusBar;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeyNames;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.action.FileActionHandler;
import org.matic.torrent.gui.action.WindowActionHandler;
import org.matic.torrent.gui.image.ImageUtils;
import org.matic.torrent.gui.model.TorrentJobView;
import org.matic.torrent.gui.model.TrackerView;
import org.matic.torrent.gui.table.TorrentJobTable;
import org.matic.torrent.gui.table.TrackerTable;
import org.matic.torrent.gui.tree.TorrentContentTree;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrent;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.utils.PeriodicTask;
import org.matic.torrent.utils.PeriodicTaskRunner;
import org.matic.torrent.utils.ResourceManager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A main application window, showing all of the GUI components.
 * 
 * @author vedran
 *
 */
public final class ApplicationWindow {
	
	//TODO: Properly updateGUI() when hiding/showing window components (such as detailed info pane)
	
	private static final String TOOLBAR_BUTTON_ADD_FROM_URL_NAME = "Add Torrent from URL";
	private static final String TOOLBAR_BUTTON_ADD_RSS_FEED_NAME = "Add RSS Feed";
	private static final String TOOLBAR_BUTTON_OPTIONS_NAME = "Preferences";
	private static final String TOOLBAR_BUTTON_START_NAME = "Start Torrent";
	private static final String TOOLBAR_BUTTON_PAUSE_NAME = "Pause Torrent";
	private static final String TOOLBAR_BUTTON_STOP_NAME = "Stop Torrent";
	private static final String TOOLBAR_BUTTON_ADD_NAME = "Add Torrent";
	private static final String TOOLBAR_BUTTON_REMOVE_NAME = "Remove";	
		
	private static final Color TOOLBAR_BUTTON_COLOR = Color.rgb(46, 46, 46);
	private static final Color REMOVE_BUTTON_COLOR = Color.rgb(165,57,57);
	
	private static final Color TAB_SELECTED_IMAGE_COLOR = Color.rgb(102, 162, 54);
	private static final Color TAB_DEFAULT_IMAGE_COLOR = Color.rgb(162, 170, 156);
	
	private static final int TAB_ICON_SIZE = 14;
	
	//The manager for handling queued torrents states
	private final QueuedTorrentManager queuedTorrentManager = new QueuedTorrentManager();
	
	private final WindowActionHandler windowActionHandler = new WindowActionHandler();
	private final FileActionHandler fileActionHandler = new FileActionHandler();
	
	//View for filtering torrents according to their status
	private final TreeView<Node> filterTreeView = new TreeView<>();
	
	//View for displaying all torrent jobs in a table
	private final TorrentJobTable torrentJobTable = new TorrentJobTable();	
	
	//View for displaying currently selected torrent's contents
	private final TorrentContentTree torrentContentTree = new TorrentContentTree(true);
	
	//View for displaying selected torrent's trackers
	private final TrackerTable trackerTable = new TrackerTable();
	
	//Detailed torrent info tab pane 
	private final TabPane detailedInfoTabPane = new TabPane();
	
	//Application status below at the bottom of the window
	private final StatusBar statusBar = new StatusBar();
	
	//Menu item for either showing or hiding the detailed info tab pane
	private final CheckMenuItem showDetailedInfoMenuItem = new CheckMenuItem("Show Detailed Info");
	
	//Menu item for selecting either compact or expanded tool bar
	private final CheckMenuItem showCompactToolbarMenuItem = new CheckMenuItem("Narrow Toolbar");
	
	//Menu item for showing or hiding the status bar
	private final CheckMenuItem showStatusBarMenuItem = new CheckMenuItem("Show Status Bar");
	
	//Menu item for either showing or hiding the filter view
	private final CheckMenuItem showFilterViewMenuItem = new CheckMenuItem("Show Sidebar");
	
	//Menu item for showing or hiding the tool bar
	private final CheckMenuItem showToolbarMenuItem = new CheckMenuItem("Show Toolbar");
	
	//Menu item for showing or hiding the tab icons
	private final CheckMenuItem showTabIconsMenuItem = new CheckMenuItem("Icons on Tabs");
	
	//Mapping between a torrent and it's tracker views
	private final Map<QueuedTorrent, List<TrackerView>> trackerViewMappings = new HashMap<>();
	
	//Mapping between toolbar's buttons and their names
	private final Map<String, Button> toolbarButtonsMap = new HashMap<>();
	
	//Mapping between details tabs and their names
	private final Map<String, Tab> detailsTabMap = new HashMap<>();
	
	//A service that periodically executes actions (such as GUI update)
	private final PeriodicTaskRunner periodicTaskRunner = new PeriodicTaskRunner();
		
	private final Stage stage;

	public ApplicationWindow(final Stage stage) {
		this.stage = stage;
		
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
		torrentContentTree.getView().setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		
		stage.setScene(initScene(mainPane));
		stage.setOnCloseRequest(this::onShutdown);		
		stage.setTitle("jfxTorrent");        		
		stage.show();
		
		Platform.runLater(() -> windowActionHandler.handleWindowStateChanges(stage));
		
		initPeriodicTaskRunner();
	}
	
	private void initStatusBar(final BorderPane contentPane) {
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
		scene.getStylesheets().add("/ui-style.css");
		
		final double windowXPosition = ApplicationPreferences.getProperty(
				GuiProperties.APPLICATION_WINDOW_POSITION_X, GuiProperties.DEFAULT_WINDOW_POSITION);
		final double windowYPosition = ApplicationPreferences.getProperty(
				GuiProperties.APPLICATION_WINDOW_POSITION_Y, GuiProperties.DEFAULT_WINDOW_POSITION);
		
		if(windowXPosition != GuiProperties.DEFAULT_WINDOW_POSITION &&
				windowYPosition != GuiProperties.DEFAULT_WINDOW_POSITION) {
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
		detailedInfoTabPane.getTabs().addAll(tabs);
		
		addTabMenus(detailedInfoTabPane.getTabs());
		addDetailsTabHeaderContextMenu(detailedInfoTabPane.getTabs());
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
		
		PeriodicTask periodicTask = new PeriodicTask(() -> Platform.runLater(this::updateGui), guiUpdateInterval);
		
		periodicTaskRunner.addTask(periodicTask);		
		periodicTaskRunner.setPeriod(Duration.seconds(1));		
		periodicTaskRunner.start();
	}
	
	private void updateGui() {		
		final List<TorrentJobView> selectedTorrents = torrentJobTable.getSelectedJobs();
		
		if(!selectedTorrents.isEmpty()) {
			//Render Detailed Info pane contents only if it is visible
			if(showDetailedInfoMenuItem.isSelected()) {
				//Render tracker statistics if Trackers tab is selected
				if(detailsTabMap.get(GuiProperties.TRACKERS_TAB_ID).isSelected()) {
					
					System.out.println("Updating Trackers Tab");
					
					ResourceManager.INSTANCE.getTrackerManager().trackerSnapshot(
						selectedTorrents.get(0).getQueuedTorrent(),
						trackerTable.getTrackerViews());
					trackerTable.sort();
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
				
		final SplitPane verticalSplitPane = new SplitPane();
		verticalSplitPane.getStyleClass().add(borderlessSplitPaneStyle);
        verticalSplitPane.setOrientation(Orientation.VERTICAL);     
        verticalSplitPane.getItems().addAll(buildTorrentJobTable(), detailedInfoTabPane);        
        
        final BorderPane centerPane = new BorderPane();
        centerPane.setCenter(verticalSplitPane);
        
        final boolean toolbarShown = ApplicationPreferences.getProperty(
    			GuiProperties.TOOLBAR_VISIBLE, GuiProperties.DEFAULT_TOOLBAR_VISIBLE);        
        showToolbarMenuItem.setSelected(toolbarShown);
        showToolbar(mainPane, centerPane, toolbar, showToolbarMenuItem.isSelected());
        showToolbarMenuItem.selectedProperty().addListener((obs, oldV, showToolbar) -> {
        	showToolbar(mainPane, centerPane, toolbar, showToolbar);
        	ApplicationPreferences.setProperty(GuiProperties.TOOLBAR_VISIBLE, showToolbar);
        });
        
        final boolean compactToolbarShown = ApplicationPreferences.getProperty(
    			GuiProperties.COMPACT_TOOLBAR, GuiProperties.DEFAULT_COMPACT_TOOLBAR);        
        showCompactToolbarMenuItem.setSelected(compactToolbarShown);
        showCompactToolbar(mainPane, centerPane, toolbar, showCompactToolbarMenuItem.isSelected());
        showCompactToolbarMenuItem.selectedProperty().addListener((obs, oldV, showCompact) -> {
        	showCompactToolbar(mainPane, centerPane, toolbar, showCompact);
        	ApplicationPreferences.setProperty(GuiProperties.COMPACT_TOOLBAR, showCompact);
        });
        initFilterTreeView();
        
        final SplitPane horizontalSplitPane = new SplitPane(); 
        horizontalSplitPane.getStyleClass().add(borderlessSplitPaneStyle);
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizontalSplitPane.getItems().addAll(filterTreeView, centerPane); 
        
        final double horizontalDividerPosition = ApplicationPreferences.getProperty(
        		GuiProperties.HORIZONTAL_DIVIDER_POSITION, GuiProperties.DEFAULT_HORIZONTAL_DIVIDER_POSITION);        
    	horizontalSplitPane.setDividerPosition(0, horizontalDividerPosition);
        
    	final double verticalDividerPosition = ApplicationPreferences.getProperty(
        		GuiProperties.VERTICAL_DIVIDER_POSITION, GuiProperties.DEFAULT_VERTICAL_DIVIDER_POSITION);    	
    	verticalSplitPane.setDividerPosition(0, verticalDividerPosition);

        final ChangeListener<?super Number> verticalDividerPositionListener = (obs, oldV, position) ->
    		ApplicationPreferences.setProperty(GuiProperties.VERTICAL_DIVIDER_POSITION, position.doubleValue());
    	
    	final boolean detailedInfoShown = ApplicationPreferences.getProperty(
    			GuiProperties.DETAILED_INFO_VISIBLE, GuiProperties.DEFAULT_DETAILED_INFO_VISIBLE);        
        showDetailedInfoMenuItem.setSelected(detailedInfoShown);
        showDetailedInfo(verticalSplitPane, showDetailedInfoMenuItem.isSelected(), verticalDividerPositionListener);
        showDetailedInfoMenuItem.selectedProperty().addListener((obs, oldV, showDetailedInfo) -> {
        	showDetailedInfo(verticalSplitPane, showDetailedInfo, verticalDividerPositionListener);
        	ApplicationPreferences.setProperty(GuiProperties.DETAILED_INFO_VISIBLE, showDetailedInfo);
        });
        
        final ChangeListener<?super Number> horizontalDividerPositionListener = (obs, oldV, position) ->
    		ApplicationPreferences.setProperty(GuiProperties.HORIZONTAL_DIVIDER_POSITION, position.doubleValue());
        
        final boolean filterViewShown = ApplicationPreferences.getProperty(
    			GuiProperties.FILTER_VIEW_VISIBLE, GuiProperties.DEFAULT_FILTER_VIEW_VISIBLE);        
        showFilterViewMenuItem.setSelected(filterViewShown);
        showFilterView(horizontalSplitPane, showFilterViewMenuItem.isSelected(), horizontalDividerPositionListener);
        showFilterViewMenuItem.selectedProperty().addListener((obs, oldV, showFilterView) -> {
        	showFilterView(horizontalSplitPane, showFilterView, horizontalDividerPositionListener);
        	ApplicationPreferences.setProperty(GuiProperties.FILTER_VIEW_VISIBLE, showFilterView);
        });
        
        SplitPane.setResizableWithParent(filterTreeView, Boolean.FALSE);        
        mainPane.setCenter(horizontalSplitPane);
        
        return mainPane;
	}
	
	private void showStatusBar(final BorderPane mainPane, final boolean showStatusBar) {		
		mainPane.setBottom(showStatusBar? statusBar : null);		
	}
	
	private void showDetailedInfo(final SplitPane verticalSplitPane, final boolean showDetailedInfo,
			final ChangeListener<?super Number> dividerPositionListener) {
		final ObservableList<Node> items = verticalSplitPane.getItems();
		if(showDetailedInfo && !items.contains(detailedInfoTabPane)) {
			items.add(1, detailedInfoTabPane);
			verticalSplitPane.getDividers().get(0).positionProperty().addListener(dividerPositionListener);
			verticalSplitPane.setDividerPosition(0, detailedInfoTabPane.getPrefHeight());
		}
		else if(!showDetailedInfo) {
			detailedInfoTabPane.setPrefHeight(verticalSplitPane.getDividerPositions()[0]);
			verticalSplitPane.getDividers().get(0).positionProperty().removeListener(dividerPositionListener);
			items.remove(detailedInfoTabPane);
		}
		else {
    		verticalSplitPane.getDividers().get(0).positionProperty().addListener(dividerPositionListener);
    	}		
	}
	
	private void showFilterView(final SplitPane horizontalSplitPane, final boolean showFilterView,
			final ChangeListener<?super Number> dividerPositionListener) {				
    	final ObservableList<Node> items = horizontalSplitPane.getItems();
    	if(showFilterView && !items.contains(filterTreeView)) {
    		items.add(0, filterTreeView);
    		horizontalSplitPane.getDividers().get(0).positionProperty().addListener(dividerPositionListener);
    		horizontalSplitPane.setDividerPosition(0, filterTreeView.getPrefWidth());    		
    	}
    	else if(!showFilterView) {
    		filterTreeView.setPrefWidth(horizontalSplitPane.getDividerPositions()[0]);
    		horizontalSplitPane.getDividers().get(0).positionProperty().removeListener(dividerPositionListener);
    		items.remove(filterTreeView);        		
    	} 
    	else {
    		horizontalSplitPane.getDividers().get(0).positionProperty().addListener(dividerPositionListener);
    	}
	}
	
	private void showCompactToolbar(final BorderPane mainPane, final BorderPane centerPane,
		final ToolBar toolbar, final boolean showCompact) {					
		final boolean isToolbarHidden = !showToolbarMenuItem.isSelected();
		mainPane.setTop(showCompact || isToolbarHidden? null : toolbar);
		centerPane.setTop(!showCompact || isToolbarHidden? null : toolbar); 
		toolbar.setId(showCompact? centerPane.getId() : mainPane.getId());
		
		final Button pauseButton = toolbarButtonsMap.get(TOOLBAR_BUTTON_PAUSE_NAME);
		final Button rssButton = toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_RSS_FEED_NAME);
		final ObservableList<Node> toolbarButtons = toolbar.getItems();
		    		
		if(showCompact) {
			toolbarButtons.removeAll(pauseButton, rssButton);
		}
		else if(!isExpandedToolbar(toolbar)) {
			//Add Pause button after Start Torrent and RSS button after Add from URL button respectively
			final int rssButtonIndex = toolbarButtons.indexOf(toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_FROM_URL_NAME));
			toolbarButtons.add(rssButtonIndex + 1, rssButton);
			
			final int downloadButtonIndex = toolbarButtons.indexOf(toolbarButtonsMap.get(TOOLBAR_BUTTON_START_NAME));    			    			
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
		return toolbarButtons.contains(toolbarButtonsMap.get(TOOLBAR_BUTTON_PAUSE_NAME)) &&
				toolbarButtons.contains(toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_RSS_FEED_NAME));
	}
	
	private ToolBar buildToolbar() {				
		final String[] buttonUrls = new String[]{ImageUtils.ADD_IMAGE_LOCATION,
				ImageUtils.LINK_IMAGE_LOCATION, ImageUtils.RSS_DARK_IMAGE_LOCATION,
				ImageUtils.NEW_IMAGE_LOCATION, ImageUtils.DELETE_IMAGE_LOCATION,
				ImageUtils.DOWNLOAD_IMAGE_LOCATION, ImageUtils.PAUSE_IMAGE_LOCATION,
				ImageUtils.STOP_IMAGE_LOCATION, ImageUtils.UP_IMAGE_LOCATION,
				ImageUtils.DOWN_IMAGE_LOCATION, ImageUtils.LOCK_IMAGE_LOCATION,
				ImageUtils.MONITOR_IMAGE_LOCATION, ImageUtils.SETTINGS_IMAGE_LOCATION};
		
		final String[] buttonIds = {TOOLBAR_BUTTON_ADD_NAME, TOOLBAR_BUTTON_ADD_FROM_URL_NAME, TOOLBAR_BUTTON_ADD_RSS_FEED_NAME,
				"Create New Torrent", TOOLBAR_BUTTON_REMOVE_NAME, TOOLBAR_BUTTON_START_NAME, TOOLBAR_BUTTON_PAUSE_NAME,
				TOOLBAR_BUTTON_STOP_NAME, "Move Up Queue", "Move Down Queue", "Unlock Bundle", "Remote", TOOLBAR_BUTTON_OPTIONS_NAME};
		
		final boolean[] buttonStates = {false, false, false, false, true, true, true, true, true, true, true, false, false};
		
		final Button[] toolbarButtons = new Button[buttonUrls.length];
		for(int i = 0; i < toolbarButtons.length; ++i) {
			toolbarButtons[i] = buildToolbarButton(buttonUrls[i], buttonIds[i], buttonStates[i]);
		}
		
		toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_NAME).setOnAction(
				event -> onAddTorrent(fileActionHandler.onFileOpen(stage)));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_FROM_URL_NAME).setOnAction(
				event -> onAddTorrent(fileActionHandler.onLoadUrl(stage)));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_OPTIONS_NAME).setOnAction(
				event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_REMOVE_NAME).setOnAction(
				event -> onRemoveTorrent());
		toolbarButtonsMap.get(TOOLBAR_BUTTON_START_NAME).setOnAction(
				event -> onChangeTorrentState(QueuedTorrent.Status.ACTIVE));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_STOP_NAME).setOnAction(
				event -> onChangeTorrentState(QueuedTorrent.Status.STOPPED));

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
	
	private Button buildToolbarButton(final String imagePath, final String id, final boolean disabled) {
		final ImageView imageView = ImageUtils.colorImage(new Image(
				getClass().getResourceAsStream(imagePath)), TOOLBAR_BUTTON_COLOR, 
				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, 
				ImageUtils.TOOLBAR_BUTTON_IMAGE_SIZE, ImageUtils.TOOLBAR_BUTTON_IMAGE_SIZE);
		
		final Button button = new Button(null, imageView);		
		button.getStyleClass().add("toolbar-button");		
		button.setTooltip(new Tooltip(id));		
		button.setDisable(disabled);		

		toolbarButtonsMap.put(id, button);				
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
		final String[] tabNames = {GuiProperties.FILES_TAB_ID, "Info", "Peers", GuiProperties.TRACKERS_TAB_ID, "Speed"};        
        final String[] imagePaths = {ImageUtils.FOLDER_OPEN_IMAGE_LOCATION,
        		"/images/appbar.information.circle.png", "/images/appbar.group.png",
        		"/images/appbar.location.circle.png", "/images/appbar.graph.line.png"};
        final Map<Tab, ImageView> tabImageViews = new LinkedHashMap<>(); 
        final boolean tabIconsShown = ApplicationPreferences.getProperty(
    			GuiProperties.TAB_ICONS_VISIBLE, GuiProperties.DEFAULT_TAB_ICONS_VISIBLE);
        showTabIconsMenuItem.setSelected(tabIconsShown);
        
        for(int i = 0; i < tabNames.length; ++i) {  
        	final Image tabImage = new Image(getClass().getResourceAsStream(imagePaths[i]));
        	final Tab tab = new Tab(tabNames[i]);
        	tab.setId(tabNames[i]);
        	tab.setClosable(false); 
        	if(selectedTabId.equals(tab.getId())) {
				detailedInfoTabPane.getSelectionModel().select(tab);        		
			}
        	tab.setOnSelectionChanged(event -> {
        		final Color tabImageColor = tab.isSelected()? TAB_SELECTED_IMAGE_COLOR : TAB_DEFAULT_IMAGE_COLOR;
        		final ImageView imageView = ImageUtils.colorImage(tabImage, tabImageColor, 
        				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE);
        		tabImageViews.put(tab, imageView);    
        		if(showTabIconsMenuItem.isSelected()) {
        			tab.setGraphic(imageView);
        		}
        		updateGui();
        	});       		        
        	tabImageViews.put(tab, ImageUtils.colorImage(tabImage, Color.rgb(162, 170, 156), 
					ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE));
        	detailsTabMap.put(tabNames[i], tab);
        }
        
        final Consumer<Boolean> setTabGraphic = set ->
    		tabImageViews.keySet().forEach(tab -> tab.setGraphic(set? tabImageViews.get(tab) : null));
    	setTabGraphic.accept(tabIconsShown);
    	showTabIconsMenuItem.selectedProperty().addListener((obs, oldV, showTabIcons) -> {
    		setTabGraphic.accept(showTabIcons);
    		ApplicationPreferences.setProperty(GuiProperties.TAB_ICONS_VISIBLE, showTabIcons);
    	});
        
        final ScrollPane trackerTableScroll = new ScrollPane(trackerTable.getView());
        trackerTableScroll.setFitToHeight(true);
        trackerTableScroll.setFitToWidth(true);
        
        final ScrollPane torrentContentTreeScroll = new ScrollPane(torrentContentTree.getView());
        torrentContentTreeScroll.setFitToHeight(true);
        torrentContentTreeScroll.setFitToWidth(true);
        
        detailsTabMap.get(GuiProperties.FILES_TAB_ID).setContent(torrentContentTreeScroll);
        detailsTabMap.get(GuiProperties.TRACKERS_TAB_ID).setContent(trackerTableScroll);

        return tabImageViews.keySet();
    }
	
	private void addTabMenus(final ObservableList<Tab> tabs) {		
		tabs.forEach(t -> {
			final ContextMenu tabContextMenu = new ContextMenu();
			final MenuItem closeTabMenuItem = new MenuItem(GuiProperties.CLOSE_TAB_ID);			
			tabContextMenu.getItems().addAll(closeTabMenuItem);
			t.setContextMenu(tabContextMenu);			
			t.selectedProperty().addListener((obs, oldV, selected) ->
				closeTabMenuItem.setDisable(tabs.size() == 1));
		});
	}
	
	private void addDetailsTabHeaderContextMenu(final ObservableList<Tab> tabs) {	
		final ContextMenu tabHeaderContextMenu = new ContextMenu();		
		tabHeaderContextMenu.getItems().addAll(tabs.stream().map(t -> {
			final CheckMenuItem tabMenuItem = new CheckMenuItem(t.getText());
			final Optional<MenuItem> closeTabMenuItem = t.getContextMenu().getItems().stream().filter(
					mi -> mi.getText().equals(GuiProperties.CLOSE_TAB_ID)).findFirst();
			if(closeTabMenuItem.isPresent()) {
				closeTabMenuItem.get().setOnAction(e -> tabMenuItem.setSelected(false));
			}
			tabMenuItem.selectedProperty().addListener((obs, oldV, selected) -> {				
				if(selected && !tabs.contains(t)) {
					if(tabs.size() == 1) {
						final Tab remainingTab = tabs.get(0);
						tabHeaderContextMenu.getItems().stream().filter(
								mi -> mi.getText().equals(remainingTab.getText())).forEach(mi -> mi.setDisable(false));											
					}
					tabs.add(t);
					if(tabs.size() > 1) {
						tabs.forEach(tb -> tb.getContextMenu().getItems().forEach(mi -> mi.setDisable(false)));	
					}
				}
				else if(!selected && tabs.contains(t)) {
					tabs.remove(t);
					if(tabs.size() == 1) {
						final Tab remainingTab = tabs.get(0);
						tabHeaderContextMenu.getItems().stream().filter(
								mi -> mi.getText().equals(remainingTab.getText())).forEach(mi -> mi.setDisable(true));
						tabs.forEach(tb -> tb.getContextMenu().getItems().forEach(mi -> mi.setDisable(true)));
					}
				}
			});						
			tabMenuItem.setSelected(true);
			return tabMenuItem;
		}).collect(Collectors.toList()));
		
		detailedInfoTabPane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			final EventTarget eventTarget = e.getTarget();
			if(e.getButton() == MouseButton.SECONDARY && eventTarget instanceof StackPane) {
				final ObservableList<String> styleClasses = ((StackPane)eventTarget).getStyleClass();
				final Optional<String> tabHeaderStyle = styleClasses.stream().filter(
						sc -> "tab-header-background".equals(sc)).findFirst();
				if(tabHeaderStyle.isPresent()) {
					tabHeaderContextMenu.show(stage, e.getScreenX(), e.getScreenY());
				}
			}			
        });
	}
	
	private MenuBar buildMenuBar() {
		final MenuBar menuBar = new MenuBar();
		
		menuBar.getMenus().addAll(buildFileMenu(), buildOptionsMenu(), buildHelpMenu());
		
		return menuBar;
	}
	
	private Menu buildFileMenu() {
		final Menu fileMenu = new Menu("_File");
		fileMenu.setMnemonicParsing(true);
		
		final MenuItem addTorrentMenuItem = new MenuItem("Add Torrent...");
		addTorrentMenuItem.setOnAction(event -> onAddTorrent(fileActionHandler.onFileOpen(stage)));
		addTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		
		final MenuItem addTorrentAndChooseDirMenuItem = new MenuItem("Add Torrent (choose save dir)...");
		addTorrentAndChooseDirMenuItem.setOnAction(event -> fileActionHandler.onFileOpenAndChooseSaveLocation(stage));
		addTorrentAndChooseDirMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
		
		final MenuItem addTorrentFromUrlMenuItem = new MenuItem("Add Torrent from URL...");
		addTorrentFromUrlMenuItem.setOnAction(event -> onAddTorrent(fileActionHandler.onLoadUrl(stage)));
		addTorrentFromUrlMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+U"));
		
		final MenuItem addRssFeedMenuItem = new MenuItem("Add RSS Feed...");
		
		final MenuItem createTorrentMenuItem = new MenuItem("Create New Torrent...");
		createTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
		
		final MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.setOnAction(this::onShutdown);
		
		fileMenu.getItems().addAll(addTorrentMenuItem, addTorrentAndChooseDirMenuItem,
				addTorrentFromUrlMenuItem, addRssFeedMenuItem, new SeparatorMenuItem(),
				createTorrentMenuItem, new SeparatorMenuItem(), exitMenuItem);
		
		return fileMenu;
	}
	
	private Menu buildOptionsMenu() {
		final Menu optionsMenu = new Menu("_Options");
		optionsMenu.setMnemonicParsing(true);
		
		final MenuItem preferencesMenuItem = new MenuItem("Preferences...");
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
	
	private void onAddTorrent(final TorrentOptions torrentOptions) {		
		if(torrentOptions != null) {
			final InfoHash torrentInfoHash = torrentOptions.getInfoHash();
			if(queuedTorrentManager.find(torrentInfoHash).isPresent()) {
				final Alert existingTorrentAlert = new Alert(AlertType.ERROR,
						"The torrent already exists.\n" +
								"Would you like to load trackers from it?",
								ButtonType.OK, ButtonType.CANCEL);
				existingTorrentAlert.setTitle("Existing torrent file");
				existingTorrentAlert.setHeaderText(null);
				existingTorrentAlert.showAndWait();			
				return;
			}
			
			final BinaryEncodedDictionary metaData = torrentOptions.getMetaData();
			final BinaryEncodedString announceUrl = (BinaryEncodedString)metaData.get(
					BinaryEncodingKeyNames.KEY_ANNOUNCE);
			final BinaryEncodedList announceList = (BinaryEncodedList)metaData.get(
					BinaryEncodingKeyNames.KEY_ANNOUNCE_LIST);
			
			final Set<String> trackerUrls = new HashSet<>();
			
			if(announceUrl != null) {
				final String trackerUrl = announceUrl.toString(); 
				trackerUrls.add(trackerUrl);
			}
			if(announceList != null) {
				announceList.stream().flatMap(l -> ((BinaryEncodedList)l).stream())
					.forEach(url -> trackerUrls.add(url.toString()));
			}
			
			final InfoHash infoHash = torrentOptions.getInfoHash();
			final QueuedTorrent.Status torrentStatus = torrentOptions.isStartTorrent()? 
					QueuedTorrent.Status.ACTIVE : QueuedTorrent.Status.STOPPED;
			final QueuedTorrent queuedTorrent = new QueuedTorrent(infoHash, 1, torrentStatus);
			
			final TorrentJobView jobView = new TorrentJobView(queuedTorrent, torrentOptions.getName(), 
					torrentOptions.getTorrentContents());
			
			final ObservableList<TrackerView> trackerViews = FXCollections.observableArrayList(
					trackerUrls.stream().map(t -> new TrackerView(t, torrentStatus)).collect(Collectors.toList()));
			
			trackerViewMappings.put(queuedTorrent, trackerViews);
			trackerTable.setContent(trackerViews);
			torrentJobTable.addJob(jobView);			
			queuedTorrentManager.add(queuedTorrent, trackerUrls);
			
			updateGui();
		}
	}
	
	private void onChangeTorrentState(final QueuedTorrent.Status newStatus) {
		toolbarButtonsMap.get(TOOLBAR_BUTTON_START_NAME).setDisable(newStatus == QueuedTorrent.Status.ACTIVE);
		toolbarButtonsMap.get(TOOLBAR_BUTTON_STOP_NAME).setDisable(newStatus == QueuedTorrent.Status.STOPPED);
		
		final ObservableList<TorrentJobView> selectedTorrentJobs = torrentJobTable.getSelectedJobs();
		
		if(selectedTorrentJobs.size() > 0) {
			selectedTorrentJobs.stream().map(
					TorrentJobView::getQueuedTorrent).forEach(t -> t.setStatus(newStatus));			
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
						TorrentJobView::getQueuedTorrent).forEach(queuedTorrentManager::remove);
				torrentJobTable.deleteJobs(selectedTorrentJobs);
				final ObservableList<TorrentJobView> newSelection = torrentJobTable.getSelectedJobs();
				if(newSelection.isEmpty()) {
					torrentContentTree.setContent(null);
					trackerTable.setContent(FXCollections.emptyObservableList());
				}
				else {
					torrentJobTable.selectJob(newSelection.get(0));
				}
			}
		}
	}
	
	private void onTorrentJobSelection(final TorrentJobView selectedTorrentJob) {
		final boolean torrentSelected = selectedTorrentJob != null;
		if(torrentSelected) {							
			torrentContentTree.setContent(selectedTorrentJob.getTorrentContents());			
			trackerTable.setContent(trackerViewMappings.get(selectedTorrentJob.getQueuedTorrent()));
			updateGui();
		}
		toolbarButtonsMap.get(TOOLBAR_BUTTON_START_NAME).setDisable(
				!torrentSelected || selectedTorrentJob.getQueuedTorrent().getStatus() == QueuedTorrent.Status.ACTIVE);
		toolbarButtonsMap.get(TOOLBAR_BUTTON_STOP_NAME).setDisable(
				!torrentSelected || selectedTorrentJob.getQueuedTorrent().getStatus() == QueuedTorrent.Status.STOPPED);
		
		final Button removeButton = toolbarButtonsMap.get(TOOLBAR_BUTTON_REMOVE_NAME);	
		final ImageView buttonImageView = (ImageView)removeButton.getGraphic();
		
		removeButton.setDisable(!torrentSelected);
		final Color buttonImageColor = removeButton.isDisabled()? 
				TOOLBAR_BUTTON_COLOR : REMOVE_BUTTON_COLOR; 
		
		removeButton.setGraphic(ImageUtils.colorImage(buttonImageView.getImage(), buttonImageColor, 
				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, (int)buttonImageView.getFitWidth(), 
				(int)buttonImageView.getFitHeight()));
	}
	
	//TODO: Store all window changes in this method only
	private void storeWindowPreferences() {
		//Store currently selected tab, if needed
		final Tab selectedTab = detailedInfoTabPane.getSelectionModel().getSelectedItem();
		if(!ApplicationPreferences.getProperty(GuiProperties.SELECTED_TAB_ID,
				GuiProperties.DEFAULT_SELECTED_TAB_ID).equals(selectedTab.getId())) {			
			ApplicationPreferences.setProperty(GuiProperties.SELECTED_TAB_ID, selectedTab.getId());
		}
		//Store status bar visibility, if needed
		final boolean wasStatusBarShown = ApplicationPreferences.getProperty(
				GuiProperties.STATUSBAR_VISIBLE, GuiProperties.DEFAULT_STATUSBAR_VISIBLE);
		final boolean isStatusBarShown = showStatusBarMenuItem.isSelected();
		if(isStatusBarShown != wasStatusBarShown) {
			ApplicationPreferences.setProperty(GuiProperties.STATUSBAR_VISIBLE, isStatusBarShown);
		}
	}
	
	private void onShutdown(final Event event) {
		final boolean isShuttingDown = windowActionHandler.onWindowClose(event, stage);
		
		if(isShuttingDown) {
			//Stop updating periodic tasks
			periodicTaskRunner.cancel();
			
			//Perform resource cleanup before shutdown
			ResourceManager.INSTANCE.cleanup();
			
			//Store any changes to window components
			storeWindowPreferences();
			
			//User chose to close the application, quit
	        Platform.exit();
		}
	}
}
