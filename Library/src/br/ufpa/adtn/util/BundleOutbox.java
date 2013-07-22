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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.EID;

public class BundleOutbox {
	private final Map<EID, Collection<Bundle>> dstMapping;
	private final Map<EID, Collection<EID>> linkMapping;
	private final Lock writeLock;
	private final Lock readLock;
	
	public BundleOutbox() {
		this.dstMapping = new HashMap<EID, Collection<Bundle>>();
		this.linkMapping = new HashMap<EID, Collection<EID>>();
		
		final ReadWriteLock rwLock = new ReentrantReadWriteLock();
		this.writeLock = rwLock.writeLock();
		this.readLock = rwLock.readLock();
	}
	
	public boolean unlink(EID dst, EID next) {
		writeLock.lock();
		try {
			final Collection<EID> eids = linkMapping.get(next);
			return eids != null ? eids.remove(dst) : false;
		} finally {
			writeLock.unlock();
		}
	}
	
	public void link(EID dst, EID next) {
		if (dst.equals(next))
			return;
		
		writeLock.lock();
		try {
			Collection<EID> eids = linkMapping.get(next);
			if (eids == null) {
				eids = new HashSet<EID>();
				linkMapping.put(next, eids);
			}
			
			eids.add(dst);
		} finally {
			writeLock.unlock();
		}
	}
	
	public void add(Bundle bundle) {
		final EID bdst = bundle.getDestination().withScheme("dtn");
		
		writeLock.lock();
		try {
			Collection<Bundle> bundles = dstMapping.get(bdst);
			if (bundles == null) {
				bundles = new ArrayList<Bundle>();
				dstMapping.put(bdst, bundles);
			}
			
			bundles.add(bundle);
		} finally {
			writeLock.unlock();
		}
	}
	
	public boolean remove(Bundle b) {
		final EID bdst = b.getDestination().withScheme("dtn");
		
		writeLock.lock();
		try {
			final Collection<Bundle> bOut = dstMapping.get(bdst);
			return bOut != null ? bOut.remove(b) : false;
		} finally {
			writeLock.unlock();
		}
	}

	public Collection<Bundle> searchBundles(EID dst) {
		EID bdst = dst.withScheme("dtn");
		
		readLock.lock();
		try {
			final List<Bundle> bundles = new ArrayList<Bundle>();
			final Deque<EID> search = new ArrayDeque<EID>();
			final Set<EID> visited = new HashSet<EID>();
			
			do {
				if (visited.contains(bdst))
					continue;

				visited.add(bdst);
				
				final Collection<Bundle> dBundles = dstMapping.get(bdst);
				if (dBundles != null)
					bundles.addAll(dBundles);

				final Collection<EID> nexts = linkMapping.get(bdst);
				if (nexts == null)
					continue;
				
				for (EID next : nexts)
					search.push(next);
			} while ((bdst = search.poll()) != null);
			
			return Collections.unmodifiableCollection(bundles);
		} finally {
			readLock.unlock();
		}
	}
	
	public boolean containsBundles(EID dst) {
		readLock.lock();
		try {
			final Deque<EID> search = new ArrayDeque<EID>();
			final Set<EID> visited = new HashSet<EID>();
			
			do {
				if (visited.contains(dst))
					continue;

				visited.add(dst);
				
				final Collection<Bundle> dBundles = dstMapping.get(dst);
				if (dBundles != null && !dBundles.isEmpty())
					return true;

				final Collection<EID> nexts = linkMapping.get(dst);
				if (nexts == null)
					continue;
				
				for (EID next : nexts)
					search.push(next);
			} while ((dst = search.poll()) != null);
			
			return false;
		} finally {
			readLock.unlock();
		}
	}
}
