package br.ufpa.adtn.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.Properties;


public abstract class BaseCL<TAdapter extends BaseCL.IAdapter, TConnection extends BaseCL.IConnection> {
	private final List<TAdapter> adapters;
	
	protected BaseCL() {
		this.adapters = new ArrayList<TAdapter>();
	}
	
	protected final void notifyConnectionDiscovered(IConnection connection) {
		BPAgent.notifyLinkNear(Link.get(connection.getEndpointID()));
	}
	
	public final TAdapter loadAdapter(Properties configuration, Object data) {
		synchronized (adapters) {
			final TAdapter adapter = createAdapter(configuration, data);
			adapters.add(adapter);
			return adapter;
		}
	}
	
	public final Collection<TAdapter> getLoadedAdapters() {
		return Collections.unmodifiableCollection(adapters);
	}
	
	
	/**
	 * 
	 * @param configuration Basic properties to be used to create the adapter.
	 * @param data Optional data. This is platform variant.
	 * @return
	 */
	protected abstract TAdapter createAdapter(Properties configuration, Object data);
	
	
	public interface IAdapter {
		public boolean start() throws Throwable;
		public void stop();
		
		public IDiscovery getDiscovery();
		public boolean isRunning();
		public String getName();
	}
	
	public interface IConnection {
		public void send(Bundle bundle);
		
		public EID getEndpointID();
		public boolean isConnected();
		public boolean isClosed();
		
		public void connect() throws IOException;
		public void close();
	}
	
	public interface IDiscovery {
		public void start() throws Throwable;
		public boolean isRunning();
		public void stop();
	}
}
