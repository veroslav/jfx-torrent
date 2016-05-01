/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
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

package org.matic.torrent.gui.window;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.hash.HashUtilities;
import org.matic.torrent.net.NetworkUtilities;

import javax.net.ssl.SSLException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A window offering the user possibility to retrieve a
 * remote resource (URL, magnet link or info hash)
 *
 * @author vedran
 *
 */
public final class UrlLoaderWindow {

	public enum ResourceType {
		URL, MAGNET_LINK, INFO_HASH, UNKNOWN
	}

	private static final String WINDOW_TITLE = "Add Torrent from URL";
	private static final String EMPTY_CONTENT = "";

	private static final String VALID_MAGNET_LINK_PREFIX = "magnet";

	private ResourceType resourceType = ResourceType.UNKNOWN;
	private BinaryEncodedDictionary torrentMap = null;

	private final TextField urlEntryField = new TextField();
	private final ProgressBar progressBar = new ProgressBar(0);
	private final Label progressStatus = new Label();

	private final Dialog<ButtonType> window = new Dialog<>();

	public UrlLoaderWindow(final Window owner) {
		window.initOwner(owner);
		initComponents();
	}

	public final UrlLoaderWindowOptions showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();

		if(result.isPresent() && resourceType != ResourceType.UNKNOWN && torrentMap != null) {
			return new UrlLoaderWindowOptions(resourceType, urlEntryField.getText(), torrentMap);
		}
		return null;
	}

	private void initComponents() {
		window.setHeaderText(null);
		window.setTitle(WINDOW_TITLE);
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		final Button downloadButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		downloadButton.addEventFilter(ActionEvent.ACTION, event -> onDownloadButtonClicked(event));
		urlEntryField.textProperty().addListener((obs, oldV, newV) -> downloadButton.setDisable(newV.isEmpty()));

		final String clipBoardContents = getClipboardContents();
		if((resourceType = determineUrlResourceType(clipBoardContents)) != ResourceType.UNKNOWN) {
			urlEntryField.setText(clipBoardContents);
		}

		final boolean hasValidClipboardContents = determineUrlResourceType(clipBoardContents) != ResourceType.UNKNOWN;
		downloadButton.setDisable(!hasValidClipboardContents);

		if(hasValidClipboardContents) {
			urlEntryField.setText(clipBoardContents);
		}

		window.setResizable(true);
		window.getDialogPane().setContent(layoutContent());
	}

	private void onDownloadButtonClicked(final Event event) {
		final String url = urlEntryField.getText();
		final ResourceType resourceType = determineUrlResourceType(url);
		if(resourceType == ResourceType.UNKNOWN) {
			showErrorMessage("Invalid path entered.");
			return;
		}
		if(resourceType == ResourceType.URL) {
            event.consume();
			downloadUrlContents(url);
		}
		else {
			/* We will leave loading of info hashes and magnet links to the caller
			   because it could take a while and as we can't show any progress anyway */
			window.setResult(ButtonType.OK);
		}
	}

	private void downloadUrlContents(final String url) {
		setGuiEnabled(false);
		progressBar.setProgress(0);
		progressStatus.setText(EMPTY_CONTENT);

		final UrlLoaderTask urlLoaderTask = new UrlLoaderTask(url);

		final Button cancelButton = (Button)window.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.addEventFilter(ActionEvent.ACTION, event -> {
			window.setResult(ButtonType.CANCEL);
			resourceType = ResourceType.UNKNOWN;
			urlLoaderTask.cancel();
		});

		final ExecutorService taskExecutor = Executors.newSingleThreadExecutor(r -> {
			final Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		});

		window.setOnCloseRequest(handler -> urlLoaderTask.cancel());

		urlLoaderTask.setOnSucceeded(handler -> {
			resetState(taskExecutor);
			torrentMap = (BinaryEncodedDictionary)handler.getSource().getValue();
			window.close();
		});
		urlLoaderTask.setOnCancelled(handler -> {
			resetState(taskExecutor);
			if(window.getResult() != ButtonType.OK) {
				resourceType = ResourceType.UNKNOWN;
			}
			window.close();
		});
		urlLoaderTask.setOnFailed(handler -> {
			resetState(taskExecutor);
			handleFailed(handler.getSource().getException(), url);
		});
		progressStatus.textProperty().bind(urlLoaderTask.messageProperty());
		progressBar.progressProperty().bind(urlLoaderTask.progressProperty());

		taskExecutor.execute(urlLoaderTask);
	}

	private void resetState(final ExecutorService executor) {
		progressBar.progressProperty().unbind();
		progressStatus.textProperty().unbind();
		executor.shutdownNow();
		setGuiEnabled(true);
	}

	private void setGuiEnabled(final boolean enabled) {
		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		final Button cancelButton = (Button)window.getDialogPane().lookupButton(ButtonType.CANCEL);

		okButton.setDisable(!enabled);
		cancelButton.setDisable(enabled);
	}

	private void handleFailed(final Throwable throwable, final String url) {
		throwable.printStackTrace();
		if(throwable instanceof SSLException) {
			final X509Certificate[] certificates = NetworkUtilities.getInterceptedCertificates();
			final boolean acceptCertificateChain = confirmCertificateChainAddition(certificates);
			if(acceptCertificateChain) {
				try {
					NetworkUtilities.addTrustedCertificate(url, certificates);
				}
				catch(final Exception e) {
					e.printStackTrace();
					resourceType = ResourceType.UNKNOWN;
					window.close();

					showAlert(AlertType.ERROR, "The certificate could not be added.",
							"Add certificate to trust store");
					return;
				}
				downloadUrlContents(url);
			}
			else {
				resourceType = ResourceType.UNKNOWN;
				window.close();
			}
		}
		else {
			resourceType = ResourceType.UNKNOWN;
			window.close();

			showAlert(AlertType.ERROR, "Invalid torrent contents.", "Invalid torrent");
		}
	}

	private boolean confirmCertificateChainAddition(final X509Certificate[] certificateChain) {
		final StringBuilder certificateSummary = new StringBuilder();
		for(final X509Certificate certificate : certificateChain) {
			certificateSummary.append(certificate.toString());
		}

		final StringBuilder userQuestion = new StringBuilder();
		userQuestion.append("The remote server is using a self signed certificate.\n");
		userQuestion.append("Would you like to add it to the trusted certificate list?");

		final Alert acceptAlert = new Alert(AlertType.WARNING, userQuestion.toString(),
				ButtonType.OK, ButtonType.CANCEL);
		acceptAlert.setHeaderText("Potential Security Risk");
		acceptAlert.setTitle("Accept self signed server certificate");
		acceptAlert.initOwner(window.getOwner());
		acceptAlert.setResizable(true);
		acceptAlert.setWidth(1000);
		acceptAlert.setHeight(400);

		final Label detailsLabel = new Label("Certificate details:");

		final TextArea detailsTextArea = new TextArea(certificateSummary.toString());
		detailsTextArea.setEditable(false);
		detailsTextArea.setWrapText(true);
		detailsTextArea.setMaxWidth(Double.MAX_VALUE);
		detailsTextArea.setMaxHeight(Double.MAX_VALUE);

		GridPane.setVgrow(detailsTextArea, Priority.ALWAYS);
		GridPane.setHgrow(detailsTextArea, Priority.ALWAYS);

		final GridPane expandableContent = new GridPane();
		expandableContent.setMaxWidth(Double.MAX_VALUE);
		expandableContent.add(detailsLabel, 0, 0);
		expandableContent.add(detailsTextArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		acceptAlert.getDialogPane().setExpandableContent(expandableContent);

		final Optional<ButtonType> result = acceptAlert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	private Node layoutContent() {
		final VBox mainLayout = new VBox(10);
		progressBar.prefWidthProperty().bind(mainLayout.widthProperty());

		mainLayout.getChildren().addAll(
				new Label("Please enter the location of the torrent you want to " +
						"open (URL, magnet link or info hash):"),
				buildUrlEntryContent(), progressStatus, progressBar);

		return mainLayout;
	}

	private Node buildUrlEntryContent() {
		final HBox entryLayout = new HBox();

		entryLayout.getChildren().addAll(new Label("Path: "), urlEntryField);
		entryLayout.setAlignment(Pos.CENTER);
		HBox.setHgrow(urlEntryField, Priority.ALWAYS);

		return entryLayout;
	}

	private void showAlert(final AlertType alertType, final String message, final String title) {
		final Alert alert = new Alert(alertType, message, ButtonType.OK);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.initOwner(window.getOwner());
		alert.setResizable(true);
		alert.setWidth(1000);
		alert.setHeight(400);
		alert.show();
	}

	private String getClipboardContents() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		return clipboard.hasString()? clipboard.getString() : EMPTY_CONTENT;
	}

	private void showErrorMessage(final String message) {
		final Alert invalidUrlAlert = new Alert(AlertType.ERROR);
		invalidUrlAlert.initOwner(window.getOwner());
		invalidUrlAlert.setContentText(message);
		invalidUrlAlert.setTitle(WINDOW_TITLE);
		invalidUrlAlert.setHeaderText(null);
		invalidUrlAlert.showAndWait();
	}

	private ResourceType determineUrlResourceType(final String urlPath) {
		//Check whether we have a valid info hash, URL or a magnet link
		if(urlPath.length() == HashUtilities.HEX_INFO_HASH_LENGTH &&
				HashUtilities.isValidHexNumber(urlPath)) {
			return ResourceType.INFO_HASH;
		}
		try {
			new URL(urlPath);
			return ResourceType.URL;
		}
		catch(final MalformedURLException mue) {
			try {
				final URI validUri = new URI(urlPath);
				if(validUri.getScheme() != null) {
					final String uriScheme = validUri.getScheme().toLowerCase();

					if(uriScheme.startsWith(VALID_MAGNET_LINK_PREFIX)) {
						return ResourceType.MAGNET_LINK;
					}
				}
				return ResourceType.UNKNOWN;
			}
			catch(final URISyntaxException use) {
				return ResourceType.UNKNOWN;
			}
		}
	}
}