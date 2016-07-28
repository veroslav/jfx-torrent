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
package org.matic.torrent.net.pwp;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class that maps binary peer id to a humanly readable version of
 * the form "client_name client_version"
 *
 * @author Vedran Matic
 *
 */
public final class PeerIdMapper {

    private static final Map<String, String> AZUREUS_STYLE_MAPPINGS = new HashMap<>();

    static {
        AZUREUS_STYLE_MAPPINGS.put("-7T", "aTorrent for Android");
        AZUREUS_STYLE_MAPPINGS.put("-AB", "AnyEvent::BitTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-AG", "Ares");
        AZUREUS_STYLE_MAPPINGS.put("-A~", "Ares");
        AZUREUS_STYLE_MAPPINGS.put("-AR", "Arctic");
        AZUREUS_STYLE_MAPPINGS.put("-AT", "Artemis");
        AZUREUS_STYLE_MAPPINGS.put("-AV", "Avicora");
        AZUREUS_STYLE_MAPPINGS.put("-AX", "BitPump");
        AZUREUS_STYLE_MAPPINGS.put("-AZ", "Azureus");
        AZUREUS_STYLE_MAPPINGS.put("-BB", "BitBuddy");
        AZUREUS_STYLE_MAPPINGS.put("-BC", "BitComet");
        AZUREUS_STYLE_MAPPINGS.put("-BE", "Baretorrent");
        AZUREUS_STYLE_MAPPINGS.put("-BF", "Bitflu");
        AZUREUS_STYLE_MAPPINGS.put("-BG", "BTG");
        AZUREUS_STYLE_MAPPINGS.put("-BL", "BitBlinder");
        AZUREUS_STYLE_MAPPINGS.put("-BP", "BitTorrent Pro");
        AZUREUS_STYLE_MAPPINGS.put("-BR", "BitRocket");
        AZUREUS_STYLE_MAPPINGS.put("-BS", "BTSlave");
        AZUREUS_STYLE_MAPPINGS.put("-BT", "BitTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-BW", "BitWombat");
        AZUREUS_STYLE_MAPPINGS.put("-BX", "~Bittorrent X");
        AZUREUS_STYLE_MAPPINGS.put("-CD", "Enhanced CTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-CT", "CTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-DE", "Deluge");
        AZUREUS_STYLE_MAPPINGS.put("-DP", "Propagate Data Client");
        AZUREUS_STYLE_MAPPINGS.put("-EB", "EBit");
        AZUREUS_STYLE_MAPPINGS.put("-ES", "electric sheep");
        AZUREUS_STYLE_MAPPINGS.put("-FC", "FileCroc");
        AZUREUS_STYLE_MAPPINGS.put("-FD", "Free Download Manager");
        AZUREUS_STYLE_MAPPINGS.put("-FT", "FoxTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-FX", "Freebox BitTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-GS", "GSTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-HK", "Hekate");
        AZUREUS_STYLE_MAPPINGS.put("-HL", "Halite");
        AZUREUS_STYLE_MAPPINGS.put("-HM", "hMule");
        AZUREUS_STYLE_MAPPINGS.put("-HN", "Hydranode");
        AZUREUS_STYLE_MAPPINGS.put("-IL", "iLivid");
        AZUREUS_STYLE_MAPPINGS.put("-JS", "Justseed.it client");
        AZUREUS_STYLE_MAPPINGS.put("-JT", "JavaTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-JX", "Trabos");
        AZUREUS_STYLE_MAPPINGS.put("-KG", "KGet");
        AZUREUS_STYLE_MAPPINGS.put("-KT", "KTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-LC", "LeechCraft");
        AZUREUS_STYLE_MAPPINGS.put("-LH", "LH-ABC");
        AZUREUS_STYLE_MAPPINGS.put("-LP", "Lphant");
        AZUREUS_STYLE_MAPPINGS.put("-LT", "libtorrent");
        AZUREUS_STYLE_MAPPINGS.put("-lt", "libTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-LW", "LimeWire");
        AZUREUS_STYLE_MAPPINGS.put("-MK", "Meerkat");
        AZUREUS_STYLE_MAPPINGS.put("-MO", "MonoTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-MP", "MooPolice");
        AZUREUS_STYLE_MAPPINGS.put("-MR", "Miro");
        AZUREUS_STYLE_MAPPINGS.put("-MT", "MoonlightTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-NB", "Net::BitTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-NX", "Net Transport");
        AZUREUS_STYLE_MAPPINGS.put("-OS", "OneSwarm");
        AZUREUS_STYLE_MAPPINGS.put("-OT", "OmegaTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-PB", "Protocol::BitTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-PD", "Pando");
        AZUREUS_STYLE_MAPPINGS.put("-PI", "PicoTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-PT", "PHPTracker");
        AZUREUS_STYLE_MAPPINGS.put("-qB", "qBittorrent");
        AZUREUS_STYLE_MAPPINGS.put("-QD", "QQDownload");
        AZUREUS_STYLE_MAPPINGS.put("-QT", "Qt4 Torrent");
        AZUREUS_STYLE_MAPPINGS.put("-RT", "Retriever");
        AZUREUS_STYLE_MAPPINGS.put("-RZ", "RezTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-S~", "Shareaza alpha/beta");
        AZUREUS_STYLE_MAPPINGS.put("-SB", "~Swiftbit");
        AZUREUS_STYLE_MAPPINGS.put("-SD", "Thunder");
        AZUREUS_STYLE_MAPPINGS.put("-SM", "SoMud");
        AZUREUS_STYLE_MAPPINGS.put("-SP", "BitSpirit");
        AZUREUS_STYLE_MAPPINGS.put("-SS", "SwarmScope");
        AZUREUS_STYLE_MAPPINGS.put("-ST", "SymTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-st", "sharktorrent");
        AZUREUS_STYLE_MAPPINGS.put("-SZ", "Shareaza");
        AZUREUS_STYLE_MAPPINGS.put("-TB", "Torch");
        AZUREUS_STYLE_MAPPINGS.put("-TE", "terasaur Seed Bank");
        AZUREUS_STYLE_MAPPINGS.put("-TL", "Tribler");
        AZUREUS_STYLE_MAPPINGS.put("-TN", "TorrentDotNet");
        AZUREUS_STYLE_MAPPINGS.put("-TR", "Transmission");
        AZUREUS_STYLE_MAPPINGS.put("-TS", "Torrentstorm");
        AZUREUS_STYLE_MAPPINGS.put("-TT", "TuoTu");
        AZUREUS_STYLE_MAPPINGS.put("-UL", "uLeecher!");
        AZUREUS_STYLE_MAPPINGS.put("-UT", "\u03BCTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-UM", "\u03BCTorrent for Mac");
        AZUREUS_STYLE_MAPPINGS.put("-VG", "Vagaa");
        AZUREUS_STYLE_MAPPINGS.put("-WD", "WebTorrent Desktop");
        AZUREUS_STYLE_MAPPINGS.put("-WT", "BitLet");
        AZUREUS_STYLE_MAPPINGS.put("-WW", "WebTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-WY", "FireTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-XF", "Xfplay");
        AZUREUS_STYLE_MAPPINGS.put("-XL", "Xunlei");
        AZUREUS_STYLE_MAPPINGS.put("-XS", "XSwifter");
        AZUREUS_STYLE_MAPPINGS.put("-XT", "XanTorrent");
        AZUREUS_STYLE_MAPPINGS.put("-XX", "Xtorrent");
        AZUREUS_STYLE_MAPPINGS.put("-ZT", "ZipTorrent");
    }

    public static String mapId(final byte[] peerId) {

        //Check whether peer id is of Azureus style
        final String azureusClientMapping = PeerIdMapper.findAzureusMappingKey(peerId);
        if(azureusClientMapping != null) {
            return azureusClientMapping;
        }

        return "unknown client " + new String(peerId, StandardCharsets.UTF_8);
    }

    private static String findAzureusMappingKey(final byte[] peerId) {
        if('-' == peerId[0] && Character.isAlphabetic(peerId[1]) &&
                Character.isAlphabetic(peerId[2]) && '-' == peerId[7]) {

            final String searchParam = new String(peerId, 0, 3, StandardCharsets.UTF_8);
            final String clientName = AZUREUS_STYLE_MAPPINGS.get(searchParam);

            if(clientName != null) {
                //Get the client version
                final StringBuilder client = new StringBuilder();

                int position = 6;
                while(peerId[position--] == '0');

                for(int i = position + 1; i > 2; --i) {
                    client.insert(0, (char)peerId[i]);
                    client.insert(0, '.');
                }
                client.setCharAt(0, ' ');
                client.insert(0, clientName);

                return client.toString();
            }
        }
        return null;
    }

    //Mainline M7-4-3--XXXXX...
    private static String findShadowMappingKey() {

        return null;
    }
}