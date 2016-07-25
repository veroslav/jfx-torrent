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
package org.matic.torrent.gui.custom;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.matic.torrent.gui.model.BitsView;

/**
 * A bar that graphically displays a torrent's availability
 * 
 * @author Vedran Matic
 *
 */
public final class AvailabilityBar extends Canvas {
	
	private static final Paint NOT_AVAILABLE_COLOR = Color.rgb(141, 28, 16);
	private static final Paint AVAILABLE_COLOR = Color.rgb(171, 214, 121);
	
	private BitsView availability = null;
	
	public AvailabilityBar() {
		this.widthProperty().addListener(obs -> update(availability));
		this.heightProperty().addListener(obs -> update(availability));		
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

	public void update(final BitsView availability) {
		this.availability = availability;
		final GraphicsContext context = this.getGraphicsContext2D();
		
		//Draw a 3D effect around the bar
		context.setFill(Color.DARKGRAY);
		context.fillRect(0, 0, this.getWidth(), 1);
		context.fillRect(0, 0, 1, this.getHeight());
		
		drawAvailabilityBar(context);
	}
	
	private void drawAvailabilityBar(final GraphicsContext context) {
		//TODO: Implement method
	}
}