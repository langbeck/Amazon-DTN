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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BaseCL.IAdapter;
import br.ufpa.adtn.core.BaseCL.IConnection;
import br.ufpa.adtn.core.BaseCL.IDiscovery;
import br.ufpa.adtn.core.configuration.AdapterConfiguration;
import br.ufpa.adtn.core.configuration.ConvergenceLayerConfiguration;
import br.ufpa.adtn.core.configuration.LoadConfiguration;
import br.ufpa.adtn.core.configuration.RouterConfiguration;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.core.registration.BundleRegistry;
import br.ufpa.adtn.core.registration.Registration;
import br.ufpa.adtn.core.registration.Registry;
import br.ufpa.adtn.util.BundleOutbox;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.EventQueue.Event;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.PeriodicEvent;
import br.ufpa.adtn.util.Properties;

public final class BPAgent {
	public static final byte VERSION = 0x06;
	
	public static enum State {
		PARSE_ERROR	, CLEAR			,
		INITIALIZING, INITIALIZED	,
		LOADING		, LOADED		,
		STARTING	, STARTED
	}

	private static final Collection<BundleStorageChangeListener> storageListeners;
	private static final Registration<String, Bundle> registration;
	private static final Collection<RouterStub<?, ?>> routers;
	private static final Collection<PeriodicEvent> events;
	private static final Collection<IAdapter> adapters;
	private static final LoadConfiguration config;
	private static final EventQueue eQueue;
	private static final BundleOutbox bOutbox;
	private static final Logger LOGGER;

	private static SimulationConfiguration sConfig;
	private static BundleStorage bStorage;
	private static boolean simulatedMode;
	private static ClassLoader cLoader;
	private static State state;
	
	static {
		storageListeners = new ArrayList<BundleStorageChangeListener>();
		registration = new Registration<String, Bundle>();
		routers = new ArrayList<RouterStub<?, ?>>();
		events = new HashSet<PeriodicEvent>();
		adapters = new ArrayList<IAdapter>();
		config = new LoadConfiguration();
		LOGGER = new Logger("BPAgent");
		eQueue = new EventQueue();
		bOutbox = new BundleOutbox();
		state = State.CLEAR;
		bStorage = null;
		cLoader = null;
	}
	
	public static SimulationConfiguration getSimulationConfig() {
		return sConfig;
	}
	
	private synchronized static void checkStateAndChange(State expected, State newState) {
		checkState(expected);
		
		LOGGER.v(String.format("Changing state from %s to %s", state, newState));
		state = newState;
	}
	
	private static void checkState(State state) throws IllegalAccessError {
		checkState(state, String.format(
				"Illegal state. Current is %s and was expected %s.",
				BPAgent.state,
				state
		));
	}
	
	private static void checkState(State state, String eMsg) throws IllegalAccessError {
		if (state != BPAgent.state)
			throw new IllegalAccessError(eMsg);
	}
	
	public static void routeUnlink(EID dst, EID next) {
		LOGGER.v(String.format("Bundle outbox unlink: %s -> %s", dst, next));
		bOutbox.unlink(dst, next);
	}
	
	public static void routeLink(EID dst, EID next) {
		LOGGER.v(String.format("Bundle outbox link: %s -> %s", dst, next));
		bOutbox.link(dst, next);
	}
	
	public static State getState() {
		return state;
	}
	
	public synchronized static void registerPeriodicEvent(PeriodicEvent event) {
		if (state.ordinal() >= State.STARTING.ordinal())
			throw new IllegalStateException("BPAgent was already started");
		
		if (!isSimulated())
			throw new IllegalStateException("BPAgent need be in simulated mode");
		
		if (event.isBinded())
			throw new IllegalArgumentException("Event already binded");

		event.bind(eQueue);
		events.add(event);
	}
	
	public synchronized static void init(SimulationConfiguration sConfig) {
		checkStateAndChange(State.CLEAR, State.INITIALIZING);
		final boolean simulation = (sConfig != null);
		BPAgent.sConfig = sConfig;
		
		if (simulation) {
			LOGGER.i("Starting in SIMULATED mode.");
			simulatedMode = true;
		} else {
			LOGGER.i("Starting in NORMAL mode.");
			simulatedMode = false;
		}
		
		checkStateAndChange(State.INITIALIZING, State.INITIALIZED);

		if (simulation)
			SystemClock.setHooker(sConfig.getClockHooker());
		
		registration.put("dtn", new Registry<Bundle>() {
			
			@Override
			public void delivery(Bundle data) {
				// TODO Check this
				LOGGER.d("Bundle received in DTN scheme.");
				addBundle(data);
			}
		});
	}
	
	public static boolean isSimulated() {
		if (state.ordinal() < State.INITIALIZED.ordinal())
			throw new IllegalStateException("BPAgent need get initialized first.");
		
		return simulatedMode;
	}
	
	public static boolean isNormal() {
		if (state.ordinal() < State.INITIALIZED.ordinal())
			throw new IllegalStateException("BPAgent need get initialized first.");
		
		return !simulatedMode;
	}
	
	static void checkConnectorSyncAndState() throws IllegalAccessError {
		checkState(State.LOADING, "BPAgent is not in loading state");
		eQueue.checkSync();
	}
	
	public static void setHostname(String hostname) {
		config.setHostname(hostname);
	}
	
	public static String getHostname() {
		return config.getHostname();
	}
	
	public static EID getHostEID() {
		final String hostname = getHostname();
		return (hostname != null) ?
				EID.get("dtn://" + hostname) :
				EID.NULL;
	}
	
	public static void load(InputStream input) throws ParserConfigurationException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		load(input, null);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized static void load(InputStream input, Object data) throws ParserConfigurationException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		try {
			checkStateAndChange(State.INITIALIZED, State.LOADING);
			LOGGER.d("Parsing configuration");
			config.load(input);
			
			LOGGER.d("Processing Convergence Layers");
			for (ConvergenceLayerConfiguration cl : config.getConvergenceLayers()) {
				final String clClass = cl.getClassName();
				
				LOGGER.d("Loading " + clClass);
				final BaseCL<?, ?> bcl = loadClass(clClass, BaseCL.class);

				LOGGER.d("Loading adapters for " + clClass);
				for (AdapterConfiguration adapter : cl.getAdapters()) {
					
					processAdapter(createAdapter(
							bcl,
							adapter.getProperties(),
							data
					));
				}
			}
			
			String sModel = config.getStorageModel();
			if (sModel == null) {
				LOGGER.i("Storage model not defined. Using in memory storage.");
				sModel = "memory";
			}
			
			try {
				bStorage = BundleStorage.createStorage(
						sModel,
						config.getStorageSize(),
						null
				);
			} catch (Exception e) {
				throw new InicializationException("Storage load failure", e);
			}
			
			LOGGER.d("Processing Routers");
			for (RouterConfiguration router : config.getRouters()) {
				final String registration = router.getRegistration();
				final String routerClass = router.getClassName();
				
				LOGGER.d(String.format(
						"Loading %s registered to %s",
						routerClass,
						registration
				));
				
				processRouter(
						loadClass(routerClass, BundleRouter.class),
						router
				);
			}
			
			checkStateAndChange(State.LOADING, State.LOADED);
		} catch (ParserConfigurationException pce) {
			checkStateAndChange(State.LOADING, State.PARSE_ERROR);
			throw pce;
		} catch (SAXException e) {
			checkStateAndChange(State.LOADING, State.PARSE_ERROR);
			throw new ParserConfigurationException(e.getMessage());
		} finally {
			try {
				input.close();
			} catch (Exception e) { }
		}
	}
	
	private static <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> void processRouter(R router, RouterConfiguration config) {
		registration.put(config.getRegistration(), new RouterRegistry<R, LC>(router));
		routers.add(new RouterStub<R, LC>(router, config));
	}
	
	private static void processAdapter(IAdapter adapter) {
		adapters.add(adapter);
	}
	
	private static IAdapter createAdapter(
			final BaseCL<?, ?> cLayer,
			final Properties config,
			final Object data
	) {
		try {
			return eQueue.submit(new Callable<IAdapter>() {
				@Override
				public IAdapter call() throws Exception {
					return cLayer.createAdapter(config, data);
				}
			});
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}
	
	private static <T> T loadClass(String className, Class<T> base) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		checkState(State.LOADING);
		
		final Class<? extends T> cl = Class.forName(
				className,
				true,
				getClassLoader()
		).asSubclass(base);
		
		try {
			/*
			 * Create the new instance inside the EventQueue of BPAgent
			 */
			return eQueue.submit(new Callable<T>() {
				@Override
				public T call() throws Exception {
					return cl.newInstance();
				}
			});
		} catch (ExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof InstantiationException)
				throw (InstantiationException) cause;
			
			throw new RuntimeException(cause);
		}
	}
	
	private static ClassLoader getClassLoader() {
		return cLoader == null ?
				Thread.currentThread().getContextClassLoader() :
				cLoader;
	}
	
	public static void setClassLoader(ClassLoader loader) {
		checkState(State.CLEAR);
		
		cLoader = loader;
	}
	
	public synchronized static void start() {
		checkStateAndChange(State.LOADED, State.STARTING);
		
		if (simulatedMode) {
			LOGGER.i(String.format(
					"Starting at %s in SIMULATED time",
					sConfig.getStart()
			));
		}
		
		LOGGER.i("Starting BPA registered adapters");
		for (IAdapter adapter : adapters) {
			final String prefix = String.format(
					"Adapter[%s]:",
					adapter.getName()
			);
			
			LOGGER.i(prefix + " Loading");
			try {
				final IDiscovery discovery = adapter.getDiscovery();
				if (discovery != null) {
					LOGGER.i(prefix + " Starting discovery");
					discovery.start();
				} else {
					LOGGER.i(prefix + " No discovery associated");
				}

				LOGGER.i(prefix + " Starting");
				adapter.start();
				LOGGER.i(prefix + " Started");
			} catch (Throwable t) {
				LOGGER.e("Component error", t);
			}
		}
		
		LOGGER.i("Starting BPA registered routers");
		for (RouterStub<?, ?> stub : routers)
			stub.init();
		
		for (PeriodicEvent event : events)
			event.start();
		
		checkStateAndChange(State.STARTING, State.STARTED);
	}
	
	static void notifyBundleReceived(IConnection conn, Bundle bundle) {
		LOGGER.v("Bundle received from " + bundle.getSource());
		
		final EID dest = bundle.getDestination();
		if (!dest.getSSP().equals("//" + getHostname())) {
			// TODO Remove
			LOGGER.w("NOT FOR ME");
			return;
		}
		
		if (!registration.publish(dest.getScheme(), bundle)) {
			LOGGER.w(String.format(
					"No registration found to reveice bundle from %s to %s",
					bundle.getSource(), dest
			));
		}
	}

	static void notifyAdapterStoped(IAdapter adapter, Throwable reason) {
		if (reason != null) {
			LOGGER.e("Adapter stoped", reason);
		} else {
			LOGGER.d("Adapter stoped");
		}
	}
	
	static void notifyAdapterStarted(IAdapter adapter) {
		//TODO Implement
	}
	
	static void notifyLinkNear(Link link) {
		final EID eid = link.getEndpointID();
		LOGGER.v("Link near " + eid);
		if (!eid.isBase()) {
			LOGGER.w("  Illegal EID. [IGNORING]");
			return;
		}
		
		synchronized (routers) {
			for (final RouterStub<?, ?> stub : routers)
				stub.router.notifyLinkNear(link);
		}
		flushBundles(link);
	}
	
	public static void flushBundles(Link link) {
		final Collection<Bundle> bundles = bOutbox.searchBundles(link.getEndpointID());
		if (!bundles.isEmpty()) {
			LOGGER.i("Sending bundles directly");
			link.sendAll(bundles);
		}
	}
	
	public static void flushBundles(EID eid) {
		final Collection<Bundle> bundles = bOutbox.searchBundles(eid);
		if (!bundles.isEmpty()) {
			LOGGER.i("Sending bundles directly");
			Link.get(eid).sendAll(bundles);
		}
	}
	
	/*
	 * STORAGE ACCESS
	 */
	
	public static void registerStorageChangeListener(BundleStorageChangeListener listener) {
		synchronized (storageListeners) {
			storageListeners.add(listener);
		}
	}
	
	public static Collection<Bundle> getBundlesFor(EID dst) {
		return bOutbox.searchBundles(dst);
	}
	
	public static Collection<Bundle> getBundles() {
		return bStorage.getBundles();
	}
	
	public static void addBundle(final Bundle bundle) {
		final long expiration = bundle.getInfo().getSecondsToExpiration();
		final long uniqueID = bundle.getUniqueID();
		LOGGER.v(String.format("Bundle add requested for %016x", uniqueID));
		if (expiration <= 0) {
			LOGGER.w(String.format("Bundle already expired %016x", uniqueID));
			return;
		}
		
		eQueue.post(new BundleExpirationEvent(bundle));
		if (!bStorage.add(bundle)) {
			LOGGER.w(String.format("Bundle being dropped %016x", uniqueID));
			InformationHub.onDeleted(bundle, true);
			return;
		}
		
		bOutbox.add(bundle);

		synchronized (storageListeners) {
			for (BundleStorageChangeListener listener : storageListeners)
				listener.notifyBundleAdded(eQueue, bundle);
		}
	}
	
	public static void removeBundle(Bundle bundle) {
		LOGGER.v(String.format("Bundle remove requested %016x", bundle.getUniqueID()));
		InformationHub.onDeleted(bundle, false);
		bStorage.remove(bundle);
		bOutbox.remove(bundle);
		
		synchronized (storageListeners) {
			for (BundleStorageChangeListener listener : storageListeners)
				listener.notifyBundleRemoved(eQueue, bundle);
		}
	}

	public static long getStorageAvailable() {
		return bStorage.getAvailable();
	}



	private static class RouterStub<R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> {
		private final RouterConfiguration config;
		private final R router;
		
		public RouterStub(R router, RouterConfiguration config) {
			this.config = config;
			this.router = router;
		}
		
		public void init() {
			router.init(config.getProperties());
		}
	}
	
	
	private static class BundleExpirationEvent extends Event {
		private final Bundle bundle;
		
		public BundleExpirationEvent(Bundle bundle) {
			this.bundle = bundle;
		}

		@Override
		public void execute() throws Throwable {
			LOGGER.v(String.format("Bundle %016x expired", bundle.getUniqueID()));
			removeBundle(bundle);
		}
	}
	
	
	private static class RouterRegistry<R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> implements BundleRegistry {
		private static final Logger LOGGER = new Logger("RouterRegistry");
		private final R router;
		
		public RouterRegistry(R router) {
			this.router = router;
		}

		@Override
		public void delivery(Bundle bundle) {
			final EID source = bundle.getSource();
			final LC connection = Link.get(source.withScheme("dtn"))
					.getConnection(router);
			
			if (connection == null) {
				LOGGER.d(String.format(
						"No connection available for EID %s in router %s",
						source.toString(), router.getClass()
				));
				return;
			}
			
			connection.notifyBundleReceived(bundle);
		}
	}

	private BPAgent() { }
}
