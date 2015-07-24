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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

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

/**
 * A main application window, showing all of the GUI components.
 * 
 * @author vedran
 *
 */
public final class ApplicationWindow {
	
	private static final String TOOLBAR_BUTTON_ADD_FROM_URL = "Add Torrent from URL";
	private static final String TOOLBAR_BUTTON_OPTIONS = "Preferences";
	private static final String TOOLBAR_BUTTON_ADD = "Add Torrent";
	private static final String TOOLBAR_BUTTON_REMOVE = "Remove";	
	
	private static final String TRACKERS_TAB_FILES_NAME = "Trackers";
	private static final String DETAILS_TAB_FILES_NAME = "Files";
		
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
	
	//Mapping between a torrent and it's tracker views
	private final Map<QueuedTorrent, ObservableList<TrackerView>> trackerViewMappings = new HashMap<>();
	
	//Mapping between toolbar's buttons and their names
	private final Map<String, Button> toolbarButtonsMap = new HashMap<>();
	
	//Mapping between details tabs and their names
	private final Map<String, Tab> detailsTabMap = new HashMap<>();
	
	//A service that periodically executes actions (such as GUI update)
	private final PeriodicTaskRunner periodicTaskRunner = new PeriodicTaskRunner();
		
	private final Stage stage;

	public ApplicationWindow(final Stage stage) {
		this.stage = stage;
		
		final BorderPane mainPane = new BorderPane();		
		mainPane.setTop(buildNorthPane());
		mainPane.setCenter(buildCenterPane());
		
		final Scene scene = new Scene(mainPane, 900, 550);		
		scene.getStylesheets().add("/ui-style.css");
		stage.setScene(scene);
		
		initComponents();
	}
	
	private void initComponents() {		
		torrentJobTable.addSelectionListener(this::onTorrentJobSelection);		
		torrentContentTree.getView().setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		
		stage.setOnCloseRequest(this::onShutdown);		
		stage.setTitle("jfxTorrent");        
		stage.centerOnScreen();		
		stage.show();
		
		periodicTaskRunner.addTask(createGuiUpdateTask());		
		periodicTaskRunner.setPeriod(Duration.seconds(1));		
		periodicTaskRunner.start();
	}
	
	private PeriodicTask createGuiUpdateTask() {
		long guiUpdateInterval = GuiProperties.DEFAULT_GUI_UPDATE_INTERVAL;
		final String updateIntervalProperty = ApplicationPreferences.getProperty(
    			GuiProperties.GUI_UPDATE_INTERVAL, String.valueOf(GuiProperties.DEFAULT_GUI_UPDATE_INTERVAL));
				          
    	try {
    		guiUpdateInterval = Long.parseLong(updateIntervalProperty);
    	} 
    	catch(final NumberFormatException nfe) {}		
		
		return new PeriodicTask(this::updateGui, guiUpdateInterval);
	}
	
	private void updateGui() {		
		final List<TorrentJobView> selectedTorrents = torrentJobTable.getSelectedJobs();

		if(!selectedTorrents.isEmpty()) {
			//Render tracker statistics only if Trackers tab is selected
			if(detailsTabMap.get(TRACKERS_TAB_FILES_NAME).isSelected()) {
				queuedTorrentManager.trackerSnapshot(
					selectedTorrents.get(0).getQueuedTorrent(),
					trackerTable.getTrackerViews());
			}
		}
	}
	
	private Pane initFilterTreeView() {
		filterTreeView.getStyleClass().add("filter-list-view");
		filterTreeView.setRoot(buildFilterTreeViewItems());
		filterTreeView.setShowRoot(false);
		filterTreeView.getSelectionModel().select(0);
		filterTreeView.requestFocus();
		
		final StackPane treeViewStack = new StackPane();
		treeViewStack.getChildren().add(filterTreeView);		
		
		return treeViewStack;
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
	
	private Pane buildNorthPane() {
		final VBox northPane = new VBox();
		northPane.getChildren().add(buildMenuBar());
		
		return northPane;
	}
	
	private SplitPane buildCenterPane() {
		final SplitPane verticalSplitPane = new SplitPane();
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.setDividerPosition(0, 0.6f);      
        verticalSplitPane.getItems().addAll(buildToolbarAndTorrentListPane(), buildTorrentDetailsPane());
        
        final Pane filterTreeView = initFilterTreeView();
        
        final SplitPane horizontalSplitPane = new SplitPane();        
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizontalSplitPane.setDividerPosition(0, 0.20);
        horizontalSplitPane.getItems().addAll(filterTreeView, verticalSplitPane);
        
        SplitPane.setResizableWithParent(filterTreeView, Boolean.FALSE);
        
        return horizontalSplitPane;
	}
	
	private Pane buildToolbarAndTorrentListPane() {				
		final BorderPane centerPane = new BorderPane();
		centerPane.setTop(buildToolbar());
		centerPane.setCenter(buildTorrentJobTable());
		
		return centerPane;
	}
	
	private ToolBar buildToolbar() {				
		final String[] buttonUrls = new String[]{"/images/appbar.add.png",
				"/images/appbar.link.png", "/images/appbar.page.new.png", 
				"/images/appbar.delete.png", "/images/appbar.download.png",
				"/images/appbar.control.stop.png", "/images/appbar.chevron.up.png",
				"/images/appbar.chevron.down.png", "/images/appbar.unlock.keyhole.png",
				"/images/appbar.monitor.png", "/images/appbar.settings.png"};
		
		final String[] buttonIds = {TOOLBAR_BUTTON_ADD, TOOLBAR_BUTTON_ADD_FROM_URL, "Create New Torrent", 
				TOOLBAR_BUTTON_REMOVE, "Start Torrent", "Stop Torrent", 
				"Move Up Queue", "Move Down Queue", 
				"Unlock Bundle", "Remote", TOOLBAR_BUTTON_OPTIONS};
		
		final boolean[] buttonStates = {false, false, false, true, true, true, true, true, true, false, false};
		
		final Button[] toolbarButtons = new Button[buttonUrls.length];
		for(int i = 0; i < toolbarButtons.length; ++i) {
			toolbarButtons[i] = buildToolbarButton(buttonUrls[i], buttonIds[i], buttonStates[i]);
		}
		
		toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD).setOnAction(
				event -> onAddTorrent(fileActionHandler.onFileOpen(stage)));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_ADD_FROM_URL).setOnAction(
				event -> onAddTorrent(fileActionHandler.onLoadUrl(stage)));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_OPTIONS).setOnAction(
				event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));
		toolbarButtonsMap.get(TOOLBAR_BUTTON_REMOVE).setOnAction(
				event -> onRemoveTorrent());

		final HBox separatorBox = new HBox();		
		HBox.setHgrow(separatorBox, Priority.ALWAYS);
		
		final Node[] toolbarContents = {toolbarButtons[0], toolbarButtons[1],
				buildToolbarSeparator(), toolbarButtons[2], buildToolbarSeparator(), 
				toolbarButtons[3], buildToolbarSeparator(), toolbarButtons[4], toolbarButtons[5],
				buildToolbarSeparator(), toolbarButtons[6], toolbarButtons[7], buildToolbarSeparator(),
				toolbarButtons[8], buildToolbarSeparator(), separatorBox, buildToolbarSeparator(), 
				toolbarButtons[9], toolbarButtons[10]};
		
		
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
	
	private Pane buildTorrentDetailsPane() {
		final TabPane torrentDetailsTab = new TabPane();
		torrentDetailsTab.getTabs().addAll(buildTorrentDetailsTabs());	
		torrentDetailsTab.getSelectionModel().selectFirst();
		
		final StackPane detailsPane = new StackPane();
		detailsPane.getChildren().add(torrentDetailsTab);
		return detailsPane;
	}
	
	private Collection<Tab> buildTorrentDetailsTabs() {
		final String[] tabNames = {DETAILS_TAB_FILES_NAME, "Info", "Peers", TRACKERS_TAB_FILES_NAME, "Speed"};        
        final String[] imagePaths = {"/images/appbar.folder.open.png",
        		"/images/appbar.information.circle.png", "/images/appbar.group.png",
        		"/images/appbar.location.circle.png", "/images/appbar.graph.line.png"};
        final List<Tab> tabList = new ArrayList<>(); 
        
        for(int i = 0; i < tabNames.length; ++i) {  
        	final Image tabImage = new Image(getClass().getResourceAsStream(imagePaths[i]));
        	final Tab tab = new Tab(tabNames[i]);
        	tab.setGraphic(ImageUtils.colorImage(tabImage, Color.rgb(162, 170, 156), 
					ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE));
        	tab.setClosable(false);        	
        	tab.setOnSelectionChanged(event -> {
        		final Color tabImageColor = tab.isSelected()? TAB_SELECTED_IMAGE_COLOR : TAB_DEFAULT_IMAGE_COLOR;
        		tab.setGraphic(ImageUtils.colorImage(tabImage, tabImageColor, 
        				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, TAB_ICON_SIZE, TAB_ICON_SIZE));
        		updateGui();
        	});        		        	
        	tabList.add(tab);
        	detailsTabMap.put(tabNames[i], tab);
        }
        
        detailsTabMap.get(DETAILS_TAB_FILES_NAME).setContent(torrentContentTree.getView());
        detailsTabMap.get(TRACKERS_TAB_FILES_NAME).setContent(trackerTable.getView());
        
        return tabList;
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
		
		final MenuItem optionsMenuItem = new MenuItem("Preferences...");
		optionsMenuItem.setOnAction(event -> windowActionHandler.onOptionsWindowShown(stage, fileActionHandler));
		optionsMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+P"));
		
		optionsMenu.getItems().addAll(optionsMenuItem);
		
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
			final QueuedTorrent queuedTorrent = new QueuedTorrent(infoHash, trackerUrls, 1, torrentStatus);
			
			final TorrentJobView jobView = new TorrentJobView(queuedTorrent, torrentOptions.getName(), 
					torrentOptions.getTorrentContents());
			
			final ObservableList<TrackerView> trackerViews = FXCollections.observableArrayList(
					queuedTorrent.getTrackers().map(t -> new TrackerView(t)).collect(Collectors.toList()));
			
			trackerViewMappings.put(queuedTorrent, trackerViews);
			trackerTable.setContent(trackerViews);
			torrentJobTable.addJob(jobView);
			queuedTorrentManager.add(queuedTorrent);
			updateGui();
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
		if(selectedTorrentJob != null) {				
			torrentContentTree.setContent(selectedTorrentJob.getTorrentContents());
			trackerTable.setContent(trackerViewMappings.get(selectedTorrentJob.getQueuedTorrent()));
		}
		
		final Button removeButton = toolbarButtonsMap.get(TOOLBAR_BUTTON_REMOVE);	
		final ImageView buttonImageView = (ImageView)removeButton.getGraphic();
		
		removeButton.setDisable(selectedTorrentJob == null);
		final Color buttonImageColor = removeButton.isDisabled()? 
				TOOLBAR_BUTTON_COLOR : REMOVE_BUTTON_COLOR; 
		
		removeButton.setGraphic(ImageUtils.colorImage(buttonImageView.getImage(), buttonImageColor, 
				ImageUtils.CROPPED_MARGINS_IMAGE_VIEW, (int)buttonImageView.getFitWidth(), 
				(int)buttonImageView.getFitHeight()));
	}
	
	private void onShutdown(final Event event) {
		final boolean isShuttingDown = windowActionHandler.onWindowClose(event, stage);
		
		if(isShuttingDown) {
			//Stop updating periodic tasks
			periodicTaskRunner.cancel();
			
			//Perform resource cleanup before shutdown
			ResourceManager.INSTANCE.cleanup();
			
			//User chose to close the application, quit
	        Platform.exit();
		}
	}
}
