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
package org.matic.torrent.gui.window;

import javafx.concurrent.Task;
import org.matic.torrent.codec.BinaryDecoder;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.exception.BinaryDecoderException;
import org.matic.torrent.net.NetworkUtilities;
import org.matic.torrent.peer.ClientProperties;
import org.matic.torrent.utils.UnitConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A task that downloads a remote torrent from an URL.
 *
 * @author Vedran Matic
 */
public final class UrlLoaderTask extends Task<BinaryEncodedDictionary> {

    private static final int BUFFER_SIZE = 16384;
    private final String url;

    public UrlLoaderTask(final String url) {
        this.url = url;
    }

    @Override
    protected BinaryEncodedDictionary call() throws Exception {
        super.updateMessage("Waiting for download to start...");
        super.updateProgress(0, 100);

        final URL targetUrl = new URL(url);

        HttpURLConnection connection = NetworkUtilities.connectTo(targetUrl);
        connection.setInstanceFollowRedirects(true);
        initConnection(connection);

        HttpURLConnection.setFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        boolean isRedirect = false;

        if (responseCode != HttpURLConnection.HTTP_OK) {
            isRedirect = responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER;
            if(!isRedirect) {
                throw new IOException("Remote server returned an error (error code: "
                        + responseCode + ")");
            }
        }

        if(isRedirect) {
            final String redirectUrl = connection.getHeaderField("Location");
            final String cookies = connection.getHeaderField("Set-Cookie");

            connection = (HttpURLConnection)new URL(redirectUrl).openConnection();
            connection.setRequestProperty("Cookie", cookies);
            initConnection(connection);
        }

        responseCode = connection.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK) {
            return extractRemoteTorrentContents(connection);
        }
        else {
            throw new IOException("Remote server returned an error (error code: "
                    + responseCode + ")");
        }
    }

    private BinaryEncodedDictionary extractRemoteTorrentContents(
            HttpURLConnection connection) throws IOException, BinaryDecoderException {
        final long contentLength = connection.getContentLengthLong();
        final String contentEncoding = connection.getHeaderField(
                NetworkUtilities.HTTP_CONTENT_ENCODING);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(final InputStream responseStream = connection.getInputStream()) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytesRead = 0;
            int bytesRead = 0;

            while(!isCancelled() && (bytesRead = responseStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if(contentLength != -1) {
                    super.updateProgress(totalBytesRead, contentLength);
                    super.updateMessage("Downloading torrent (" +
                            (int)(totalBytesRead * 100.0 / contentLength + 0.5) + "% done)");
                }
                else {
                    super.updateMessage("Unknown size, downloaded " +
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

    private void initConnection(final HttpURLConnection connection) {
        connection.setRequestProperty(NetworkUtilities.HTTP_USER_AGENT_NAME,
                NetworkUtilities.getHttpUserAgent());
        connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_ENCODING,
                NetworkUtilities.HTTP_GZIP_ENCODING);
        connection.setRequestProperty(NetworkUtilities.HTTP_ACCEPT_LANGUAGE,
                ClientProperties.getUserLocale());
    }
}
