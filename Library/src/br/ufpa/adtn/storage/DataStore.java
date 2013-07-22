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
package br.ufpa.adtn.storage;

import br.ufpa.adtn.util.Properties;

public abstract class DataStore {
	private boolean initialized;
	
	public DataStore() {
		this.initialized = false;
	}
	
	public final void init(Properties config) {
		synchronized (this) {
			if (initialized)
				throw new IllegalStateException("Already initialized");
			
			onInit(config);
			initialized = true;
		}
	}
	
	public abstract <T extends Identificable> EntityStorage<T> open(Class<T> cl);
	
	protected abstract void onInit(Properties config);
}
