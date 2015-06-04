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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import javax.net.ssl.SSLException;

import org.matic.torrent.io.codec.BinaryDecoder;
import org.matic.torrent.io.codec.BinaryDecoderException;
import org.matic.torrent.io.codec.BinaryEncodedDictionary;
import org.matic.torrent.net.CertificateManagerInitializationException;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.peer.ClientProperties;
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
	
	public enum ResourceType {
		URL, MAGNET_LINK, INFO_HASH, NONE
	}
	
	private static final int BUFFER_SIZE = 16384;
	
	private static final String WINDOW_TITLE = "Add Torrent from URL";
	private static final String EMPTY_CLIPBOARD_CONTENT = "";
	
	private static final String VALID_MAGNET_LINK_PREFIX = "magnet";
		
	private ResourceType resourceType = ResourceType.NONE;
	private UrlDownloaderTask urlDownloaderTask = null;
	private BinaryEncodedDictionary torrentMap = null;			
	
	private final ExecutorService urlDownloadExecutor;
	
	private final TextField urlEntryField;	
	private final ProgressBar progressBar;
	private final Label progressLabel;
	
	private final Dialog<ButtonType> window;

	public UrlLoaderWindow(final Window owner) {		
		window = new Dialog<>();
		window.initOwner(owner);
		
		urlDownloadExecutor = Executors.newSingleThreadExecutor();
		
		progressBar = new ProgressBar(0);
		urlEntryField = new TextField();		
		progressLabel = new Label();
		
		initComponents();
	}
	
	public final UrlLoaderWindowOptions showAndWait() {
		final Optional<ButtonType> result = window.showAndWait();
		urlDownloadExecutor.shutdown();
		
		if(result.isPresent() && resourceType != ResourceType.NONE) {
			return new UrlLoaderWindowOptions(resourceType, urlEntryField.getText(), torrentMap);
		}		
		return null;
	}
	
	private void initComponents() {
		window.setHeaderText(null);
		window.setTitle(WINDOW_TITLE);
		
		window.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		window.setOnCloseRequest(handler -> {
			if(urlDownloaderTask != null) {
				urlDownloaderTask.cancel();
			}
		});
		
		final Button okButton = (Button)window.getDialogPane().lookupButton(ButtonType.OK);
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			okButton.setDisable(true);
			event.consume();	
			final boolean urlValidated = requestUrlResource(urlEntryField.getText());
			okButton.setDisable(urlValidated);
		});
		final Button cancelButton = (Button)window.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.addEventFilter(ActionEvent.ACTION, event -> {
			window.setResult(ButtonType.CANCEL);
			resourceType = ResourceType.NONE;
			if(urlDownloaderTask != null) {
				urlDownloaderTask.cancel();
			}
		});
		
		urlEntryField.textProperty().addListener((obs, oldV, newV) -> okButton.setDisable(newV.isEmpty()));
		
		final String clipBoardContents = getClipboardContents();
		if(validateUrlResource(clipBoardContents) != ResourceType.NONE) {
			urlEntryField.setText(clipBoardContents);
		}
		
		final boolean hasValidClipboardContents = validateUrlResource(clipBoardContents) != ResourceType.NONE;		
		okButton.setDisable(!hasValidClipboardContents);
		
		if(hasValidClipboardContents) {
			urlEntryField.setText(clipBoardContents);			
		}
	
		window.setResizable(true);		
		window.getDialogPane().setContent(layoutContent());
	}
	
	private void initUrlDownloader(final UrlDownloaderTask urlDownloaderTask) {
		urlDownloaderTask.setOnSucceeded(handler -> {
			progressBar.progressProperty().unbind();
			torrentMap = (BinaryEncodedDictionary)handler.getSource().getValue();
			window.close();
		});
		urlDownloaderTask.setOnCancelled(handler -> {
			progressBar.progressProperty().unbind();
			if(window.getResult() != ButtonType.OK) {
				resourceType = ResourceType.NONE;
			}			
		});
		urlDownloaderTask.setOnFailed(handler -> {
			progressBar.progressProperty().unbind();
			final Throwable throwable = handler.getSource().getException();			
			
			if(throwable instanceof SSLException) {						
				final X509Certificate[] certificates = NetworkUtilities.getInterceptedCertificates();
				final boolean acceptCertificateChain = confirmCertificateChainAddition(certificates);
				if(acceptCertificateChain) {
					final String url = urlDownloaderTask.url.get();
					try {
						NetworkUtilities.addTrustedCertificate(url, certificates);						
					}
					catch(final Exception e) {
						e.printStackTrace();
						resourceType = ResourceType.NONE;
						window.close();
						
						showAlert(AlertType.ERROR, "The certificate could not be added.",
								"Add certificate to trust store");
						return;
					}						
					requestUrlResource(url);
				}
				else {
					resourceType = ResourceType.NONE;
					window.close();
				}
			}
			else {
				resourceType = ResourceType.NONE;
				window.close();
				
				showAlert(AlertType.ERROR, "Invalid torrent contents.", "Invalid torrent");
			}
		});
		
		urlDownloaderTask.messageProperty().addListener((obs, oldV, newV) -> progressLabel.setText(newV));		
		progressBar.progressProperty().addListener((obs, oldV, newV) -> {
			final int percentDone = (int)(newV.doubleValue() * 100);
			if(percentDone == 0) {
				progressLabel.setText("Waiting for download to start...");
			}
			else {
				progressLabel.setText("Downloading torrent (" + percentDone + "% done)");
			}
		});
	}
	
	private boolean confirmCertificateChainAddition(final X509Certificate[] certificateChain) {
		final StringBuilder userQuestion = new StringBuilder();
		userQuestion.append("The remote server is using a self signed certificate.");
		userQuestion.append("\n");
		userQuestion.append("Would you like to add it to the trusted certificate list?");
		
		final StringBuilder certificateSummary = new StringBuilder();
	    for(final X509Certificate certificate : certificateChain) {
	    	certificateSummary.append(certificate.toString());
	    }
	    
	    final Alert acceptAlert = new Alert(AlertType.WARNING, userQuestion.toString(),
	    		ButtonType.OK, ButtonType.CANCEL);
	    acceptAlert.setHeaderText("Potential Security Risk");
	    acceptAlert.setTitle("Accept self signed server certificate");
	    acceptAlert.initOwner(window.getOwner());
	    acceptAlert.setResizable(true);
	    acceptAlert.setWidth(1000);
	    acceptAlert.setHeight(400);
	    
	    final Label detailsLabel = new Label("Certificate details:");

	    final TextArea detailstextArea = new TextArea(certificateSummary.toString());
	    detailstextArea.setEditable(false);
	    detailstextArea.setWrapText(true);
	    detailstextArea.setMaxWidth(Double.MAX_VALUE);
	    detailstextArea.setMaxHeight(Double.MAX_VALUE);
	    
	    GridPane.setVgrow(detailstextArea, Priority.ALWAYS);
	    GridPane.setHgrow(detailstextArea, Priority.ALWAYS);

	    final GridPane expandableContent = new GridPane();
	    expandableContent.setMaxWidth(Double.MAX_VALUE);
	    expandableContent.add(detailsLabel, 0, 0);
	    expandableContent.add(detailstextArea, 0, 1);

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
		return clipboard.hasString()? clipboard.getString() : EMPTY_CLIPBOARD_CONTENT;		
	}
	
	private boolean requestUrlResource(final String url) {		
		if(url.trim().equals(EMPTY_CLIPBOARD_CONTENT) || 
				(resourceType = validateUrlResource(urlEntryField.getText())) == ResourceType.NONE) {
			showErrorMessage("Invalid path entered.");			
			return false;
		}
		if(resourceType != ResourceType.URL) {
			/* We will leave loading of info hashes and magnet links to the caller
			   because it could take a while and as we can't show any progress anyway */
			window.setResult(ButtonType.OK);
			return true;
		}		
		urlDownloaderTask = new UrlDownloaderTask();
		initUrlDownloader(urlDownloaderTask);
		
		urlDownloaderTask.setUrl(url);		
		progressBar.progressProperty().bind(urlDownloaderTask.progressProperty());		
		urlEntryField.setDisable(true);
		
		urlDownloadExecutor.submit(urlDownloaderTask);
		
		return true;
	}
	
	private void showErrorMessage(final String message) {
		final Alert invalidUrlAlert = new Alert(AlertType.ERROR);
		invalidUrlAlert.setContentText(message);
		invalidUrlAlert.setTitle(WINDOW_TITLE);
		invalidUrlAlert.setHeaderText(null);
		invalidUrlAlert.showAndWait();
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
				return ResourceType.NONE;
			}
			catch(final URISyntaxException use) {
				return ResourceType.NONE;
			}
		}
	}
	
	private class UrlDownloaderTask extends Task<BinaryEncodedDictionary> {		
		private final StringProperty url = new SimpleStringProperty();

        public final void setUrl(final String value) {
            url.set(value);
        }

        public final String getUrl() {
            return url.get();
        }

		@Override
		protected final BinaryEncodedDictionary call() throws IOException, 
			BinaryDecoderException, CertificateManagerInitializationException {			
			updateProgress(0, 100);			
			
			final URL targetUrl = new URL(getUrl());	
			
			final HttpURLConnection connection = NetworkUtilities.connectTo(targetUrl);
			connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME, 
					NetworkUtilities.getHttpUserAgent());
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_ENCODING, 
					NetworkUtilities.HTTP_GZIP_ENCODING);
			connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_LANGUAGE, 
					ClientProperties.getUserLocale());
			
			final long contentLength = connection.getContentLengthLong();			
			final int responseCode = connection.getResponseCode();
			final String contentEncoding = connection.getHeaderField(
					NetworkUtilities.HTTP_CONTENT_ENCODING);
			
			if(responseCode == HttpURLConnection.HTTP_OK) {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try(final InputStream responseStream = connection.getInputStream()) {
					final byte[] buffer = new byte[BUFFER_SIZE];
					long totalBytesRead = 0;
					int bytesRead = 0;
					
					while(!isCancelled() && (bytesRead = responseStream.read(buffer)) != -1) {										
						baos.write(buffer, 0, bytesRead);
						totalBytesRead += bytesRead;
						
						if(contentLength != -1) {
							updateProgress(totalBytesRead, contentLength);
						}
						else {
							updateMessage("Unknown size, downloaded " + 
									UnitConverter.formatByteCount(totalBytesRead));
						}					
					}
				}				
				try(final InputStream torrentStream = new ByteArrayInputStream(baos.toByteArray())) {
					final BinaryDecoder decoder = new BinaryDecoder();
					return contentEncoding != null && 
							NetworkUtilities.HTTP_GZIP_ENCODING.equals(contentEncoding)?
							decoder.decodeGzip(torrentStream) : decoder.decode(torrentStream);
				}
			}
			else {
				throw new IOException("Remote server returned an error (error code: " 
						+ responseCode + ")");
			}										
		}		
	}
}