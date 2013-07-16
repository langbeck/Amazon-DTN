package br.ufpa.adtn.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.EventQueue.Event;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;

public abstract class BundleRouter<R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> {
	private static final String CONFIGURATION_KEY_NAME = "name";
	private final Map<EID, LC> connections;
	private boolean initialized;
	private String registration;
	private EventQueue eQueue;
	private Logger logger;
	private EID local_eid;
	private String name;
	private R self;
	
	@SuppressWarnings("unchecked")
	protected BundleRouter() {
		this.connections = new HashMap<EID, LC>();
		this.initialized = false;
		this.registration = null;
		
		try {
			this.self = (R) this;
		} catch (ClassCastException e) {
			logger.e("Wrong BundleRouter generic Router (self) type.", e);
			throw e;
		}
	}
	
	private void checkState() {
		if (!initialized)
			throw new IllegalStateException("Router not initialized");
	}
	
	final void init(final Properties config) {
		synchronized (this) {
			if (initialized)
				throw new IllegalStateException("Router already initialized");

			registration = config.getString("registration");
			if (!EID.isValidScheme(registration))
				throw new IllegalArgumentException("Invalid registration");
			
			local_eid = EID.get(String.format(
					"%s://%s",
					registration,
					BPAgent.getHostname()
			));
			
			name = config.getString(
					CONFIGURATION_KEY_NAME,
					getClass().getName().replaceFirst("^.*\\.", "")
			);
			
			logger = new Logger(name);
			eQueue = new EventQueue(logger, "EventQueue");
			
			initialized = true;
		}
		
		try {
			eQueue.postAndWait(new Runnable() {
				@Override
				public void run() {
					onCreate(config);
				}
			});
		} catch (ExecutionException e) {
			throw new InicializationException(e);
		}
	}
	
	public String getRegistration() {
		checkState();
		return registration;
	}
	
	public LC getConnection(EID to) {
		checkState();
		return connections.get(to);
	}
	
	protected final EventQueue getEventQueue() {
		checkState();
		return eQueue;
	}
	
	public void notifyLinkNear(Link link) {
		checkState();
		eQueue.post(new LinkNearEvent(link));
	}
	
	public void notifyDestroyed(boolean await) {
		checkState();
		eQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				onDestroy();
			}
		});
	}
	
	public EID getLocalEID() {
		checkState();
		return local_eid;
	}


	protected void onCreate(Properties config) { }
	protected void onDestroy() { }
	
	protected abstract boolean onLinkNear(Link link);
	
	protected abstract LC createConnection(Link link);
	
	
	private class LinkNearEvent extends Event {
		private final Link link;
		
		public LinkNearEvent(Link link) {
			this.link = link;
		}

		@Override
		public void execute() throws Throwable {
			if (onLinkNear(link))
				link.requestPark(self);
		}
	}
}
