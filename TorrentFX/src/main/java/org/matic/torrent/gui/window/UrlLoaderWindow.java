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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.utils.HashUtilities;
import org.matic.torrent.utils.UnitConverter;

/**
 * A window offering the user possibility to retrieve a 
 * remote resource (URL, magnet link or info hash)
 * 
 * @author vedran
 *
 */
public final class UrlLoaderWindow {
	
	protected enum ResourceType {
		URL, MAGNET_LINK, INFO_HASH, INVALID
	}
	
	private static final int BUFFER_SIZE = 1024;
	
	private static final String WINDOW_TITLE = "Add Torrent from URL";
	private static final String EMPTY_CLIPBOARD_CONTENT = "";
	
	private static final String VALID_MAGNET_LINK_PREFIX = "magnet";
		
	private ResourceType resourceType = ResourceType.INVALID;
	private BinaryEncodedDictionary torrentMap = null;
	
	private final UrlDownloaderService urlDownloaderService;
	
	private final TextField urlEntryField;	
	private final ProgressBar progressBar;
	private final Label progressLabel;
	
	private final Dialog<ButtonType> window;

	public UrlLoaderWindow(final Window owner) {
		window = new Dialog<>();
		window.initOwner(owner);
		
		urlEntryField = new TextField();
		progressBar = new ProgressBar(0);
		progressLabel = new Label();
		
		urlDownloaderService = new UrlDownloaderService();
		
		initComponents();
	}
	
	public final UrlLoaderWindowOptions showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();
		if(result.isPresent()) {
			System.out.println("Result available");
			return new UrlLoaderWindowOptions(resourceType, urlEntryField.getText(), torrentMap);
		}
		System.out.println("Cancelled by user");
		return null;
	}
	
	private void initComponents() {
		final String clipBoardContents = getClipboardContents();
		if(validateUrlResource(clipBoardContents) != ResourceType.INVALID) {
			urlEntryField.setText(clipBoardContents);
		}
		
		urlDownloaderService.setOnSucceeded(handler -> {
			progressBar.progressProperty().unbind();
			System.out.println("Download SUCCESS");
			torrentMap = (BinaryEncodedDictionary)handler.getSource().getValue();
			window.close();
		});
		urlDownloaderService.setOnCancelled(handler -> {
			System.out.println("onCancelled()");
			resourceType = ResourceType.INVALID;
			//window.close();
		});
				
		urlDownloaderService.messageProperty().addListener((obs, oldV, newV) -> progressLabel.setText(newV));		
		progressBar.progressProperty().addListener((obs, oldV, newV) -> 
			progressLabel.setText("Downloading torrent (" + (int)(newV.doubleValue() * 100) + "% done)")
		);
		
		window.setHeaderText(null);
		window.setTitle(WINDOW_TITLE);
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			okButton.setDisable(true);
			event.consume();	
			requestUrlResource(urlEntryField.getText());			
		});
		final Button cancelButton = (Button)window.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.addEventFilter(ActionEvent.ACTION, event -> urlDownloaderService.cancel());

		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private Node layoutContent() {
		final VBox mainLayout = new VBox(10);
		progressBar.prefWidthProperty().bind(mainLayout.widthProperty());
		
		mainLayout.getChildren().addAll(
				new Label("Please enter the location of the torrent you want to " +
						"open (URL, magnet link or info hash):"),
				buildUrlEntryContent(), progressLabel, progressBar);
		
		return mainLayout;
	}
	
	private Node buildUrlEntryContent() {
		final HBox entryLayout = new HBox();
		
		entryLayout.getChildren().addAll(new Label("Path: "), urlEntryField);
		entryLayout.setAlignment(Pos.CENTER);
		HBox.setHgrow(urlEntryField, Priority.ALWAYS);
		
		return entryLayout;
	}
	
	private String getClipboardContents() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();	    
		return clipboard.hasString()? clipboard.getString() : EMPTY_CLIPBOARD_CONTENT;		
	}
	
	//https://docs.oracle.com/javase/8/javafx/interoperability-tutorial/concurrency.htm
	private void requestUrlResource(final String url) {		
		if(url.trim().equals(EMPTY_CLIPBOARD_CONTENT) || 
				(resourceType = validateUrlResource(urlEntryField.getText())) == ResourceType.INVALID) {
			//Show error dialog
			final Alert invalidUrlAlert = new Alert(AlertType.ERROR);
			invalidUrlAlert.setContentText("Invalid or no path entered.");
			invalidUrlAlert.setTitle(WINDOW_TITLE);
			invalidUrlAlert.setHeaderText(null);
			invalidUrlAlert.showAndWait();
			return;
		}
		if(resourceType != ResourceType.URL) {
			/* We will leave loading of info hashes and magnet links to the caller
			   because it could take a while and as we can't show any progress anyway */
			window.close();
			return;
		}
		
		urlDownloaderService.setUrl(url);	
		
		progressBar.progressProperty().bind(urlDownloaderService.progressProperty());
		urlEntryField.setDisable(true);
		
		urlDownloaderService.start();
	}
		
	private ResourceType validateUrlResource(final String urlPath) {
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
				return ResourceType.INVALID;
			}
			catch(final URISyntaxException use) {
				return ResourceType.INVALID;
			}
		}
	}
	
	private class UrlDownloaderService extends Service<BinaryEncodedDictionary> {
		
		private final StringProperty url = new SimpleStringProperty();

        public final void setUrl(final String value) {
            url.set(value);
        }

        public final String getUrl() {
            return url.get();
        }

        public final StringProperty urlProperty() {
           return url;
        }

		@Override
		protected Task<BinaryEncodedDictionary> createTask() {
			return new Task<BinaryEncodedDictionary>() {
				@Override
				protected BinaryEncodedDictionary call() throws IOException {
					System.out.println("Download started");
					final URL targetUrl = new URL(getUrl());			
					final HttpURLConnection connection = (HttpURLConnection)targetUrl.openConnection();
					connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
					connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME, NetworkUtilities.HTTP_USER_AGENT_VALUE);
					
					final long contentLength = connection.getContentLengthLong();
					System.out.println("Content-Length: " + contentLength);
					
					//Platform.runLater(() -> progressBar.setProgress(contentLength != -1? 0 : ProgressBar.INDETERMINATE_PROGRESS));
					
					final int responseCode = connection.getResponseCode();
					if(responseCode == HttpURLConnection.HTTP_OK) {
						System.out.println("Received OK from the server, start downloading...");
						final InputStream responseStream = connection.getInputStream();
						final byte[] buffer = new byte[BUFFER_SIZE];
						long totalBytesRead = 0;
						long bytesRead = 0;
						
						while(!Thread.currentThread().isInterrupted() && (bytesRead = responseStream.read(buffer)) != -1) {
							totalBytesRead += bytesRead;						
							if(contentLength != -1) {
								updateProgress(totalBytesRead, contentLength);
							}
							else {
								System.out.println("updateMessage() called, should repaint ProgressBar");
								updateMessage("Unknown size, downloaded " + UnitConverter.formatByteCount(totalBytesRead));
							}
							try {
								Thread.sleep(500);
								System.out.println("Downloaded " + UnitConverter.formatByteCount(totalBytesRead));
							} catch (InterruptedException e) {
								System.err.println("Interrupted");
								Thread.currentThread().interrupt();
							}
						}
						
						System.out.println("Total bytes read: " + UnitConverter.formatByteCount(totalBytesRead));
					}
					else {
						throw new IOException("Remote server returned an error (error code: " 
								+ responseCode + ")");
					}
					//TODO: Return downloaded torrent map
					return null;
				}
				
			};
		}
		
	}
}