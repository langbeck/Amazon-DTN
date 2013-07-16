package br.ufpa.adtn.core;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BaseCL.IAdapter;
import br.ufpa.adtn.core.BaseCL.IConnection;

public class ConvergenceLayerConnector extends BPAgentConnector {
	private final IAdapter adapter;
	
	ConvergenceLayerConnector(IAdapter adapter) {
		this.adapter = adapter;
	}
	
	void notifyBundleReceived(IConnection conn, Bundle bundle) {
		BPAgent.notifyBundleReceived(conn, bundle);
	}

	void notifyAdapterStoped(Throwable reason) {
		BPAgent.notifyAdapterStoped(adapter, reason);
	}
	
	void notifyAdapterStarted() {
		BPAgent.notifyAdapterStarted(adapter);
	}
}
