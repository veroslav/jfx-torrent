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

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public final class FilterTorrentsComboBoxSkin<T> extends ComboBoxListViewSkin<T> {

    private final Label filterGraphic = new Label("[Y]");

    public FilterTorrentsComboBoxSkin(final ComboBox<T> comboBox) {
        super(comboBox);
        super.getChildren().add(filterGraphic);

        final Node listSkin = super.getChildren().stream().filter(
                c -> c.getStyleClass().contains("list-view")).findAny().get();
        listSkin.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<Event>() {
            @Override
            public void handle(final Event event) {
                System.out.println("ListView mouse released, option: " + event.getTarget().toString());
            }
        });
    }

    @Override
    protected void layoutChildren(final double x, final double y, final double width, final double height) {
        super.layoutChildren(x, y, width, height);

        final double arrowWidth = snapSize(arrow.prefWidth(-1));
        final double arrowButtonWidth = arrowButton.snappedLeftInset() + arrowWidth +
                        arrowButton.snappedRightInset();

        final double filterGraphicWidth = filterGraphic.snappedLeftInset() + snapSize(filterGraphic.prefWidth(-1))
                + filterGraphic.snappedRightInset();

        getChildren().stream()
                .filter(node -> !node.getStyleClass().contains("arrow") && !node.getStyleClass().contains("arrow-button"))
                .forEach(node -> node.resizeRelocate(x + arrowButtonWidth + filterGraphicWidth, y,
                        width - arrowButtonWidth - filterGraphicWidth, height));

        positionInArea(filterGraphic, x, y,
                filterGraphicWidth, height, 0, HPos.LEFT, VPos.CENTER);

        positionInArea(arrowButton, x + filterGraphicWidth, y,
                arrowButtonWidth, height, 0, HPos.CENTER, VPos.CENTER);


    }
}