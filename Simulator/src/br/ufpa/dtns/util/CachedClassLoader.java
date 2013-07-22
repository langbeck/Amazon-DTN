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
package br.ufpa.dtns.util;

import java.util.HashMap;
import java.util.Map;

public class CachedClassLoader extends ClassLoader {
	private final Map<String, Class<?>> loaded;
	private final CachedByteCode cache;
	private final ClassLoader parent;
	
	public CachedClassLoader(CachedByteCode cache) {
		this(null, cache);
	}
	
	public CachedClassLoader(ClassLoader parent, CachedByteCode cache) {
		if (cache == null)
			throw new NullPointerException();
		
		this.loaded = new HashMap<String, Class<?>>();
		this.parent = parent;
		this.cache = cache;
	}
	
	public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cl = loaded.get(name);
		if (cl != null)
			return cl;
		
		final byte[] bcode = cache.get(name);
		if (bcode != null) {
			cl = defineClass(name, bcode, 0, bcode.length);
			loaded.put(name, cl);
			return cl;
		}
		
		if (parent != null)
			return parent.loadClass(name);
		
		throw new ClassNotFoundException();
	}
	
	public CachedByteCode getCache() {
		return cache;
	}
}
