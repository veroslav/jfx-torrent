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
package org.matic.torrent.gui.window.preferences;

import org.matic.torrent.gui.action.enums.BorderStyle;
import org.matic.torrent.gui.action.enums.EncryptionMode;
import org.matic.torrent.gui.custom.TitledBorderPane;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.BitTorrentProperties;
import org.matic.torrent.preferences.GuiProperties;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class BitTorrentContentPane extends CategoryContentPane {
	
	private static final String BITTORRENT_CONTENT_PANE_NAME = "BitTorrent";
	
	//Basic BitTorrent options
	private final CheckBox enableLocalPeerDiscoveryCheck = new CheckBox("Enable Local Peer Discovery");
	private final CheckBox enableDhtNetworkCheck = new CheckBox("Enable DHT (decentralized) network");
	private final CheckBox enableDhtForNewTorrentsCheck = new CheckBox("Enable DHT for new torrents");
	private final CheckBox enablePexCheck = new CheckBox("Enable Peer Exchange (PeX)");	
	private final CheckBox enableWebSeedsCheck = new CheckBox("Enable Web Seeds");
	
	//Tracker options
	private final CheckBox reportedTrackerIpCheck = new CheckBox("IP/Hostname to report to trackers: ");
	private final CheckBox scrapeTrackerCheck = new CheckBox("Ask trackers for scrape information");
	private final CheckBox enableUdpTrackerCheck = new CheckBox("Enable UDP tracker support");	
	private final TextField reportedTrackerIpField = new TextField();
	
	//Protocol encryption options
	private final CheckBox allowLegacyConnectionsCheck = new CheckBox("Allow incoming legacy connections");
	private final CheckBox anonymousModeCheck = new CheckBox("Enable anonymous mode");	
	private final ComboBox<EncryptionMode> encryptionModeComboBox = new ComboBox<>();
	
	public BitTorrentContentPane(final BooleanProperty preferencesChanged) {
		super(BITTORRENT_CONTENT_PANE_NAME, preferencesChanged);
		initComponents(preferencesChanged);
	}
	
	@Override
	protected Node build() {
		return buildOptionsView();
	}

	@Override
	public void onSaveContentChanges() {
		if(preferencesChanged.get()) {
			//Save check box values
			ApplicationPreferences.setProperty(BitTorrentProperties.LOCAL_PEER_DISCOVERY_ENABLED,
					enableLocalPeerDiscoveryCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.DHT_NETWORK_ENABLED,
					enableDhtNetworkCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.DHT_NETWORK_NEW_TORRENTS_ENABLED,
					enableDhtForNewTorrentsCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.PEER_EXCHANGE_ENABLED,
					enablePexCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.WEB_SEEDS_ENABLED,
					enableWebSeedsCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.REPORT_TRACKER_IP_ENABLED,
					reportedTrackerIpCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.TRACKER_SCRAPE_ENABLED,
					scrapeTrackerCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.UDP_TRACKER_ENABLED,
					enableUdpTrackerCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.ALLOW_LEGACY_CONNECTIONS,
					allowLegacyConnectionsCheck.isSelected());
			ApplicationPreferences.setProperty(BitTorrentProperties.ANONYMOUS_MODE_ENABLED,
					anonymousModeCheck.isSelected());
			
			//Save encryption value
			ApplicationPreferences.setProperty(BitTorrentProperties.ENCRYPTION_MODE,
					encryptionModeComboBox.getSelectionModel().getSelectedItem().name());
			
			//Save tracker reported IP
			ApplicationPreferences.setProperty(BitTorrentProperties.TRACKER_IP,
					reportedTrackerIpField.getText().trim());
		}
	}
	
	private void initComponents(final BooleanProperty preferencesChanged) {
		encryptionModeComboBox.setItems(FXCollections.observableArrayList(EncryptionMode.values()));
		encryptionModeComboBox.setMaxWidth(Double.POSITIVE_INFINITY);
		
		setComboBoxActions(preferencesChanged);
		setCheckBoxActions(preferencesChanged);
		applyCheckBoxValues();
		
		reportedTrackerIpField.setDisable(!reportedTrackerIpCheck.isSelected());
		reportedTrackerIpField.setText(ApplicationPreferences.getProperty(BitTorrentProperties.TRACKER_IP, ""));
		reportedTrackerIpField.textProperty().addListener((obs, oldV, newV) -> preferencesChanged.set(true));
	}
	
	private void setComboBoxActions(final BooleanProperty preferencesChanged) {
		encryptionModeComboBox.setOnAction(e -> preferencesChanged.set(true));
		final EncryptionMode encryptionMode = 
				EncryptionMode.valueOf(ApplicationPreferences.getProperty(
				BitTorrentProperties.ENCRYPTION_MODE,
				EncryptionMode.DISABLED.name()));
		encryptionModeComboBox.getSelectionModel().select(encryptionMode);
	}
	
	private void setCheckBoxActions(final BooleanProperty preferencesChanged) {
		enableLocalPeerDiscoveryCheck.setOnAction(e -> preferencesChanged.set(true));
		enableDhtForNewTorrentsCheck.setOnAction(e -> preferencesChanged.set(true));
		allowLegacyConnectionsCheck.setOnAction(e -> preferencesChanged.set(true));
		enableUdpTrackerCheck.setOnAction(e -> preferencesChanged.set(true));
		enableDhtNetworkCheck.setOnAction(e -> preferencesChanged.set(true));
		enableWebSeedsCheck.setOnAction(e -> preferencesChanged.set(true));
		scrapeTrackerCheck.setOnAction(e -> preferencesChanged.set(true));
		anonymousModeCheck.setOnAction(e -> preferencesChanged.set(true));
		enablePexCheck.setOnAction(e -> preferencesChanged.set(true));		
		
		reportedTrackerIpCheck.setOnAction(e -> {
			reportedTrackerIpField.setDisable(!reportedTrackerIpCheck.isSelected());
			preferencesChanged.set(true);
		});
	}
	
	private void applyCheckBoxValues() {
		final boolean enableLocalPeerDiscoverySet = ApplicationPreferences.getProperty(
				BitTorrentProperties.LOCAL_PEER_DISCOVERY_ENABLED, BitTorrentProperties.DEFAULT_LOCAL_PEER_DISCOVERY_ENABLED);
		enableLocalPeerDiscoveryCheck.setSelected(enableLocalPeerDiscoverySet);
		final boolean enableDhtNetworkSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.DHT_NETWORK_ENABLED, BitTorrentProperties.DEFAULT_DHT_NETWORK_ENABLED);
		enableDhtNetworkCheck.setSelected(enableDhtNetworkSet);
		final boolean enableDhtNetworkNewTorrentsSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.DHT_NETWORK_NEW_TORRENTS_ENABLED, BitTorrentProperties.DEFAULT_DHT_NETWORK_NEW_TORRENTS_ENABLED);
		enableDhtForNewTorrentsCheck.setSelected(enableDhtNetworkNewTorrentsSet);
		final boolean enablePexSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.PEER_EXCHANGE_ENABLED, BitTorrentProperties.DEFAULT_PEER_EXCHANGE_ENABLED);
		enablePexCheck.setSelected(enablePexSet);
		final boolean enableWebSeedsSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.WEB_SEEDS_ENABLED, BitTorrentProperties.DEFAULT_WEB_SEEDS_ENABLED);
		enableWebSeedsCheck.setSelected(enableWebSeedsSet);
		final boolean enableReportTrackerIpSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.REPORT_TRACKER_IP_ENABLED, BitTorrentProperties.DEFAULT_REPORT_TRACKER_IP_ENABLED);
		reportedTrackerIpCheck.setSelected(enableReportTrackerIpSet);
		final boolean enableTrackerScrapeSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.TRACKER_SCRAPE_ENABLED, BitTorrentProperties.DEFAULT_TRACKER_SCRAPE_ENABLED);
		scrapeTrackerCheck.setSelected(enableTrackerScrapeSet);
		final boolean enableUdpTrackerSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.UDP_TRACKER_ENABLED, BitTorrentProperties.DEFAULT_UDP_TRACKER_ENABLED);
		enableUdpTrackerCheck.setSelected(enableUdpTrackerSet);
		final boolean allowLegacyConnectionSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.ALLOW_LEGACY_CONNECTIONS, BitTorrentProperties.DEFAULT_ALLOW_LEGACY_CONNECTIONS);
		allowLegacyConnectionsCheck.setSelected(allowLegacyConnectionSet);
		final boolean enableAnonimousModeSet = ApplicationPreferences.getProperty(
				BitTorrentProperties.ANONYMOUS_MODE_ENABLED, BitTorrentProperties.DEFAULT_ANONYMOUS_MODE_ENABLED);
		anonymousModeCheck.setSelected(enableAnonimousModeSet);
	}
	
	private Node buildOptionsView() {
		final TitledBorderPane basicOptions = new TitledBorderPane(
				"Basic BitTorrent Features", buildBasicOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		final TitledBorderPane trackerOptions = new TitledBorderPane(
				"Tracker Features", buildTrackerOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
		final TitledBorderPane encryptionOptions = new TitledBorderPane(
				"Protocol Encryption", buildEncryptionOptionsPane(), BorderStyle.COMPACT,
				TitledBorderPane.SECONDARY_BORDER_COLOR_STYLE);
						
		final VBox content = new VBox();
		content.getStyleClass().add(GuiProperties.VERTICAL_LAYOUT_SPACING);
		content.getChildren().addAll(basicOptions, trackerOptions, encryptionOptions);
		
		final ScrollPane contentScroll = new ScrollPane(content);
		contentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		contentScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		contentScroll.setFitToWidth(true);
		
		return contentScroll;
	}
	
	private Node buildBasicOptionsPane() {
		final ColumnConstraints firstColumn = new ColumnConstraints();
		final ColumnConstraints secondColumn = new ColumnConstraints();
	    
		firstColumn.setPercentWidth(50);	     
	    secondColumn.setPercentWidth(50);
	    
	    final GridPane basicOptionsPane = new GridPane();
	    basicOptionsPane.getColumnConstraints().addAll(firstColumn, secondColumn);
		basicOptionsPane.setVgap(10);
		
		basicOptionsPane.add(enableDhtNetworkCheck, 0, 0);
		basicOptionsPane.add(enableDhtForNewTorrentsCheck, 1, 0);
		basicOptionsPane.add(enablePexCheck, 0, 1);
		basicOptionsPane.add(enableLocalPeerDiscoveryCheck, 1, 1);
		basicOptionsPane.add(enableWebSeedsCheck, 0, 2, 2, 1);
		
		return basicOptionsPane;
	}
	
	private Node buildTrackerOptionsPane() {
		final ColumnConstraints firstColumn = new ColumnConstraints();
		final ColumnConstraints secondColumn = new ColumnConstraints();
	    
		firstColumn.setPercentWidth(50);	     
	    secondColumn.setPercentWidth(50);
	    
	    final GridPane basicOptionsPane = new GridPane();
	    basicOptionsPane.getColumnConstraints().addAll(firstColumn, secondColumn);
		basicOptionsPane.setVgap(10);
		
		basicOptionsPane.add(scrapeTrackerCheck, 0, 0);
		basicOptionsPane.add(enableUdpTrackerCheck, 1, 0);
		basicOptionsPane.add(reportedTrackerIpCheck, 0, 1);
		basicOptionsPane.add(reportedTrackerIpField, 1, 1);
		
		return basicOptionsPane;
	}
	
	private Node buildEncryptionOptionsPane() {
		final GridPane encryptionOptionsPane = buildGridPane();
		encryptionOptionsPane.add(anonymousModeCheck, 0, 0);
		encryptionOptionsPane.add(allowLegacyConnectionsCheck, 1, 0);
		encryptionOptionsPane.add(new Label("Outgoing encryption mode: "), 0, 1);
		encryptionOptionsPane.add(encryptionModeComboBox, 1, 1);
		
		return encryptionOptionsPane;
	}
}