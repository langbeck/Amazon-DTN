package br.ufpa.adtn.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.Properties;

public abstract class BundleStorage {
	private static final Map<String, Constructor<? extends BundleStorage>> MODELS;
	
	static {
		MODELS = new HashMap<String, Constructor<? extends BundleStorage>>();
		registerModel("memory", MemoryStorage.class);
	}
	
	public synchronized static Collection<String> getModels() {
		return Collections.unmodifiableCollection(MODELS.keySet());
	}
	
	public synchronized static boolean hasModelRegistered(String model) {
		return MODELS.containsKey(model);
	}
	
	public synchronized static void registerModel(String model, Class<? extends BundleStorage> mClass) throws IllegalArgumentException {
		if (MODELS.containsKey(model))
			throw new IllegalArgumentException("Model already registered");
		
		try {
			MODELS.put(model, mClass.getConstructor());
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Model does not have a default constructor", e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Default constructor is not accessible", e);
		}
	}
	
	public static BundleStorage createStorage(String model, Properties config) throws IllegalStateException, ExecutionException {
		final Constructor<? extends BundleStorage> c = MODELS.get(model);
		if (c == null)
			throw new IllegalArgumentException("Invalid model");
		
		try {
			final BundleStorage storage = c.newInstance();
			storage.init(config);
			return storage;
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Unexpected", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Unexpected", e);
		} catch (InvocationTargetException e) {
			throw new ExecutionException(e);
		} catch (InstantiationException e) {
			throw new ExecutionException(e);
		}
	}
	
	
	private boolean initialized;
	
	protected BundleStorage() {
		this.initialized = false;
	}

	public Collection<Bundle> getBundlesFrom(EID src) {
		final List<Bundle> bundles = new ArrayList<Bundle>();
		for (Bundle bundle : getBundles())
			if (bundle.getSource().equals(src))
				bundles.add(bundle);
		
		return bundles;
	}

	public Collection<Bundle> getBundlesFor(EID dst) {
		final List<Bundle> bundles = new ArrayList<Bundle>();
		for (Bundle bundle : getBundles())
			if (bundle.getDestination().equals(dst))
				bundles.add(bundle);
		
		return bundles;
	}
	
	private final void init(Properties config) throws IllegalStateException {
		synchronized (this) {
			if (initialized)
				throw new IllegalStateException("Already initialized");
			
			onInit(config);
			initialized = true;
		}
	}
	
	protected void onInit(Properties config) { }
	
	public abstract Collection<Bundle> getBundles();
	public abstract void remove(Bundle bundle);
	public abstract void add(Bundle bundle);
	
	

	public static class MemoryStorage extends BundleStorage {
		private final Collection<Bundle> roBundles;
		private final Collection<Bundle> bundles;
		
		public MemoryStorage() {
			this.bundles = new HashSet<Bundle>();
			this.roBundles = Collections.unmodifiableCollection(bundles);
		}
	
		@Override
		public Collection<Bundle> getBundles() {
			return roBundles;
		}
	
		@Override
		public void remove(Bundle bundle) {
			bundles.remove(bundle);
		}
	
		@Override
		public void add(Bundle bundle) {
			bundles.add(bundle);
		}
	}
}
