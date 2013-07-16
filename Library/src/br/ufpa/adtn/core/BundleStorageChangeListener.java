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
