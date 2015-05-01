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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public final class SelfSignedCertificateInterceptor implements X509TrustManager {
	private final X509TrustManager trustManager;
	private X509Certificate[] chain;

	public SelfSignedCertificateInterceptor(final X509TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	@Override
	public final X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}

	@Override
	public final void checkClientTrusted(final X509Certificate[] chain,
			final String authType) throws CertificateException {
		throw new UnsupportedOperationException();		
	}

	@Override
	public final void checkServerTrusted(final X509Certificate[] chain,
			final String authType) throws CertificateException {
		this.chain = chain;
		trustManager.checkServerTrusted(this.chain, authType);
	}
	
	public final X509Certificate[] getChain() {
		final X509Certificate[] chainCopy = new X509Certificate[chain.length];
		System.arraycopy(chain, 0, chainCopy, 0, chainCopy.length);
		
		return chainCopy;
	}
}