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
package org.matic.torrent.net;

import org.matic.torrent.client.ClientProperties;
import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.NetworkProperties;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtilities {

    public static final String HTTP_CONTENT_ENCODING = "Content-Encoding";
    public static final String HTTP_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";

    public static final String DEFAULT_NETWORK_INTERFACE = "Any interface";

    public static final int HTTP_CONNECTION_TIMEOUT = 10000; //10 seconds

    //Protocol constants
    public static final String HTTPS_PROTOCOL = "https";
    public static final String HTTP_PROTOCOL = "http";
    public static final String UDP_PROTOCOL = "udp";

    private static final String HTTP_USER_AGENT_VALUE = buildHttpUserAgentValue();

    public static final String HTTP_ACCEPT_CHARSET = "Accept-Charset";
    public static final String HTTP_USER_AGENT_NAME = "User-Agent";
    public static final String HTTP_GZIP_ENCODING = "gzip";

    private static final RetryableCertificateManager CERTIFICATE_MANAGER = initCertificateManager();

    private static final Enumeration<InetAddress> INTERFACE_ADDRESSES = initInterfaceAddresses();

    public static String getHttpUserAgent() {
        return HTTP_USER_AGENT_VALUE;
    }

    private static Enumeration<InetAddress> initInterfaceAddresses() {
        final String interfaceName = ApplicationPreferences.getProperty(
                NetworkProperties.NETWORK_INTERFACE_NAME, null);

        try {
            final NetworkInterface listenInterface = interfaceName != null &&
                    !DEFAULT_NETWORK_INTERFACE.equals(interfaceName)?
                    NetworkInterface.getByName(interfaceName) : null;
            return listenInterface != null? listenInterface.getInetAddresses() : Collections.emptyEnumeration();
        } catch (final SocketException se) {
            return Collections.emptyEnumeration();
        }
    }

    public static HttpURLConnection connectTo(final URL url) throws IOException,
            CertificateManagerInitializationException {
        HttpURLConnection.setFollowRedirects(true);
        final HttpURLConnection connection = HTTPS_PROTOCOL.equals(url.getProtocol())?
                initSecureConnection(url) : (HttpURLConnection)url.openConnection();

        connection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
        connection.setReadTimeout(HTTP_CONNECTION_TIMEOUT);

        return connection;
    }

    public static void addTrustedCertificate(final String url, final X509Certificate[] certificates)
            throws CertificateManagerInitializationException {
        if(CERTIFICATE_MANAGER == null) {
            throw new CertificateManagerInitializationException("Certificate manager is null");
        }
        CERTIFICATE_MANAGER.addToTrustStore(url, certificates);
    }

    public static X509Certificate[] getInterceptedCertificates() {
        return CERTIFICATE_MANAGER == null? new X509Certificate[0] : CERTIFICATE_MANAGER.getCertificateChain();
    }

    public static List<String> getAvailableNetworkInterfaces() {
        final List<String> networkInterfaceNames = new ArrayList<>();
        networkInterfaceNames.add(DEFAULT_NETWORK_INTERFACE);
        try {
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()) {
                final NetworkInterface face = networkInterfaces.nextElement();
                if(!face.isLoopback()) {
                    networkInterfaceNames.add(face.getName());
                }
            }
        } catch (final SocketException se) {
            se.printStackTrace();
        }
        return networkInterfaceNames;
    }

    //TODO: Do NOT fallback to default interface unless user specifically requests it (VPN case)
    public static InetSocketAddress getSocketAddress(final int port) {
        return INTERFACE_ADDRESSES.hasMoreElements()?
                new InetSocketAddress(INTERFACE_ADDRESSES.nextElement(), port) : new InetSocketAddress(port);
    }

    public static String resolveIp(final String ip) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getHostName();
        } catch (final UnknownHostException uhe) {
            return ip;
        }
    }

    private static HttpURLConnection initSecureConnection(final URL url) throws IOException,
            CertificateManagerInitializationException {
        if(CERTIFICATE_MANAGER == null) {
            throw new CertificateManagerInitializationException("Certificate manager is null");
        }
        final HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setSSLSocketFactory(CERTIFICATE_MANAGER.getSslSocketFactory());

        return connection;
    }

    private static RetryableCertificateManager initCertificateManager() {
        //TODO: Always perform null check on returned certificate manager
        RetryableCertificateManager manager;
        try {
            manager = new RetryableCertificateManager();
        }
        catch(final CertificateManagerInitializationException cie) {
            manager = null;
        }
        return manager;
    }

    private static String buildHttpUserAgentValue() {
        final StringBuilder userAgent = new StringBuilder("Mozilla/5.0 (");
        switch(ClientProperties.getOS()) {
            case Windows:
                userAgent.append("Windows NT 6.3");
                break;
            case Mac:
                userAgent.append("Macintosh; Intel Mac OS X 10_10");
                break;
            default:
                userAgent.append("X11; Linux x86_64");
        }
        userAgent.append("; rv:36.0) Gecko/20100101 Firefox/37.0");
        return userAgent.toString();
    }
}
