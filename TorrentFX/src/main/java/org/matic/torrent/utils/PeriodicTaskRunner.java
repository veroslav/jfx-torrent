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

package org.matic.torrent.utils;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A service that periodically executes actions 
 * (such as updating the GUI with the latest values)
 * 
 * @author vedran
 *
 */
public final class PeriodicTaskRunner extends ScheduledService<Void> {
	private final List<PeriodicTask> tasks = new CopyOnWriteArrayList<>();	
	
	public final void addTask(final PeriodicTask task) {
		tasks.add(task);
	}
	
	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
            @Override
            public Void call() {		            	
            	tasks.forEach(t -> {
            		//if((System.currentTimeMillis() - t.getLastRunTime()) >= t.getPeriod()) {           			
	            		t.getTask().run();	            		
	            		//t.setLastRunTime(System.currentTimeMillis());
	            	//}
            	});		            	
            	return null;
            }
		};
	}
}