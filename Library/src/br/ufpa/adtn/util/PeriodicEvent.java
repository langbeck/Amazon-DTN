/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpa.adtn.util;

import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.util.EventQueue.Event;

public abstract class PeriodicEvent {
	private final TimeUnit unit;
	private final long interval;
	private final Event gEvent;
	private EventQueue eQueue;
	private boolean started;
	private boolean stoped;
	
	public PeriodicEvent(long interval, TimeUnit unit) {
		this(null, interval, unit);
	}
	
	public PeriodicEvent(EventQueue eQueue, long interval, TimeUnit unit) {
		this.interval = interval;
		this.eQueue = eQueue;
		this.started = false;
		this.stoped = false;
		this.unit = unit;
		
		this.gEvent = new Event() {
			@Override
			public void execute() throws Throwable {
				onEvent();
				schedule();
			}
		};
	}
	
	public synchronized boolean isBinded() {
		return eQueue != null;
	}
	
	public synchronized void bind(EventQueue eQueue) throws IllegalStateException {
		if (this.eQueue != null)
			throw new IllegalStateException("Already binded");
		
		this.eQueue = eQueue;
	}

	public synchronized void start() throws IllegalStateException {
		if (started)
			throw new IllegalStateException("Already started");
		
		if (eQueue == null)
			throw new IllegalStateException("Not binded");
		
		started = true;
		schedule();
	}
	
	public synchronized void stop() throws IllegalStateException {
		if (!started)
			throw new IllegalStateException("Not started");
		
		if (stoped)
			throw new IllegalStateException("Already stoped");
		
		stoped = true;
	}
	
	private synchronized void schedule() {
		if (!stoped)
			eQueue.schedule(gEvent, interval, unit);
	}
	
	protected abstract void onEvent();
}
