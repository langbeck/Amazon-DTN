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
	private Thread thread;
	
	public SingleThreadFactory() {
		this.thread = null;
	}

	@Override
	public synchronized Thread newThread(Runnable r) {
		if (thread != null)
			throw new IllegalStateException("One thread has already been created");
		
		thread = new Thread(r);
		return thread;
	}
	
	public synchronized Thread getThread() {
		return thread;
	}
}
