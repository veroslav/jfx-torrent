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
package org.matic.torrent.gui.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.PeerView;
import org.matic.torrent.preferences.GuiProperties;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import org.matic.torrent.utils.UnitConverter;

public final class PeerTable {
	
	//Default columns
	private static final String PEER_DOWNLOAD_COLUMN_NAME = "Peer dl.";
	private static final String DOWN_SPEED_COLUMN_NAME = "Down Speed";
	private static final String DOWNLOADED_COLUMN_NAME = "Downloaded";	
	private static final String UP_SPEED_COLUMN_NAME = "Up Speed";
	private static final String UPLOADED_COLUMN_NAME = "Uploaded";
	private static final String PERCENT_DONE_COLUMN_NAME = "%";	
	private static final String CLIENT_COLUMN_NAME = "Client";
	private static final String REQUESTS_COLUMN_NAME = "Reqs";
	private static final String FLAGS_COLUMN_NAME = "Flags";
	private static final String IP_COLUMN_NAME = "IP";
	
	//Other columns
	private static final String PORT_COLUMN_NAME = "Port";

	//Flags
    private static final String CLIENT_NOT_INTERESTED_AND_NOT_CHOKED_FLAG = "K";
    private static final String PEER_UNCHOKED_AND_NOT_INTERESTED_FLAG = "?";
    private static final String CLIENT_INTERESTED_AND_NOT_CHOKED_FLAG = "D";
    private static final String CLIENT_INTERESTED_AND_CHOKED_FLAG = "d";
    private static final String PEER_UNCHOKED_AND_INTERESTED_FLAG = "U";
    private static final String PEER_CHOKED_AND_INTERESTED_FLAG = "u";

    private final MenuItem logTrafficToLoggerMenuItem = new MenuItem("_Log Traffic to Logger Tab");
	private final MenuItem copySelectedHostsMenuItem = new MenuItem("C_opy Selected Hosts");
	private final MenuItem reloadIpFilterMenuItem = new MenuItem("Reload _IPFilter");
	private final MenuItem copyPeerListMenuItem = new MenuItem("_Copy Peer List");	
	private final MenuItem addPeerMenuItem = new MenuItem("_Add Peer...");

	private final CheckMenuItem wholePeerListMenuItem = new CheckMenuItem("_Whole Peer List");
	private final CheckMenuItem resolveIpsMenuItem = new CheckMenuItem("_Resolve IPs");

	private final TableView<PeerView> peerTable = new TableView<>();
	
	public PeerTable() {
		initComponents();
		createColumns();
		createContextMenu();
	}
	
	public void setContent(final Collection<PeerView> peers) {
        Platform.runLater(() -> {
            peerTable.getItems().clear();
            peerTable.getItems().addAll(peers);
        });
	}

    /**
     * Sort the table based on the current sort order and latest table entry values.
     */
    public void sort() {
        final List<TableColumn<PeerView, ?>> sortOrder = new ArrayList<>(peerTable.getSortOrder());
        peerTable.getSortOrder().clear();
        peerTable.getSortOrder().addAll(sortOrder);
    }

	public void storeColumnStates() {
        TableUtils.storeColumnStates(peerTable.getColumns(), GuiProperties.PEER_TAB_COLUMN_VISIBILITY,
                GuiProperties.DEFAULT_PEER_TAB_COLUMN_VISIBILITIES, GuiProperties.PEER_TAB_COLUMN_SIZE,
                GuiProperties.DEFAULT_PEER_TAB_COLUMN_SIZES, GuiProperties.PEER_TAB_COLUMN_ORDER,
                GuiProperties.DEFAULT_PEER_TAB_COLUMN_ORDER);
    }

    public void wrapWith(final ScrollPane wrapper) {
        wrapper.setContent(peerTable);
    }
	
	private void initComponents() {
		peerTable.setPlaceholder(GuiUtils.getEmptyTablePlaceholder());
		peerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		peerTable.setTableMenuButtonVisible(false);
    }
	
	private void createColumns() {
		final LinkedHashMap<String, TableColumn<PeerView, ?>> columnMappings = buildColumnMappings();
		
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {
            final TableColumn<PeerView,?> tableColumn = columnMappings.get(columnId);
            peerTable.getColumns().add(tableColumn);
            peerTable.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());
        };
        final TableState<PeerView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
                GuiProperties.PEER_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_PEER_TAB_COLUMN_VISIBILITIES,
                GuiProperties.PEER_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_PEER_TAB_COLUMN_SIZES,
                GuiProperties.PEER_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_PEER_TAB_COLUMN_ORDER);

        TableUtils.addTableHeaderContextMenus(peerTable.getColumns(), columnState, columnResizer);
	}
	
	private void createContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();
		contextMenu.getItems().addAll(addPeerMenuItem, new SeparatorMenuItem(), copyPeerListMenuItem,
				copySelectedHostsMenuItem, logTrafficToLoggerMenuItem, reloadIpFilterMenuItem,
				new SeparatorMenuItem(), resolveIpsMenuItem, wholePeerListMenuItem);

        peerTable.setContextMenu(contextMenu);
        peerTable.setRowFactory(table -> {
            final TableRow<PeerView> tableRow = new PeerTableRow<>();
            return tableRow;
        });
	}
	
	private LinkedHashMap<String, TableColumn<PeerView, ?>> buildColumnMappings() {
		final LinkedHashMap<String, TableColumn<PeerView, ?>> columnMappings = new LinkedHashMap<>();
		
		final Callback<CellDataFeatures<PeerView, String>, ObservableValue<String>> ipValueFactory =
                p -> p.getValue().ipProperty();
                
        final Callback<CellDataFeatures<PeerView, String>, ObservableValue<String>> clientValueFactory =
                p -> p.getValue().clientIdProperty();
                
        final Callback<CellDataFeatures<PeerView, String>, ObservableValue<String>> flagsValueFactory =
                p -> p.getValue().flagsProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> percentDoneValueFactory =
                p -> p.getValue().percentDoneProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> downSpeedValueFactory =
                p -> p.getValue().downSpeedProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> upSpeedValueFactory =
                p -> p.getValue().upSpeedProperty();
                
        final Callback<CellDataFeatures<PeerView, String>, ObservableValue<String>> requestsValueFactory =
                p -> p.getValue().requestsProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> uploadedValueFactory =
                p -> p.getValue().uploadedProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> downloadedValueFactory =
                p -> p.getValue().downloadedProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> peerDownloadValueFactory =
                p -> p.getValue().peerDownloadProperty();
                
        final Callback<CellDataFeatures<PeerView, Number>, ObservableValue<Number>> portValueFactory =
                p -> new ReadOnlyObjectWrapper<>(p.getValue().getPort());
                
        final Function<PeerView, String> percentDoneValueConverter = p -> UnitConverter.formatDouble(p.getPercentDone());
        
        final Function<PeerView, String> downSpeedValueConverter = p -> String.valueOf(p.getDownSpeed());
        
        final Function<PeerView, String> upSpeedValueConverter = p -> String.valueOf(p.getUpSpeed());
        
        final Function<PeerView, String> uploadedValueConverter = p -> String.valueOf(p.getUploaded());
        
        final Function<PeerView, String> downloadedValueConverter = p -> String.valueOf(p.getDownloaded());
        
        final Function<PeerView, String> peerDownloadValueConverter = p -> String.valueOf(p.getPeerDownload());
        
        final Function<PeerView, String> portValueConverter = p -> String.valueOf(p.getPort());
		
		columnMappings.put(IP_COLUMN_NAME, TableUtils.buildColumn(ipValueFactory,
                PeerView::getIp, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, IP_COLUMN_NAME));
		
		columnMappings.put(PORT_COLUMN_NAME, TableUtils.buildColumn(portValueFactory,
                portValueConverter, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PORT_COLUMN_NAME));
		
		columnMappings.put(CLIENT_COLUMN_NAME, TableUtils.buildColumn(clientValueFactory,
				PeerView::getClientName, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, CLIENT_COLUMN_NAME));
		
		columnMappings.put(FLAGS_COLUMN_NAME, TableUtils.buildColumn(flagsValueFactory,
				peerView -> {
		            final StringBuilder flagsBuilder = new StringBuilder();

		            if(!peerView.isInterestedInUs() && !peerView.areWeChoking()) {
		                flagsBuilder.append(PEER_UNCHOKED_AND_NOT_INTERESTED_FLAG);
                    }
                    if(peerView.areWeInterestedIn()) {
                        flagsBuilder.append(peerView.isChokingUs()? CLIENT_INTERESTED_AND_CHOKED_FLAG
                                : CLIENT_INTERESTED_AND_NOT_CHOKED_FLAG);
                    }
                    if(!peerView.isChokingUs() && !peerView.areWeInterestedIn()) {
                        flagsBuilder.append(CLIENT_NOT_INTERESTED_AND_NOT_CHOKED_FLAG);
                    }
                    if(peerView.isInterestedInUs()) {
                        flagsBuilder.append(peerView.areWeChoking()? PEER_CHOKED_AND_INTERESTED_FLAG
                                : PEER_UNCHOKED_AND_INTERESTED_FLAG);
                    }

		            return flagsBuilder.toString();
                }, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, FLAGS_COLUMN_NAME));
		
		columnMappings.put(PERCENT_DONE_COLUMN_NAME, TableUtils.buildColumn(percentDoneValueFactory,
				percentDoneValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PERCENT_DONE_COLUMN_NAME));
		
		columnMappings.put(DOWN_SPEED_COLUMN_NAME, TableUtils.buildColumn(downSpeedValueFactory,
				downSpeedValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, DOWN_SPEED_COLUMN_NAME));
		
		columnMappings.put(UP_SPEED_COLUMN_NAME, TableUtils.buildColumn(upSpeedValueFactory,
				upSpeedValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, UP_SPEED_COLUMN_NAME));
		
		columnMappings.put(REQUESTS_COLUMN_NAME, TableUtils.buildColumn(requestsValueFactory,
                PeerView::getRequests, GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, REQUESTS_COLUMN_NAME));
		
		columnMappings.put(UPLOADED_COLUMN_NAME, TableUtils.buildColumn(uploadedValueFactory,
				uploadedValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, UPLOADED_COLUMN_NAME));
		
		columnMappings.put(DOWNLOADED_COLUMN_NAME, TableUtils.buildColumn(downloadedValueFactory,
				downloadedValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, DOWNLOADED_COLUMN_NAME));
		
		columnMappings.put(PEER_DOWNLOAD_COLUMN_NAME, TableUtils.buildColumn(peerDownloadValueFactory,
				peerDownloadValueConverter, GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PEER_DOWNLOAD_COLUMN_NAME));
		
		return columnMappings;
	}
}