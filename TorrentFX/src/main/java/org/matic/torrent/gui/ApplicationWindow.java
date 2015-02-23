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

package org.matic.torrent.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.matic.torrent.gui.action.WindowActionHandler;
import org.matic.torrent.gui.model.TorrentStatus;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public final class ApplicationWindow extends Application {
	
	private final WindowActionHandler windowActionHandler = new WindowActionHandler();
	
	//View for filtering torrents according to their status
	private final TreeView<Node> filterTreeView = new TreeView<>(); 
	
	private Stage stage;

	/**
	 * Main application execution entry point. Used when the application packaging
	 * is performed by other means than by JavaFX
	 * 
	 * @param args Application parameters
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public final void start(final Stage stage) throws Exception {
		this.stage = stage;
		
		final BorderPane mainPane = new BorderPane();		
		mainPane.setTop(buildNorthPane());
		mainPane.setCenter(buildCenterPane());
		
		final Scene scene = new Scene(mainPane, 900, 550);
		scene.getStylesheets().add(getClass().getResource("css/ui-style.css").toExternalForm());
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
        verticalSplitPane.getItems().addAll(buildTorrentListTable(), buildTorrentDetailsTab());
        
        final SplitPane horizontalSplitPane = new SplitPane();        
        horizontalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizontalSplitPane.setDividerPosition(0, 0.20);
        horizontalSplitPane.getItems().addAll(initFilterTreeView(), verticalSplitPane);
        
        return horizontalSplitPane;
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
	
	private Pane buildTorrentDetailsTab() {
		final TabPane detailsTab = new TabPane();
		detailsTab.getTabs().addAll(buildTorrentDetailsTabs());
		
		final StackPane detailsPane = new StackPane();
		detailsPane.getChildren().add(detailsTab);
		return detailsPane;
	}
	
	private Collection<Tab> buildTorrentDetailsTabs() {
        final List<String> tabNames = Arrays.asList("Files", "Info", "Peers");
      
        return tabNames.stream().map(tabName -> {
        	final Tab tab = new Tab(tabName);
        	tab.setClosable(false);
        	return tab;
        }).collect(Collectors.toList());
    }
	
	private MenuBar buildMenuBar() {
		final MenuBar menuBar = new MenuBar();
		
		menuBar.getMenus().addAll(buildFileMenu(), buildOptionsMenu(), buildHelpMenu());
		
		return menuBar;
	}
	
	private Menu buildFileMenu() {
		final Menu fileMenu = new Menu("_File");
		fileMenu.setMnemonicParsing(true);
		
		return fileMenu;
	}
	
	private Menu buildOptionsMenu() {
		final Menu optionsMenu = new Menu("_Options");
		optionsMenu.setMnemonicParsing(true);
		
		return optionsMenu;
	}
	
	private Menu buildHelpMenu() {
		final Menu helpMenu = new Menu("_Help");
		helpMenu.setMnemonicParsing(true);
		
		return helpMenu;
	}
}
