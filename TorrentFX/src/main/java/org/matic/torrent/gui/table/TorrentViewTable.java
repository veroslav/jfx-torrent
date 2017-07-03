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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.matic.torrent.gui.GuiUtils;
import org.matic.torrent.gui.model.TorrentView;
import org.matic.torrent.hash.InfoHash;
import org.matic.torrent.preferences.CssProperties;
import org.matic.torrent.preferences.GuiProperties;
import org.matic.torrent.queue.QueuedTorrentManager;
import org.matic.torrent.queue.enums.PriorityChange;
import org.matic.torrent.queue.enums.QueueType;
import org.matic.torrent.queue.enums.TorrentStatus;
import org.matic.torrent.utils.UnitConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This is a graphical view (represented as a table) of current torrent jobs
 * 
 * @author vedran
 *
 */
public final class TorrentViewTable {

    public static final String DOWNLOADING_FILTER = "Downloading";
    public static final String COMPLETED_FILTER = "Completed";
    public static final String INACTIVE_FILTER = "Inactive";
    public static final String TORRENTS_FILTER = "Torrents";
    public static final String NO_LABEL_FILTER = "No Label";
    public static final String SEEDING_FILTER = "Seeding";
    public static final String LABELS_FILTER = "Labels";
    public static final String ACTIVE_FILTER = "Active";
    public static final String FEEDS_FILTER = "Feeds";

	private static final String PRIORITY_COLUMN_LABEL = "#";
	private static final String NAME_COLUMN_LABEL = "Name";
	private static final String SIZE_COLUMN_LABEL = "Size";
	private static final String SELECTED_SIZE_COLUMN_LABEL = "Selected Size";
	private static final String ADDED_COLUMN_LABEL = "Added";
	private static final String TRACKER_COLUMN_LABEL = "Tracker";

    //Context menu items
    private final MenuItem openContainingFolderMenuItem = new MenuItem("Open _Containing Folder");
    private final MenuItem openUrlInBrowserMenuItem = new MenuItem("Open URL in Browser");
    private final MenuItem moveDownQueueMenuItem = new MenuItem("Move _Down Queue");
    private final MenuItem updateTrackerMenuItem = new MenuItem("Update Trac_ker");
    private final MenuItem copyMagnetUriMenuItem = new MenuItem("Copy Magnet URI");
    private final MenuItem forceRecheckMenuItem = new MenuItem("Force Re-Check");
    private final MenuItem moveUpQueueMenuItem = new MenuItem("Move _Up Queue");
    private final MenuItem forceStartMenuItem = new MenuItem("_Force Start");
    private final MenuItem propertiesMenuItem = new MenuItem("Prop_erties");
    private final MenuItem newLabelMenuItem = new MenuItem("New Label...");
    private final MenuItem removeMenuItem = new MenuItem("_Remove");
    private final MenuItem startMenuItem = new MenuItem("_Start");
    private final MenuItem pauseMenuItem = new MenuItem("_Pause");
    private final MenuItem stopMenuItem = new MenuItem("S_top");
    private final MenuItem openMenuItem = new MenuItem("_Open");

    private final MenuItem setDownloadLocationMenuItem = new MenuItem("Set Download Location...");
    private final MenuItem setDestinationNameMenuItem = new MenuItem("Set Destination Name...");
    private final MenuItem updateTorrentMenuItem = new MenuItem("Update Torrent...");
    private final MenuItem clearPeerListMenuItem = new MenuItem("Clear Peer List");
    private final MenuItem resetBansMenuItem = new MenuItem("Reset Bans");

    private final MenuItem deleteTorrentAndDataMenuItem = new MenuItem("Delete .torrent + Data");
    private final MenuItem deleteTorrentMenuItem = new MenuItem("Delete .torrent");
    private final MenuItem deleteDataMenuItem = new MenuItem("Delete Data");

    private final RadioMenuItem normalMenuItem = new RadioMenuItem("Normal");
    private final RadioMenuItem highMenuItem = new RadioMenuItem("High");
    private final RadioMenuItem lowMenuItem = new RadioMenuItem("Low");

	private final TableView<TorrentView> torrentTable = new TableView<>();
    private final ObservableList<TorrentView> torrentViews = FXCollections.observableArrayList();
    private final FilteredList<TorrentView> filteredTorrents =
            new FilteredList<>(torrentViews, p -> true);

    private final QueuedTorrentManager torrentManager;

    private final IntegerProperty inactiveTorrents = new SimpleIntegerProperty(0);
    private final IntegerProperty activeTorrents = new SimpleIntegerProperty(0);
    private final IntegerProperty totalTorrents = new SimpleIntegerProperty(0);
	
	public TorrentViewTable(final QueuedTorrentManager torrentManager) {
        this.torrentManager = torrentManager;

		initComponents();
        createContextMenu();
	}

    public IntegerProperty totalTorrentsProperty() {
        return totalTorrents;
    }

    public IntegerProperty activeTorrentsProperty() {
        return activeTorrents;
    }

    public IntegerProperty inactiveTorrentsProperty() {
        return inactiveTorrents;
    }

    public void filter(final String filterName) {
        filteredTorrents.setPredicate(tv -> {
            final QueueType queueType = tv.getQueueType();
            final TorrentStatus torrentStatus = tv.getStatus();

            switch(filterName) {
                case ACTIVE_FILTER:
                    return torrentStatus == TorrentStatus.ACTIVE &&
                            (queueType == QueueType.ACTIVE || queueType == QueueType.FORCED);
                case INACTIVE_FILTER:
                    return queueType == QueueType.INACTIVE || queueType == QueueType.QUEUED;
                default:
                    return true;
            }
        });
    }
	
	public void addSelectionListener(final Consumer<TorrentView> handler) {
		torrentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
			handler.accept(newV));
	}
	
	public Optional<TorrentView> find(final InfoHash infoHash) {
		return torrentTable.getItems().stream().filter(
                tv -> tv.getInfoHash().equals(infoHash)).findFirst();
	}

    public void refresh() {
        TableUtils.refresh(torrentTable);
    }

    /**
     * Sort the table based on the current sort order and latest table entry values
     */
    public void sort() {
        final List<TableColumn<TorrentView, ?>> sortOrder = new ArrayList<>(torrentTable.getSortOrder());
        torrentTable.getSortOrder().clear();
        torrentTable.getSortOrder().addAll(sortOrder);
    }
	
	/**
	 * Create a binding that updates the target when this table becomes empty 
	 * 
	 * @return Generated boolean binding
	 */
	public BooleanBinding bindOnEmpty() {
		return Bindings.size(torrentTable.getItems()).isEqualTo(0);
	}
	
	public void addJob(final TorrentView torrentJob) {
        torrentViews.add(torrentJob);
		torrentTable.getSelectionModel().clearSelection();
		torrentTable.getSelectionModel().select(torrentJob);

        updateTorrentStatusStatistics(QueueType.NOT_ON_QUEUE, torrentJob.getQueueType());
        torrentJob.addQueueStatusChangeListener((obs, oldV, newV) ->
            updateTorrentStatusStatistics(oldV, newV));
        refresh();
	}

    private void updateTorrentStatusStatistics(final QueueType oldQueueType, final QueueType newQueueType) {
        switch(newQueueType) {
            case ACTIVE:
                activeTorrents.set(activeTorrents.intValue() + 1);
                break;
            case INACTIVE:
            case QUEUED:
                inactiveTorrents.set(inactiveTorrents.intValue() + 1);
                break;
        }
        switch(oldQueueType) {
            case ACTIVE:
                activeTorrents.set(activeTorrents.intValue() - 1);
                break;
            case INACTIVE:
            case QUEUED:
                inactiveTorrents.set(inactiveTorrents.intValue() - 1);
                break;
        }
    }
	
	public void deleteJobs(final ObservableList<TorrentView> torrentJobs) {
        //A workaround for bug JDK-8087508
        torrentTable.sort();

        torrentViews.removeAll(torrentJobs);
	}
	
	public ObservableList<TorrentView> getSelectedJobs() {
		return torrentTable.getSelectionModel().getSelectedItems();
	}
	
	public void selectJob(final TorrentView torrentJob) {
        torrentTable.getSelectionModel().clearSelection();
		torrentTable.getSelectionModel().select(torrentJob);
	}
	
	public void wrapWith(final ScrollPane wrapper) {
		wrapper.setContent(torrentTable);
	}
	
	/**
	 * Store any changes to column order, visibility, and/or size
	 */
	public void storeColumnStates() {		
		TableUtils.storeColumnStates(torrentTable.getColumns(), GuiProperties.TORRENT_JOBS_TAB_COLUMN_VISIBILITY,
			GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES, GuiProperties.TORRENT_JOBS_TAB_COLUMN_SIZE,
			GuiProperties.DEFAULT_TORRENT_JOBS_COLUMN_SIZES, GuiProperties.TORRENT_JOBS_TAB_COLUMN_ORDER,
			GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER);		
	}

	private void initComponents() {
	    torrentViews.addListener((ListChangeListener<TorrentView>) l -> {
	        if(l.next()) {
                totalTorrents.set(torrentViews.size());
            }
        });

        final SortedList<TorrentView> sortedTorrents = new SortedList<>(filteredTorrents);
        sortedTorrents.comparatorProperty().bind(torrentTable.comparatorProperty());

        torrentTable.setItems(sortedTorrents);
		torrentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		torrentTable.setTableMenuButtonVisible(false);
		torrentTable.setRowFactory(t -> new TorrentViewTableRow<>());
		
		final Text emptyTorrentListPlaceholder = new Text("Go to 'File->Add Torrent...' to add torrents.");
		emptyTorrentListPlaceholder.getStyleClass().add(CssProperties.TORRENT_LIST_EMPTY_TEXT);
		emptyTorrentListPlaceholder.visibleProperty().bind(Bindings.isEmpty(torrentTable.getItems()));
		
		final BorderPane placeholderPane = new BorderPane();
		placeholderPane.getStyleClass().add(CssProperties.PLACEHOLDER_EMPTY);
		placeholderPane.setPadding(new Insets(15, 0, 0, 40));
		placeholderPane.setLeft(emptyTorrentListPlaceholder);
		
		torrentTable.setPlaceholder(placeholderPane);
		createColumns();		
	}

    private void createContextMenu() {
        final Menu labelsMenu = new Menu("_Labels");
        labelsMenu.getItems().addAll(newLabelMenuItem);

        final Menu setDownloadLimitMenu = new Menu("Set Download Limit");
        final Menu setUploadLimitMenu = new Menu("Set Upload Limit");

        final ToggleGroup bandwidthAllocationGroup = new ToggleGroup();
        highMenuItem.setToggleGroup(bandwidthAllocationGroup);
        normalMenuItem.setToggleGroup(bandwidthAllocationGroup);
        lowMenuItem.setToggleGroup(bandwidthAllocationGroup);
        bandwidthAllocationGroup.selectToggle(normalMenuItem);

        final Menu bandwidthAllocationMenu = new Menu("Band_width Allocation");
        bandwidthAllocationMenu.getItems().addAll(highMenuItem, normalMenuItem, lowMenuItem,
                new SeparatorMenuItem(), setDownloadLimitMenu, setUploadLimitMenu);

        final Menu removeAndMenu = new Menu("Remove A_nd");
        removeAndMenu.getItems().addAll(deleteTorrentMenuItem, deleteTorrentAndDataMenuItem, deleteDataMenuItem);

        final Menu advancedMenu = new Menu("_Advanced");
        advancedMenu.getItems().addAll(resetBansMenuItem, clearPeerListMenuItem, setDownloadLocationMenuItem,
                setDestinationNameMenuItem, updateTorrentMenuItem);

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(openMenuItem, openContainingFolderMenuItem, new SeparatorMenuItem(),
                copyMagnetUriMenuItem, openUrlInBrowserMenuItem, new SeparatorMenuItem(),
                forceStartMenuItem, startMenuItem, pauseMenuItem, stopMenuItem, new SeparatorMenuItem(),
                moveUpQueueMenuItem, moveDownQueueMenuItem, labelsMenu, new SeparatorMenuItem(),
                bandwidthAllocationMenu, new SeparatorMenuItem(), removeMenuItem, removeAndMenu,
                new SeparatorMenuItem(), forceRecheckMenuItem, advancedMenu, new SeparatorMenuItem(),
                updateTrackerMenuItem, new SeparatorMenuItem(), propertiesMenuItem);
        contextMenu.showingProperty().addListener(obs -> {});
        torrentTable.setRowFactory(table -> {
            final TableRow<TorrentView> tableRow = new TrackerTableRow<>();
            tableRow.setContextMenu(contextMenu);
            tableRow.setOnContextMenuRequested(cme -> {
                final TorrentView torrentView = tableRow.getItem();
                if(torrentView == null) {
                    contextMenu.hide();
                    return;
                }
                setupMenuStates(torrentView);
                setupMenuActions(torrentView);
            });

            return tableRow;
        });
    }

    private void setupMenuStates(final TorrentView torrentView) {
        final QueueType queueType = torrentView.getQueueType();
        final TorrentStatus torrentStatus = torrentView.getStatus();

        startMenuItem.setDisable(torrentStatus == TorrentStatus.ACTIVE ||
                queueType == QueueType.QUEUED || queueType == QueueType.FORCED);
        stopMenuItem.setDisable(torrentStatus == TorrentStatus.STOPPED);
        pauseMenuItem.setDisable(torrentStatus == TorrentStatus.STOPPED);
        forceStartMenuItem.setDisable(queueType == QueueType.FORCED);

        final int priority = torrentView.getPriority();

        moveUpQueueMenuItem.setDisable(priority == 1);
        moveDownQueueMenuItem.setDisable(priority == torrentTable.getItems().size());
    }

    private void setupMenuActions(final TorrentView torrentView) {
        forceStartMenuItem.setOnAction(e ->
                torrentManager.requestTorrentPriorityChange(torrentView, PriorityChange.FORCED));
        startMenuItem.setOnAction(e ->
                torrentManager.requestTorrentStatusChange(torrentView, TorrentStatus.ACTIVE));
        pauseMenuItem.setOnAction(e ->
                torrentManager.requestTorrentStatusChange(torrentView, TorrentStatus.PAUSED));
        stopMenuItem.setOnAction(e ->
                torrentManager.requestTorrentStatusChange(torrentView, TorrentStatus.STOPPED));

        moveUpQueueMenuItem.setOnAction(e ->
                torrentManager.requestTorrentPriorityChange(torrentView, PriorityChange.HIGHER));
        moveDownQueueMenuItem.setOnAction(e ->
                torrentManager.requestTorrentPriorityChange(torrentView, PriorityChange.LOWER));
    }
	
	private void createColumns() {
		final LinkedHashMap<String, TableColumn<TorrentView, ?>> columnMappings = buildColumnMappings();
		final BiConsumer<String, Double> columnResizer = (columnId, targetWidth) -> {
			final TableColumn<TorrentView,?> tableColumn = columnMappings.get(columnId);
			torrentTable.getColumns().add(tableColumn);
			torrentTable.resizeColumn(tableColumn, targetWidth - tableColumn.getWidth());
		};
		final TableState<TorrentView> columnState = TableUtils.loadColumnStates(columnMappings, columnResizer,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_VISIBILITY, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_VISIBILITIES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_SIZE, GuiProperties.DEFAULT_TORRENT_JOBS_COLUMN_SIZES,
				GuiProperties.TORRENT_JOBS_TAB_COLUMN_ORDER, GuiProperties.DEFAULT_TORRENT_JOBS_TAB_COLUMN_ORDER);
		
		TableUtils.addTableHeaderContextMenus(torrentTable.getColumns(), columnState, columnResizer);
	}
		
	private LinkedHashMap<String, TableColumn<TorrentView, ?>> buildColumnMappings() {
		final Callback<CellDataFeatures<TorrentView, String>, ObservableValue<String>> priorityValueFactory =
				tj -> tj.getValue().lifeCycleChangeProperty();
		final Callback<CellDataFeatures<TorrentView, String>, ObservableValue<String>> nameValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getFileName());
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> sizeValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getTotalLength());
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> selectedSizeValueFactory =
				tj -> tj.getValue().selectedLengthProperty();
		final Callback<CellDataFeatures<TorrentView, Number>, ObservableValue<Number>> addedValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getAddedOnTime());
		final Callback<CellDataFeatures<TorrentView, String>, ObservableValue<String>> trackerValueFactory =
				tj -> new ReadOnlyObjectWrapper<>(tj.getValue().getTrackerUrl());

        final TableColumn<TorrentView, ?> priorityColumn = TableUtils.buildColumn(priorityValueFactory,
                val -> val.getLifeCycleChange(),
                GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, PRIORITY_COLUMN_LABEL);
        torrentTable.getSortOrder().add(priorityColumn);

		final LinkedHashMap<String, TableColumn<TorrentView, ?>> columnMappings = new LinkedHashMap<>();
		columnMappings.put(PRIORITY_COLUMN_LABEL, priorityColumn);
		columnMappings.put(NAME_COLUMN_LABEL, TableUtils.buildColumn(nameValueFactory, tj -> tj.getFileName(),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, NAME_COLUMN_LABEL));
		columnMappings.put(SIZE_COLUMN_LABEL, TableUtils.buildColumn(sizeValueFactory, tj -> 
			UnitConverter.formatByteCount(tj.getTotalLength()),
				GuiUtils.RIGHT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SIZE_COLUMN_LABEL));
		columnMappings.put(SELECTED_SIZE_COLUMN_LABEL, TableUtils.buildColumn(selectedSizeValueFactory, tj -> 
			UnitConverter.formatByteCount(tj.getSelectedLength()),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, SELECTED_SIZE_COLUMN_LABEL));
		columnMappings.put(ADDED_COLUMN_LABEL, TableUtils.buildColumn(addedValueFactory, tj -> 
			UnitConverter.formatMillisToDate(tj.getAddedOnTime(), TimeZone.getDefault()),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, ADDED_COLUMN_LABEL));
		columnMappings.put(TRACKER_COLUMN_LABEL, TableUtils.buildColumn(trackerValueFactory, tj -> tj.getTrackerUrl(),
				GuiUtils.LEFT_ALIGNED_COLUMN_HEADER_TYPE_NAME, TRACKER_COLUMN_LABEL));
		
		return columnMappings;
	}
}