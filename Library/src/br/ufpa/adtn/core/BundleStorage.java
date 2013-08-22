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
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;

public abstract class BundleStorage {
	public static final int REASON_OK			= 0x00;
	public static final int REASON_UNKNOWN		= 0x01;
	public static final int REASON_DUPLICATED	= 0x02;
	
	private static final Map<String, Constructor<? extends BundleStorage>> MODELS;
	private static final Logger LOGGER = new Logger("BundleStorage");
	
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
			MODELS.put(model, mClass.getConstructor(long.class));
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Model does not have a default constructor", e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Default constructor is not accessible", e);
		}
	}
	
	public static BundleStorage createStorage(String model, long size, Properties config) throws IllegalStateException, ExecutionException {
		final Constructor<? extends BundleStorage> c = MODELS.get(model);
		if (c == null)
			throw new IllegalArgumentException("Invalid model");
		
		try {
			final BundleStorage storage = c.newInstance(size);
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
	
	private final long capacity;
	private boolean initialized;
	private long used;
	
	protected BundleStorage(long capacity) {
		this.initialized = false;
		this.capacity = capacity;
		this.used = 0L;
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

	public final long getAvailable() {
		return capacity - used;
	}
	
	public final long getCapacity() {
		return capacity;
	}

	public final long getUsed() {
		return used;
	}
	
	protected void onInit(Properties config) { }
	
	public final void remove(Bundle bundle) {
		LOGGER.v(String.format("Removing bundle %016x", bundle.getUniqueID()));
		synchronized (this) {
			used -= bundle.getPayloadLength();
			if (used < 0) {
				LOGGER.e(String.format(
						"Used space calculation error: %d [Set to 0]",
						used
				));
				used = 0;
			}
			
			delete(bundle);
		}
	}

	public final boolean add(Bundle bundle) {
		final long uniqueID = bundle.getUniqueID();
		LOGGER.v(String.format("Adding bundle %016x", uniqueID));
		synchronized (this) {
			final long blen = bundle.getPayloadLength();
			if (used + blen > capacity) {
				LOGGER.w("Storage capacity overflow");
				return false;
			}
			
			final int reason = put(bundle);
			if (reason != REASON_OK) {
				LOGGER.w(String.format(
						"Storage model refused bundle %016x for reason %d",
						uniqueID,
						reason
				));
				return false;
			}

			used += blen;
			return true;
		}
	}

	public abstract Collection<Bundle> getBundles();
	protected abstract void delete(Bundle bundle);
	protected abstract int put(Bundle bundle);



	public static class MemoryStorage extends BundleStorage {
		private final Collection<Bundle> roBundles;
		private final Collection<Bundle> bundles;
		
		public MemoryStorage(long capacity) {
			super(capacity);
			this.bundles = new HashSet<Bundle>();
			this.roBundles = Collections.unmodifiableCollection(bundles);
		}
	
		@Override
		public Collection<Bundle> getBundles() {
			return roBundles;
		}
	
		@Override
		protected void delete(Bundle bundle) {
			bundles.remove(bundle);
		}
	
		@Override
		protected int put(Bundle bundle) {
			return bundles.add(bundle) ? 0 : REASON_DUPLICATED;
		}
	}
}
