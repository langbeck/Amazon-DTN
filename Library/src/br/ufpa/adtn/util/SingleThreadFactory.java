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

import java.util.concurrent.ThreadFactory;

public class SingleThreadFactory implements ThreadFactory {
	private final Object locker;
	private boolean locked;
	private Thread thread;
	
	public SingleThreadFactory(boolean locked) {
		this.locker = new Object();
		this.locked = locked;
		this.thread = null;
	}
	
	public SingleThreadFactory() {
		this(false);
	}

	@Override
	public Thread newThread(Runnable r) {
		synchronized (locker) {
			if (thread != null)
				throw new IllegalStateException("One thread has already been created");
			
			thread = new Thread(r) {
				
				@Override
				public void start() {
					synchronized (locker) {
						if (locked)
							return;
						
						super.start();
					}
				}
			};
			return thread;
		}
	}
	
	public void startThread() {
		synchronized (locker) {
			if (thread == null)
				throw new RuntimeException("Inner thread not created yet");
			
			if (!locked)
				throw new RuntimeException("Not locked");
			
			locked = false;
			thread.start();
		}
	}
	
	public Thread getThread() {
		synchronized (locker) {
			return thread;
		}
	}
}
