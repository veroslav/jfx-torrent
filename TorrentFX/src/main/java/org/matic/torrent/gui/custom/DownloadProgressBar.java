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
package org.matic.torrent.gui.custom;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.matic.torrent.gui.model.BitsView;

public final class DownloadProgressBar extends Canvas {
	
	private static final Color DOWNLOADED_PIECE_COLOR = Color.rgb(171, 214, 121);
	private static final Color PROGRESSBAR_COLOR = Color.rgb(80, 80, 255);
	
	private BitsView availabilityView = null;

	public DownloadProgressBar() {
		this.widthProperty().addListener(obs -> update(availabilityView));
		this.heightProperty().addListener(obs -> update(availabilityView));
	}
	
	@Override
	public double prefWidth(final double width) {
		return this.getWidth();
	};
	
	@Override
	public double prefHeight(final double height) {
		return this.getHeight();
	};
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	public void update(final BitsView availabilityView) {
		this.availabilityView = availabilityView;
		final GraphicsContext context = this.getGraphicsContext2D();
		context.clearRect(0, 0, this.getWidth(), this.getHeight());
		
		//Draw a 3D effect around the bar
		context.setFill(Color.DARKGRAY);
		context.fillRect(0, 0, this.getWidth(), 1);
		context.fillRect(0, 0, 1, this.getHeight());
		
		//Draw a separator between progress and availability bars
		context.fillRect(0, 6, this.getWidth(), 1);
		
		//Draw status bars (progress and availability)		
		drawProgressBar(context);
		drawAvailabilityBar(context);
	}
	
	private void drawProgressBar(final GraphicsContext context) {
		final double barWidth = availabilityView == null? 0 :
			(((double)availabilityView.getHavePiecesCount()) / availabilityView.getTotalPieces()) * (this.getWidth() - 1);
		
		context.setFill(PROGRESSBAR_COLOR);
		context.fillRect(1, 1, barWidth, 5);
	}
	
	private void drawAvailabilityBar(final GraphicsContext context) {
		if(availabilityView == null) {
			return;
		}

		final double pieceWidth = (this.getWidth() - 2) / availabilityView.getTotalPieces();
		
		context.setFill(DOWNLOADED_PIECE_COLOR);
		double xOffset = 1;
		for(int j = 0; j < availabilityView.getLastHaveIndex(); xOffset += pieceWidth, ++j) {

		    //TODO: It is better to draw continuous pieces at once in a single fillRect()

			if(availabilityView.getHave(j)) {
				context.fillRect(xOffset, 7, pieceWidth, this.getHeight() - 1);
			}
		}
	}
}