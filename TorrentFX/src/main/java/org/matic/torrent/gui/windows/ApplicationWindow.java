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

package org.matic.torrent.gui.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.matic.torrent.gui.action.WindowActionHandler;
import org.matic.torrent.gui.model.TorrentStatus;

public final class ApplicationWindow {
	
	private static final String ADD_TORRENT_COMMAND = "add.torrent";
	private static final String ADD_TORRENT_LINK_COMMAND = "add.torrent.link";
	private static final String CREATE_TORRENT_COMMAND = "create.torrent"; 
	private static final String DELETE_TORRENT_COMMAND = "delete.torrent";
	private static final String DOWNLOAD_TORRENT_COMMAND = "download.torrent";
	private static final String STOP_TORRENT_COMMAND = "stop.torrent"; 
	private static final String MOVE_TORRENT_UP_COMMAND = "move.torrent.up"; 
	private static final String MOVE_TORRENT_DOWN_COMMAND = "move.torrent.down";
	private static final String LOCK_COMMAND = "lock.app";
	
	private final WindowActionHandler windowActionHandler = new WindowActionHandler();
	
	//Toolbar buttons, stored and identified by their command name string
	private final Map<String, Button> toolbarButtons = new HashMap<>();
	
	//View for filtering torrents according to their status
	private final TreeView<Node> filterTreeView = new TreeView<>(); 
	
	private final Stage stage;

	public ApplicationWindow(final Stage stage) {
		this.stage = stage;
		
		final BorderPane mainPane = new BorderPane();		
		mainPane.setTop(buildNorthPane());
		mainPane.setCenter(buildCenterPane());
		
		final Scene scene = new Scene(mainPane, 900, 550);
		scene.getStylesheets().add(getClass().getResource("../css/ui-style.css").toExternalForm());
		stage.setScene(scene);
		
		initComponents();
	}
	
	private void initComponents() {		
		stage.setOnCloseRequest(event -> windowActionHandler.onWindowClose(event, stage));		
		stage.setTitle("jfxTorrent");        
		stage.centerOnScreen();        
        stage.show();
	}
	
	private Pane initFilterTreeView() {
		filterTreeView.getStyleClass().add("filter-list-view");
		filterTreeView.setRoot(buildFilterTreeViewItems());
		filterTreeView.setShowRoot(false);		
		
		final StackPane treeViewStack = new StackPane();
		treeViewStack.getChildren().add(filterTreeView);
		
		return treeViewStack;
	}
	
	private TreeItem<Node> buildFilterTreeViewItems() {		
		final List<TreeItem<Node>> torrentNodeElements = Arrays.asList("Downloading (0)",
				"Seeding (0)", "Completed (0)", "Active (0)", "Inactive (0)").stream().map(labelName -> {
			final Label label = new Label(labelName);
			label.getStyleClass().add("filter-list-text-cell");
			return new TreeItem<Node>(label);
		}).collect(Collectors.toList());
		
		final List<TreeItem<Node>> labelsNodeElements = Arrays.asList("No Label (0)").stream().map(labelName -> {
			final Label label = new Label(labelName);
			label.getStyleClass().add("filter-list-text-cell");
			return new TreeItem<Node>(label);
		}).collect(Collectors.toList());
		
		final Image torrentsRootImage = new Image(getClass().getResourceAsStream("/images/appbar.arrow.down.up.png"),
				25, 25, true, true);
		final Label torrentsRootLabel = new Label("Torrents (0)");
		torrentsRootLabel.getStyleClass().add("filter-list-text-cell");
		torrentsRootLabel.setGraphic(new ImageView(torrentsRootImage));
		final TreeItem<Node> torrentsRootNode = new TreeItem<>(torrentsRootLabel);
		torrentsRootNode.setExpanded(true);
		torrentsRootNode.getChildren().addAll(torrentNodeElements);
		
		final Image labelsRootImage = new Image(getClass().getResourceAsStream("/images/appbar.tag.label.png"),
				25, 25, true, true);
		final Label labelsRootLabel = new Label("Labels");
		labelsRootLabel.getStyleClass().add("filter-list-text-cell");
		labelsRootLabel.setGraphic(new ImageView(labelsRootImage));
		final TreeItem<Node> labelsRootNode = new TreeItem<>(labelsRootLabel);
		labelsRootNode.setExpanded(true);
		labelsRootNode.getChildren().addAll(labelsNodeElements);
		
		final Image rssFeedsRootImage = new Image(getClass().getResourceAsStream("/images/appbar.rss.png"),
				25, 25, true, true);
		final Label rssFeedsRootLabel = new Label("Feeds (0)");
		rssFeedsRootLabel.getStyleClass().add("filter-list-text-cell");
		rssFeedsRootLabel.setGraphic(new ImageView(rssFeedsRootImage));
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
        
        final SplitPane horizontalSplitPane = new SplitPane();        
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizontalSplitPane.setDividerPosition(0, 0.20);
        horizontalSplitPane.getItems().addAll(initFilterTreeView(), verticalSplitPane);
        
        return horizontalSplitPane;
	}
	
	private Pane buildToolbarAndTorrentListPane() {				
		final BorderPane centerPane = new BorderPane();
		centerPane.setTop(buildToolbarPane());
		centerPane.setCenter(buildTorrentListTable());
		
		return centerPane;
	}
	
	private Pane buildToolbarPane() {				
		final String[] buttonUrls = new String[]{"/images/appbar.add.png",
				"/images/appbar.link.png", null, "/images/appbar.page.new.png", null, 
				"/images/appbar.delete.png", null, "/images/appbar.download.png",
				"/images/appbar.control.stop.png", null, "/images/appbar.chevron.up.png",
				"/images/appbar.chevron.down.png", null, "/images/appbar.unlock.keyhole.png", null};
		final String[] buttonTooltips = {"Add Torrent", "Add Torrent from URL", "Create New Torrent", 
				"Remove", "Start Torrent", "Stop Torrent", 
				"Move Up Queue", "Move Down Queue", 
				"Unlock Bundle"};
		final String[] buttonCommands = {ADD_TORRENT_COMMAND, ADD_TORRENT_LINK_COMMAND,
				CREATE_TORRENT_COMMAND, DELETE_TORRENT_COMMAND, DOWNLOAD_TORRENT_COMMAND,
				STOP_TORRENT_COMMAND, MOVE_TORRENT_UP_COMMAND, MOVE_TORRENT_DOWN_COMMAND,
				LOCK_COMMAND};
		final boolean[] buttonState = {true, true, true, false, false, false, false,
				false, false};
		final List<Node> leftToolbarButtons = new ArrayList<>();
			
		for(int i = 0, j = 0; i < buttonUrls.length; ++i) {
			final String buttonUrl = buttonUrls[i];
			if(buttonUrl == null) {
				final Separator separator = new Separator(Orientation.VERTICAL);
				separator.getStyleClass().add("toolbar-separator");
				separator.setDisable(true);
				leftToolbarButtons.add(separator);
				continue;
			}
			final String buttonCommand = buttonCommands[j];
			final Button toolbarButton = new Button(null, new ImageView(new Image(
					getClass().getResourceAsStream(buttonUrl),
					26, 26, true, true)));
			toolbarButton.getStyleClass().add("toolbar-button");
			toolbarButton.setTooltip(new Tooltip(buttonTooltips[j]));
			toolbarButton.setDisable(!buttonState[j]);
			toolbarButton.setId(buttonCommand);
			leftToolbarButtons.add(toolbarButton);
			toolbarButtons.put(buttonCommand, toolbarButton);
			++j;
		}
		
		final List<Node> rightToolbarButtons = Arrays.stream(new String[]{null, "/images/appbar.monitor.png",
				"/images/appbar.settings.png"}).map(url -> {
			if(url == null) {
				final Separator separator = new Separator(Orientation.VERTICAL);
				separator.getStyleClass().add("toolbar-separator");
				separator.setDisable(true);
				return separator;
			}
			final Button toolbarButton = new Button(null, new ImageView(new Image(
					getClass().getResourceAsStream(url),
					26, 26, true, true)));
			toolbarButton.getStyleClass().add("toolbar-button");
			return toolbarButton;
		}).collect(Collectors.toList());;
		
		final HBox leftToolbarPane = new HBox();
		leftToolbarPane.getChildren().addAll(leftToolbarButtons);
		
		final HBox rightToolbarPane = new HBox();
		rightToolbarPane.getChildren().addAll(rightToolbarButtons);
		
		final BorderPane toolbarPane = new BorderPane();
		toolbarPane.setLeft(leftToolbarPane);
		toolbarPane.setRight(rightToolbarPane);
	
		return toolbarPane;
	}
	
	private Pane buildTorrentListTable() {		
		final TableView<TorrentStatus> torrentListTable = new TableView<>();
		
		final Text emptyTorrentListPlaceholder = new Text("Go to Bundles to get torrents.");
		emptyTorrentListPlaceholder.getStyleClass().add("empty-torrent-list-text");
		emptyTorrentListPlaceholder.visibleProperty().bind(Bindings.isEmpty(torrentListTable.getItems()));
		
		final BorderPane placeholderPane = new BorderPane();
		placeholderPane.setPadding(new Insets(15, 0, 0, 40));
		placeholderPane.setLeft(emptyTorrentListPlaceholder);
		
		torrentListTable.setPlaceholder(placeholderPane);
		
		final StackPane torrentListPane = new StackPane();
		torrentListPane.getChildren().add(torrentListTable);
		
		return torrentListPane;
	}
	
	private Pane buildTorrentDetailsPane() {
		final TabPane detailsTab = new TabPane();
		detailsTab.getTabs().addAll(buildTorrentDetailsTabs());
		
		final StackPane detailsPane = new StackPane();
		detailsPane.getChildren().add(detailsTab);
		return detailsPane;
	}
	
	private Collection<Tab> buildTorrentDetailsTabs() {
		final String[] tabNames = {"Files", "Info", "Peers", "Trackers", "Speed"};        
        final String[] imagePaths = {"/images/appbar.folder.open.png",
        		"/images/appbar.information.circle.png", "/images/appbar.group.png",
        		"/images/appbar.location.circle.png", "/images/appbar.graph.line.png"};
        final List<Tab> tabList = new ArrayList<>();
        
        for(int i = 0; i < tabNames.length; ++i) {
        	final Tab tab = new Tab(tabNames[i]);
        	tab.setGraphic(new ImageView(new Image(
					getClass().getResourceAsStream(imagePaths[i]), 20, 20, true, true)));
        	tab.setClosable(false);
        	tabList.add(tab);
        }
        
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
		addTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		
		final MenuItem addTorrentAndChooseDirMenuItem = new MenuItem("Add Torrent (choose save dir)...");
		addTorrentAndChooseDirMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
		
		final MenuItem addTorrentFromUrlMenuItem = new MenuItem("Add Torrent from URL...");
		addTorrentFromUrlMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+U"));
		
		final MenuItem addRssFeedMenuItem = new MenuItem("Add RSS Feed...");
		
		final MenuItem createTorrentMenuItem = new MenuItem("Create New Torrent...");
		createTorrentMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
		
		final MenuItem exitMenuItem = new MenuItem("Exit");
		
		fileMenu.getItems().addAll(addTorrentMenuItem, addTorrentAndChooseDirMenuItem,
				addTorrentFromUrlMenuItem, addRssFeedMenuItem, new SeparatorMenuItem(),
				createTorrentMenuItem, new SeparatorMenuItem(), exitMenuItem);
		
		return fileMenu;
	}
	
	private Menu buildOptionsMenu() {
		final Menu optionsMenu = new Menu("_Options");
		optionsMenu.setMnemonicParsing(true);
		
		final MenuItem optionsMenuItem = new MenuItem("Preferences...");
		optionsMenuItem.setAccelerator(KeyCombination.keyCombination("Ctrl+P"));
		
		optionsMenu.getItems().addAll(optionsMenuItem);
		
		return optionsMenu;
	}
	
	private Menu buildHelpMenu() {
		final Menu helpMenu = new Menu("_Help");
		helpMenu.setMnemonicParsing(true);
		
		return helpMenu;
	}
}
