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

package org.matic.torrent.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.matic.torrent.preferences.ApplicationPreferences;
import org.matic.torrent.preferences.PathProperties;

final class RetryableCertificateManager {
	
	private static final String TRUST_STORE_FILE_NAME = "jssecacerts";	
	private static final String TRUST_STORE_PASSWORD = "changeit"; 
	
	private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
	private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
	
	private static final String PROTOCOL = "TLS";

	private final RetryableCertificateInterceptor certificateInterceptor;
	private final TrustManagerFactory trustManagerFactory; 	
	private final SSLSocketFactory sslSocketFactory;
	private final KeyStore trustStore;
	
	private final char[] trustStorePassword;
	
	RetryableCertificateManager() throws CertificateManagerInitializationException {		
		trustStorePassword = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY, TRUST_STORE_PASSWORD).toCharArray();		
		try {			
			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			
			final File trustStoreFile = new File(System.getProperty(TRUST_STORE_PROPERTY, getKeyStorePath()));
			if(!trustStoreFile.exists()) {	
				//System.setProperty(TRUST_STORE_PROPERTY, TRUST_STORE_PATH);
				//System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, TRUST_STORE_PASSWORD);
				trustStore.load(null, trustStorePassword);				
			}
			else {
				try(final InputStream trustStoreInputStream = new FileInputStream(trustStoreFile)) {				
					trustStore.load(trustStoreInputStream, trustStorePassword);						
				}
			}
			
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);
			
			final X509TrustManager trustManager = (X509TrustManager)trustManagerFactory.getTrustManagers()[0];
			certificateInterceptor = new RetryableCertificateInterceptor(trustManager);
			
			final SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
			sslContext.init(null, new TrustManager[]{certificateInterceptor}, null);
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (final KeyManagementException | NoSuchAlgorithmException
				| KeyStoreException | CertificateException | IOException e) {
			throw new CertificateManagerInitializationException(e.getMessage(), e.getCause());
		}	    
	}
	
	public final void addToTrustStore(final String alias, final X509Certificate[] certificates) 
			throws CertificateManagerInitializationException {
		for(int i = 0; i < certificates.length; i++) {
			try {
				trustStore.setCertificateEntry(alias + "[" + (i + 1) + "]", certificates[i]);				
			} 
			catch(final KeyStoreException kse) {
				throw new CertificateManagerInitializationException(kse.getMessage(), kse.getCause());
			}
		}
		
		updateKeyStore();
	}
	
	final SSLSocketFactory getSslSocketFactory() {
		return sslSocketFactory;
	}
	
	final X509Certificate[] getCertificateChain() {
		return certificateInterceptor.getChain();
	}
	
	private static String getKeyStorePath() {
		return ApplicationPreferences.getProperty(PathProperties.KEY_STORE, 
				System.getProperty("user.dir")) + 
				File.separator + TRUST_STORE_FILE_NAME;
	}
	
	private void updateKeyStore() throws CertificateManagerInitializationException {		
		try(final OutputStream output = new FileOutputStream(getKeyStorePath())) {
			trustStore.store(output, TRUST_STORE_PASSWORD.toCharArray());
			trustManagerFactory.init(trustStore);
			certificateInterceptor.setTrustManager((X509TrustManager)trustManagerFactory.getTrustManagers()[0]);
		} catch (final KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			throw new CertificateManagerInitializationException(e.getMessage(), e.getCause());			
		}
		
		System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, TRUST_STORE_PASSWORD);
		System.setProperty(TRUST_STORE_PROPERTY, getKeyStorePath());	    
	}
}