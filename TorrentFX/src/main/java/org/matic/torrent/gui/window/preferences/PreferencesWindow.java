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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.matic.torrent.gui.action.FileActionHandler;
import org.matic.torrent.preferences.CssProperties;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A window showing all program preferences that can be changed by the user.
 * 
 * @author vedran
 *
 */
public final class PreferencesWindow {
	
	//View for selecting available option categories
	private final TreeView<Node> optionCategoriesTreeView = new TreeView<>();
	private final Label categoryNameLabel = new Label();
	
	//Whether any of the preferences has been changed by the user
	private final BooleanProperty preferencesChanged = new SimpleBooleanProperty(false);
	
	private final CategoryContentPane[] categoryContentPanes = new CategoryContentPane[6];
	
	private final Dialog<ButtonType> window;
	
	private final Map<String, Node> optionGroupMappings;
	private final Pane optionsView = new StackPane();
	
	public PreferencesWindow(final Window owner, final FileActionHandler fileActionHandler) {
		optionGroupMappings = new HashMap<>();

        categoryContentPanes[0] = new UiSettingsContentPane(preferencesChanged);
        categoryContentPanes[1] = new DirectoriesContentPane(owner, fileActionHandler, preferencesChanged);
        categoryContentPanes[2] = new ConnectionContentPane(preferencesChanged);
        categoryContentPanes[3] = new BitTorrentContentPane(preferencesChanged);
        categoryContentPanes[4] = new QueueingContentPane(preferencesChanged);
        categoryContentPanes[5] = new UiExtrasContentPane(preferencesChanged, owner.getScene());
		
		window = new Dialog<>();
		window.initOwner(owner);		
		initComponents();
	}
	
	public void showAndWait() {
		window.showAndWait();
	}
	
	private void initComponents() {				
		final Node contentLayout = layoutContent();
		optionCategoriesTreeView.getStyleClass().add(CssProperties.OPTION_CATEGORY_LIST);
		optionCategoriesTreeView.setRoot(buildOptionCategories());
		optionCategoriesTreeView.setShowRoot(false);		
		optionCategoriesTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
			final String selectedCategoryName = ((Label)obs.getValue().getValue()).getText();
			categoryNameLabel.setText(selectedCategoryName);
			optionsView.getChildren().clear();
			optionsView.getChildren().add(optionGroupMappings.get(selectedCategoryName));
		});
		optionCategoriesTreeView.getSelectionModel().select(2);	
		
		categoryNameLabel.getStyleClass().add(CssProperties.OPTION_CATEGORY_TITLE);
		categoryNameLabel.setText(((Label)optionCategoriesTreeView.getSelectionModel()
				.getSelectedItem().getValue()).getText());
		
		window.setHeaderText(null);
		window.setTitle("Preferences");		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.APPLY);
		
		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);	
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			if(preferencesChanged.get()) {
				savePreferences();
			}
		});
		
		final Button applyButton = (Button)window.getDialogPane().lookupButton(ButtonType.APPLY);	
		applyButton.disableProperty().bind(preferencesChanged.not());
		
		applyButton.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume();
			savePreferences();
			preferencesChanged.set(false);
		});
		
		window.setResizable(true);	
		window.getDialogPane().setContent(contentLayout);
	}
	
	private void savePreferences() {	
		Arrays.stream(categoryContentPanes).forEach(CategoryContentPane::onSaveContentChanges);		
	}
	
	private Node layoutContent() {					
		final VBox optionCardLayout = new VBox(5);
		optionCardLayout.getChildren().addAll(categoryNameLabel, optionsView);
		optionCardLayout.setPadding(new Insets(0, 5, 0, 15));
		
		final SplitPane mainLayout = new SplitPane();        
		mainLayout.setOrientation(Orientation.HORIZONTAL);
		mainLayout.setDividerPosition(0, 0.20);
		mainLayout.getItems().addAll(optionCategoriesTreeView, optionCardLayout);
		
		mainLayout.setPrefSize(770, 500);
		categoryNameLabel.prefWidthProperty().bind(mainLayout.widthProperty());
        
        SplitPane.setResizableWithParent(optionCategoriesTreeView, Boolean.FALSE);
		return mainLayout;
	}
	
	private TreeItem<Node> buildOptionCategories() {		
		//General option names
		final String generalOptionName = "General";
		final String uiSettingsOptionName = categoryContentPanes[0].getName();
		final String directoriesOptionName = categoryContentPanes[1].getName();
		final String connectionOptionName = categoryContentPanes[2].getName();
		final String bandwidthOptionName = "Bandwidth";
		final String bitTorrentOptionName = categoryContentPanes[3].getName();
		final String transferCapOptionName = "Transfer Cap";
		final String queueingOptionName = categoryContentPanes[4].getName();
		final String schedulerOptionName = "Scheduler";
		final String remoteOptionName = "Remote";
		
		//Advanced option names
		final String advancedOptionName = "Advanced";
		final String uiExtrasOptionName = categoryContentPanes[5].getName();
		final String diskCacheOptionName = "Disk Cache";
		final String webUiOptionName = "Web UI";
		final String runProgramOptionName = "Run Program";
		
		//General option mappings
		optionGroupMappings.put(generalOptionName, buildGeneralOptionsView());
		optionGroupMappings.put(uiSettingsOptionName, categoryContentPanes[0].build());
		optionGroupMappings.put(directoriesOptionName, categoryContentPanes[1].build());
		optionGroupMappings.put(connectionOptionName, categoryContentPanes[2].build());
		optionGroupMappings.put(bandwidthOptionName, buildBandwidthOptionsView());
		optionGroupMappings.put(bitTorrentOptionName, categoryContentPanes[3].build());
		optionGroupMappings.put(transferCapOptionName, buildTransferCapOptionsView());
		optionGroupMappings.put(queueingOptionName, categoryContentPanes[4].build());
		optionGroupMappings.put(schedulerOptionName, buildSchedulerOptionsView());
		optionGroupMappings.put(remoteOptionName, buildRemoteOptionsView());
		
		//Advanced option mappings
		optionGroupMappings.put(advancedOptionName, buildAdvancedOptionsView());
		optionGroupMappings.put(uiExtrasOptionName, categoryContentPanes[5].build());
		optionGroupMappings.put(diskCacheOptionName, buildDiskCacheOptionsView());
		optionGroupMappings.put(webUiOptionName, buildWebUiOptionsView());
		optionGroupMappings.put(runProgramOptionName, buildRunProgramOptionsView());
		
		final List<TreeItem<Node>> generalOptionsNodes = Arrays.asList(generalOptionName, uiSettingsOptionName,
				directoriesOptionName, connectionOptionName, bandwidthOptionName, bitTorrentOptionName,
				transferCapOptionName, queueingOptionName, schedulerOptionName, remoteOptionName).stream()
				.map(name -> {					
					final Label nameLabel = new Label(name);
					nameLabel.getStyleClass().add(CssProperties.OPTION_CATEGORY_LIST_CELL);
					return new TreeItem<Node>(nameLabel);
				}).collect(Collectors.toList());
		
		final List<TreeItem<Node>> advancedOptionsElements = Arrays.asList(uiExtrasOptionName, 
				diskCacheOptionName, webUiOptionName, runProgramOptionName).stream()
				.map(name -> {
					final Label nameLabel = new Label(name);
					nameLabel.getStyleClass().add(CssProperties.OPTION_CATEGORY_LIST_CELL);
					return new TreeItem<Node>(nameLabel);
				}).collect(Collectors.toList());
				
		
		final Label advancedNameLabel = new Label(advancedOptionName);
		advancedNameLabel.getStyleClass().add(CssProperties.OPTION_CATEGORY_LIST__ROOT_CELL);
		
		final TreeItem<Node> advancedOptionsNodes = new TreeItem<Node>(advancedNameLabel);
		advancedOptionsNodes.setExpanded(false);
		advancedOptionsNodes.getChildren().addAll(advancedOptionsElements);
		
		final TreeItem<Node> rootNode = new TreeItem<>();		
		rootNode.setExpanded(true);
		rootNode.getChildren().addAll(generalOptionsNodes);
		rootNode.getChildren().add(advancedOptionsNodes);
		
		return rootNode;
	}
	
	private Pane buildGeneralOptionsView() {
		return new Pane(new Label("GENERAL"));
	}

	private Pane buildBandwidthOptionsView() {
		return new Pane(new Label("BANDWIDTH"));
	}
	
	private Pane buildTransferCapOptionsView() {
		return new Pane(new Label("TRANSFER CAP"));
	}

	private Pane buildSchedulerOptionsView() {
		return new Pane(new Label("SCHEDULER"));
	}
	
	private Pane buildRemoteOptionsView() {
		return new Pane(new Label("REMOTE"));
	}
	
	private Pane buildAdvancedOptionsView() {
		return new Pane(new Label("ADVANCED"));
	}
	
	private Pane buildDiskCacheOptionsView() {
		return new Pane(new Label("DISK CACHE"));
	}
	
	private Pane buildWebUiOptionsView() {
		return new Pane(new Label("WEB UI"));
	}
	
	private Pane buildRunProgramOptionsView() {
		return new Pane(new Label("RUN PROGRAM"));
	}
}