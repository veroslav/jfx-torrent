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

package org.matic.torrent.gui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import org.matic.torrent.queue.FilePriority;

/**
 * A representation of a file entry in a torrent meta data file. It is used for
 * rendering the table of torrent contents when adding a new torrent.
 * 
 * @author vedran
 *
 */
public final class TorrentFileEntry {
	
	private final DoubleProperty progress;
	private final IntegerProperty priority;
	
	private final BooleanProperty selected;
	private final StringProperty name;
	private final StringProperty path;
	
	private final LongProperty pieceCount;
	private final LongProperty firstPiece;
	private final LongProperty selectionSize;
	private final LongProperty size;
	private final LongProperty done;
	
	private final Image fileImage;
	
	public TorrentFileEntry(final String name, final String path, 
			final long size, final boolean selected, final Image fileImage) {
		this.priority = new SimpleIntegerProperty(FilePriority.NORMAL.getValue());
		this.progress = new SimpleDoubleProperty();
		
		this.selected = new SimpleBooleanProperty(selected);		
		this.name = new SimpleStringProperty(name);
		this.path = new SimpleStringProperty(path);
		
		this.pieceCount = new SimpleLongProperty();
		this.firstPiece = new SimpleLongProperty();
		this.selectionSize = new SimpleLongProperty(selected? size : 0);
		this.size = new SimpleLongProperty(size);
		this.done = new SimpleLongProperty();
		
		this.fileImage = fileImage;
	}
	
	public final Image getImage() {
		return fileImage;
	}
	
	public final void setSelected(final boolean selected) {		
		this.selected.set(selected);
	}
	
	public final boolean isSelected() {	
		return selected.get();
	}
	
	public final long getSize() {
		return size.get();
	}
	
	public final long getSelectionSize() {
		return selectionSize.get();
	}
	
	public final void updateSelectionSize(final long sizeDiff) {
		selectionSize.set(selectionSize.get() + sizeDiff);
	}
	
	public void setFirstPiece(final long firstPiece) {
		this.firstPiece.set(firstPiece);
	}
	
	public LongProperty firstPieceProperty() {
		return firstPiece;
	}
	
	public void setPieceCount(final long pieceCount) {
		this.pieceCount.set(pieceCount);
	}
	
	public LongProperty pieceCountProperty() {
		return pieceCount;
	}
	
	public void setDone(final long done) {
		this.done.set(done);
	}
	
	public LongProperty doneProperty() {
		return done;
	}
		
	public final BooleanProperty selectedProperty() {		
		return selected;
	}
	
	public final StringProperty nameProperty() {
		return name;
	}
	
	public final StringProperty pathProperty() {
		return path;
	}
	
	public final IntegerProperty priorityProperty() {
		return priority;
	}
	
	public final DoubleProperty progressProperty() {
		return progress;
	}
	
	public final LongProperty sizeProperty() {
		return size;
	}
	
	public final LongProperty selectionSizeProperty() {
		return selectionSize;
	}

	@Override
	public String toString() {
		return "TorrentContentModel [selected=" + selected + ", name=" + name
				+ ", path=" + path + ", size=" + selectionSize + "]";
	}	
}

