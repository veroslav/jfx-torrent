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
package org.matic.torrent.queue;

import org.matic.torrent.codec.BinaryEncodable;
import org.matic.torrent.codec.BinaryEncodedDictionary;
import org.matic.torrent.codec.BinaryEncodedInteger;
import org.matic.torrent.codec.BinaryEncodedList;
import org.matic.torrent.codec.BinaryEncodedString;
import org.matic.torrent.codec.BinaryEncodingKeys;
import org.matic.torrent.net.pwp.PwpPeer;
import org.matic.torrent.queue.enums.FilePriority;
import org.matic.torrent.queue.enums.QueueType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class QueuedTorrentProgress {

    private final BinaryEncodedDictionary torrentState;

    public QueuedTorrentProgress(final BinaryEncodedDictionary torrentState) {
        this.torrentState = torrentState;
    }

    public String getName() {
        final BinaryEncodable name = torrentState.get(BinaryEncodingKeys.KEY_NAME);
        return name != null? name.toString() : null;
    }

    protected void setName(final String name) {
        torrentState.put(BinaryEncodingKeys.KEY_NAME, new BinaryEncodedString(name));
    }

    public void addTrackerUrls(final Set<String> trackerUrls) {
        BinaryEncodedList list = (BinaryEncodedList)torrentState.get(
                BinaryEncodingKeys.KEY_ANNOUNCE_LIST);

        final BinaryEncodedList trackerList = list != null? list : new BinaryEncodedList();

        trackerUrls.forEach(t -> trackerList.add(new BinaryEncodedString(t)));
        torrentState.put(BinaryEncodingKeys.KEY_ANNOUNCE_LIST, trackerList);
    }

    public void removeTrackerUrls(final Collection<String> trackerUrls) {
        final BinaryEncodedList trackerList = (BinaryEncodedList)torrentState.get(
                BinaryEncodingKeys.KEY_ANNOUNCE_LIST);

        if(trackerList != null) {
            trackerList.remove(trackerUrls.stream().map(BinaryEncodedString::new).collect(Collectors.toList()));
        }
    }

    public Set<String> getTrackerUrls() {
        final BinaryEncodedList trackerList = (BinaryEncodedList)torrentState.get(
                BinaryEncodingKeys.KEY_ANNOUNCE_LIST);

        final Set<String> trackerUrls = new LinkedHashSet<>();

        if(trackerList != null) {
            trackerList.stream().forEach(t -> trackerUrls.add(t.toString()));
        }

        return trackerUrls;
    }

    protected void setTorrentPriority(final int priority) {
        torrentState.put(BinaryEncodingKeys.STATE_KEY_PRIORITY, new BinaryEncodedInteger(priority));
    }

    public int getTorrentPriority() {
        final BinaryEncodedInteger priority = (BinaryEncodedInteger)torrentState.get(BinaryEncodingKeys.STATE_KEY_PRIORITY);
        return priority != null? (int)priority.getValue() : 0;
    }

    public void setFilePriorities(final Map<String, FilePriority> filePrios) {
        BinaryEncodedDictionary filePrioMap = (BinaryEncodedDictionary)torrentState.get(
                BinaryEncodingKeys.STATE_KEY_FILE_PRIO);
        if(filePrioMap == null) {
            filePrioMap = new BinaryEncodedDictionary();
            torrentState.put(BinaryEncodingKeys.STATE_KEY_FILE_PRIO, filePrioMap);
        }
        final BinaryEncodedDictionary finalFilePrioMap = filePrioMap;
        filePrios.forEach((path, prio) -> finalFilePrioMap.put(
                new BinaryEncodedString(path), new BinaryEncodedString(prio.toString())));
    }

    public void setFilePriority(final String fileId, final FilePriority priority) {
        BinaryEncodedDictionary filePrioMap = (BinaryEncodedDictionary)torrentState.get(
                BinaryEncodingKeys.STATE_KEY_FILE_PRIO);
        if(filePrioMap == null) {
            filePrioMap = new BinaryEncodedDictionary();
            torrentState.put(BinaryEncodingKeys.STATE_KEY_FILE_PRIO, filePrioMap);
        }
        filePrioMap.put(new BinaryEncodedString(fileId), new BinaryEncodedInteger(priority.getValue()));
    }

    public FilePriority getFilePriority(final String fileId) {
        final BinaryEncodedDictionary filePriorityMap = (BinaryEncodedDictionary)torrentState.get(
                BinaryEncodingKeys.STATE_KEY_FILE_PRIO);
        if(filePriorityMap == null) {
            return FilePriority.NORMAL;
        }
        final BinaryEncodedInteger filePriority = (BinaryEncodedInteger)filePriorityMap.get(new BinaryEncodedString(fileId));
        if(filePriority == null) {
            return FilePriority.NORMAL;
        }
        return FilePriority.values()[(int)filePriority.getValue()];
    }

    protected void setAddedOn(final long addedOnMillis) {
        torrentState.put(BinaryEncodingKeys.STATE_KEY_ADDED_ON, new BinaryEncodedInteger(addedOnMillis));
    }

    public long getAddedOn() {
        return ((BinaryEncodedInteger)torrentState.get(BinaryEncodingKeys.STATE_KEY_ADDED_ON)).getValue();
    }

    public void setQueueStatus(final QueueType status) {
        torrentState.put(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME, new BinaryEncodedString(status.name()));
    }

    public QueueType getQueueStatus() {
        final BinaryEncodedString queueStatus = (BinaryEncodedString)torrentState.get(BinaryEncodingKeys.STATE_KEY_QUEUE_NAME);
        return queueStatus != null? QueueType.valueOf(queueStatus.toString()) : QueueType.NOT_ON_QUEUE;
    }

    public byte[] toExportableValue() throws IOException {
        return torrentState.toExportableValue();
    }

    /**
     * Store a (handshaken) peer so that we can connect to it again at a later stage.
     *
     * @param peer Handshaken peer to be stored
     */
    public void addPeer(final PwpPeer peer) {
        final BinaryEncodedDictionary peerDictionary = encodePeer(peer);
        final BinaryEncodedList peerList = (BinaryEncodedList)torrentState.get(BinaryEncodingKeys.STATE_KEY_PEERS);
        if(peerList != null && !peerList.contains(peerDictionary)) {
            peerList.add(peerDictionary);
        }
        else if(peerList == null) {
            final BinaryEncodedList newPeerList = new BinaryEncodedList(Arrays.asList(peerDictionary));
            torrentState.put(BinaryEncodingKeys.STATE_KEY_PEERS, newPeerList);
        }
    }

    /**
     * Retrieve and clear a list of previously stored (handshaken) peers.
     *
     * @param resetList Whether to remove all entries afterwards.
     * @return The list of (handshaken) peers
     */
    public Set<PwpPeer> getPeers(final boolean resetList) {
        final BinaryEncodedList peers = (BinaryEncodedList)torrentState.get(BinaryEncodingKeys.STATE_KEY_PEERS);
        if(peers == null) {
            return Collections.emptySet();
        }
        final Set<PwpPeer> extractedPeers = PwpPeer.extractPeers(peers, null);
        if(resetList) {
            torrentState.put(BinaryEncodingKeys.STATE_KEY_PEERS, new BinaryEncodedList());
        }
        return extractedPeers;
    }

    private BinaryEncodedDictionary encodePeer(final PwpPeer peer) {
        final BinaryEncodedDictionary peerDictionary = new BinaryEncodedDictionary();
        peerDictionary.put(BinaryEncodingKeys.KEY_IP, new BinaryEncodedString(peer.getIp()));
        peerDictionary.put(BinaryEncodingKeys.KEY_PORT, new BinaryEncodedInteger(peer.getPort()));
        return peerDictionary;
    }
}