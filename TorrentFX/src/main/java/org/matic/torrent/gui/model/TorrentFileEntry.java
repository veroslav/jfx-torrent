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

package org.matic.torrent.gui.model;

import org.matic.torrent.queue.FilePriority;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * A representation of a file entry in a torrent meta data file. It is used for
 * rendering the table of torrent contents when adding a new torrent.
 * 
 * @author vedran
 *
 */
public final class TorrentFileEntry {
	
	private final DoubleProperty progress;
	private final ObjectProperty<FilePriority> priority;
	
	private final BooleanProperty selected;
	private final StringProperty name;
	private final StringProperty path;
	
	private final LongProperty pieceCount;
	private final LongProperty firstPiece;
	private final LongProperty selectionLength;
	private final LongProperty length;
	private final LongProperty done;
	
	private final Image fileImage;
	
	public TorrentFileEntry(final String name, final String path, 
			final long length, final boolean selected, final Image fileImage) {
		this.priority = new SimpleObjectProperty<>(FilePriority.NORMAL);
		this.progress = new SimpleDoubleProperty();
		
		this.selected = new SimpleBooleanProperty(selected);		
		this.name = new SimpleStringProperty(name);
		this.path = new SimpleStringProperty(path);
		
		this.pieceCount = new SimpleLongProperty();
		this.firstPiece = new SimpleLongProperty();
		this.selectionLength = new SimpleLongProperty(selected? length : 0);
		this.length = new SimpleLongProperty(length);
		this.done = new SimpleLongProperty();
		
		this.fileImage = fileImage;
	}
	
	public Image getImage() {
		return fileImage;
	}
	
	public void setSelected(final boolean selected) {		
		this.selected.set(selected);
	}
	
	public boolean isSelected() {	
		return selected.get();
	}
	
	public long getLength() {
		return length.get();
	}
	
	public void updateLength(final long lengthDiff) {
		length.set(length.get() + lengthDiff);
	}
	
	public long getSelectionLength() {
		return selectionLength.get();
	}
	
	public void updateSelectionLength(final long sizeLength) {
		selectionLength.set(selectionLength.get() + sizeLength);
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
	
	public void setPriority(final FilePriority priority) {
		this.priority.set(priority);
	}
	
	public FilePriority getPriority() {
		return priority.get();
	}
	
	public LongProperty doneProperty() {
		return done;
	}
		
	public BooleanProperty selectedProperty() {		
		return selected;
	}
	
	public StringProperty nameProperty() {
		return name;
	}
	
	public StringProperty pathProperty() {
		return path;
	}
	
	public ObjectProperty<FilePriority> priorityProperty() {
		return priority;
	}
	
	public DoubleProperty progressProperty() {
		return progress;
	}
	
	public LongProperty lengthProperty() {
		return length;
	}
	
	public LongProperty selectionLengthProperty() {
		return selectionLength;
	}

	@Override
	public String toString() {
		return "TorrentFileEntry [progress=" + progress + ", priority=" + priority + ", selected=" + selected
				+ ", name=" + name + ", path=" + path + ", pieceCount=" + pieceCount + ", firstPiece=" + firstPiece
				+ ", selectionLength=" + selectionLength + ", length=" + length + ", done=" + done + ", fileImage="
				+ fileImage + "]";
	}
}

