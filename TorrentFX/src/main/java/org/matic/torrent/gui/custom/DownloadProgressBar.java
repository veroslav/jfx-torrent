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
import org.matic.torrent.gui.model.TorrentView;

import java.util.BitSet;

public final class DownloadProgressBar extends Canvas {
	
	private static final Color DOWNLOADED_PIECE_COLOR = Color.rgb(171, 214, 121);
	private static final Color PROGRESSBAR_COLOR = Color.rgb(80, 80, 255);

    private TorrentView torrentView;

	public DownloadProgressBar() {
		this.widthProperty().addListener(obs -> update(torrentView));
		this.heightProperty().addListener(obs -> update(torrentView));
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
	
	public void update(final TorrentView torrentView) {
		this.torrentView = torrentView;
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
		final double barWidth = torrentView == null? 0 :
			(((double)torrentView.getHavePieces()) / torrentView.getTotalPieces()) * (this.getWidth() - 1);
		
		context.setFill(PROGRESSBAR_COLOR);
		context.fillRect(1, 1, barWidth, 5);
	}
	
	private void drawAvailabilityBar(final GraphicsContext context) {
		if(torrentView == null) {
			return;
		}

        context.setFill(DOWNLOADED_PIECE_COLOR);

        final BitSet obtainedPieces = torrentView.getObtainedPieces();

		final double pieceWidth = (this.getWidth() - 2) / torrentView.getTotalPieces();
        int currentSegmentPieceIndex = -1;
        int segmentStartPieceIndex = obtainedPieces.nextSetBit(0);
        double xOffset = 1;
        double segmentStartX = xOffset + segmentStartPieceIndex * pieceWidth;

        for(int i = segmentStartPieceIndex; i != -1; i = obtainedPieces.nextSetBit(i + 1)) {
            if(i - 1 == currentSegmentPieceIndex) {
                currentSegmentPieceIndex = i;
                xOffset += pieceWidth;
            }
            else {
                //End of a segment, we can draw it
                context.fillRect(segmentStartX, 7, xOffset - segmentStartX, this.getHeight() - 1);

                //Prepare for next segment
                xOffset += (i - currentSegmentPieceIndex) * pieceWidth;

                segmentStartX = xOffset;
                currentSegmentPieceIndex = i;
            }
        }
	}
}