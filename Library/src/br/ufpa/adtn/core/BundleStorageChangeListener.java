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
package br.ufpa.adtn.core;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.EventQueue.Event;

public abstract class BundleStorageChangeListener {
	private final EventQueue eQueue;
	
	public BundleStorageChangeListener(EventQueue eQueue) {
		this.eQueue = eQueue;
	}
	
	final void notifyBundleRemoved(EventQueue eQueue, final Bundle bundle) {
		if (this.eQueue != null)
			eQueue = this.eQueue;
		
		eQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				onBundleRemoved(bundle);
			}
		});
	}
	
	final void notifyBundleAdded(EventQueue eQueue, final Bundle bundle) {
		if (this.eQueue != null)
			eQueue = this.eQueue;
		
		eQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				onBundleAdded(bundle);
			}
		});
	}
	

	public abstract void onBundleRemoved(Bundle bundle);
	
	public abstract void onBundleAdded(Bundle bundle);
}
