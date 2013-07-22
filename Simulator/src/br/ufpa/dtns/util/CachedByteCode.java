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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CachedByteCode {
	private final Map<String, byte[]> transformed;
	private final Map<String, byte[]> cached;
	private ClassTransformer transformer;
	private int translen;
	private int datalen;
	
	public CachedByteCode() {
		this(null);
	}
	
	public CachedByteCode(CachedByteCode cbc, ClassTransformer transformer) {
		this(cbc, transformer, false);
	}
	
	public CachedByteCode(ClassTransformer transformer) {
		this.transformed = new HashMap<String, byte[]>();
		this.cached = new HashMap<String, byte[]>();
		this.transformer = transformer;
		this.translen = 0;
		this.datalen = 0;
	}
	
	public CachedByteCode(CachedByteCode cbc, ClassTransformer transformer, boolean original) {
		this.translen = 0;
		this.datalen = 0;
		
		if (transformer == null) {
			this.transformed = new HashMap<String, byte[]>(cbc.transformed);
			this.cached = new HashMap<String, byte[]>(cbc.cached);
			this.transformer = null;
		} else {
			final Map<String, byte[]> map = new HashMap<String, byte[]>(cbc.cached);
			if (!original)
				map.putAll(cbc.transformed);

			this.transformed = new HashMap<String, byte[]>();
			this.cached = new HashMap<String, byte[]>();
			this.transformer = transformer;

			for (Map.Entry<String, byte[]> e : map.entrySet())
				register(e.getKey(), e.getValue());
		}
	}
	
	public synchronized void updateTransformer(ClassTransformer cTrans) {
		transformer = cTrans;
		transformed.clear();
		translen = 0;
		
		if (transformer == null)
			return;
		
		datalen = 0;
		for (Map.Entry<String, byte[]> e : cached.entrySet()) {
			final String name = e.getKey();
			byte[] data = e.getValue();
			datalen += data.length;
			
			data = doTransform(name, e.getValue());
			if (data == null)
				continue;
			
			transformed.put(name, data);
			translen += data.length;
		}
	}
	
	public synchronized void load(URL ... urls) throws IOException, URISyntaxException {
		for (URL url : urls) {
			final File file = new File(url.toURI());
			if (!file.exists())
				continue;
			
			if (file.getName().endsWith(".jar"))
				load(new JarFile(file));
			
			else if (file.isDirectory())
				load(file);
		}
	}

	public synchronized void load(JarFile jar) throws IOException {
		final Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			final JarEntry e = entries.nextElement();
			final String fname = e.getName();
			if (!fname.endsWith(".class"))
				continue;
			
			final String cname = fname.replace('/', '.').replaceFirst("\\.class$", "");
			if (cached.containsKey(cname))
				continue;
			
			final byte[] data = read(jar.getInputStream(e));
			if (data == null)
				continue;
			
			register(cname, data);
		}
	}
	
	public synchronized void load(File dir) throws IOException {
		if (!dir.isDirectory())
			throw new IllegalArgumentException();
		
		load(dir, "");
	}
	
	public byte[] get(String cname) {
		return get(cname, false);
	}
	
	public byte[] get(String cname, boolean original) {
		if (original)
			return cached.get(cname);
		
		byte[] data = transformed.get(cname);
		if (data != null)
			return data;
		
		return cached.get(cname);
	}
	
	public ClassTransformer getTransformer() {
		return transformer;
	}
	
	public int getTransformedSize() {
		return translen;
	}
	
	public int getCachedSize() {
		return datalen;
	}
	
	public int getDataSize() {
		return datalen + translen;
	}
	
	public synchronized void commit() {
		cached.putAll(transformed);
		transformed.clear();
	}
	
	private void load(File dir, String pkg) throws IOException {
		for (File f : dir.listFiles()) {
			final String fname = f.getName();
			if (f.isDirectory()) {
				load(f, pkg + fname + ".");
			} else if (fname.endsWith(".class")) {
				final String cname = pkg + fname.replaceFirst("\\.class$", "");
				if (cached.containsKey(cname))
					continue;
				
				final byte[] data = read(f);
				if (data == null)
					continue;
				
				register(cname, data);
			}
		}
	}
	
	private byte[] read(InputStream in) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] buffer = new byte[0x100];
		try {
			for (int readed; (readed = in.read(buffer)) != -1; )
				baos.write(buffer, 0, readed);
		} finally {
			try {
				in.close();
			} catch (IOException ex) { }
		}
		
		return baos.toByteArray();
	}
	
	private byte[] read(File f) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		return read(new BufferedInputStream(fis));
	}
	
	private void register(String name, byte[] data) {
		cached.put(name, data);
		datalen += data.length;
		
		if (transformer == null)
			return;
		
		data = doTransform(name, data);
		if (data == null)
			return;
		
		transformed.put(name, data);
		translen += data.length;
	}
	
	private byte[] doTransform(String name, byte[] data) {
		final byte[] copy = new byte[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return transformer.transform(name, copy);
	}
}
