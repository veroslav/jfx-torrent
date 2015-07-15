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

/**
 * A task that can scheduled to run multiple times
 * 
 * @author vedran
 *
 */
public final class PeriodicTask {

	private final Runnable task;
	private final long period;
	
	private long lastRunTime;
	
	/**
	 * Create a new periodic task
	 * 
	 * @param task Task to be executed
	 * @param period How often to run the task
	 */
	public PeriodicTask(final Runnable task, final long period) {
		this.task = task;
		this.period = period;
		
		lastRunTime = 0;
	}
	
	public final long getPeriod() {
		return period;
	}
	
	public final Runnable getTask() {
		return task;
	}
	
	public final void setLastRunTime(final long lastRunTime) {
		this.lastRunTime = lastRunTime;
	}
	
	public final long getLastRunTime() {
		return lastRunTime;
	}
}