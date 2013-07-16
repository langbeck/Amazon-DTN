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
