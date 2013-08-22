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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Properties {
	private final HashMap<String, String> table;
	private final Properties parent;
	
	public Properties() {
		this(null);
	}
	
	public Properties(Properties parent) {
		this(parent, false);
	}
	
	public Properties(Properties parent, boolean readOnly) {
		if (readOnly) {
			if (parent == null)
				throw new IllegalArgumentException("Parent can not be null in read-only mode");

			this.parent = parent;
			this.table = null;
		} else {
			this.table = new HashMap<String, String>();
			this.parent = parent;
		}
	}
	
	public void setBoolean(String key, boolean value) {
		setString(key, String.valueOf(value));
	}
	
	public String setString(String key, String value) {
		if (table == null)
			throw new UnsupportedOperationException("Read-only");
		
		return (value != null) ? table.put(key, value) : table.remove(key);
	}

	public void setFloat(String key, float value) {
		setString(key, String.valueOf(value));
	}

	public void setInteger(String key, int value) {
		setString(key, String.valueOf(value));
	}

	public void setLong(String key, long value) {
		setString(key, String.valueOf(value));
	}
	
	public boolean getBoolean(String key, boolean def) {
		return Boolean.parseBoolean(getString(key, String.valueOf(def)));
	}
	
	public String getString(String key, String def) {
		if (table == null)
			return parent.getString(key, def);
		
		final String s = table.get(key);
		if (s == null) {
			if (parent == null)
				return def;
			
			return parent.getString(key, def);
		}
		
		return (s != null) ? s : def;
	}

	public float getFloat(String key, float def) {
		return Float.parseFloat(getString(key, String.valueOf(def)));
	}

	public int getInteger(String key, int def) {
		return Integer.parseInt(getString(key, String.valueOf(def)));
	}
	
	public long getLong(String key, long def) {
		return Long.parseLong(getString(key, String.valueOf(def)));
	}
	
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}
	
	public String getString(String key) {
		return getString(key, null);
	}
	
	public float getFloat(String key) {
		return getFloat(key, 0);
	}

	public int getInteger(String key) {
		return getInteger(key, 0);
	}

	public long getLong(String key) {
		return getLong(key, 0);
	}
	
	public String remove(String key) {
		return table.remove(key);
	}

	public boolean contains(String key) {
		return table.containsKey(key);
	}
	
	public int export(Properties dst, String ... keys) {
		if (dst.table == null)
			throw new UnsupportedOperationException("Destination is read-only");
		
		if (keys != null) {
			int copied = 0;
			for (String key : keys) {
				final String value = getString(key);
				if (value != null) {
					dst.table.put(key, value);
					copied++;
				}
			}
			return copied;
		} else {
			final int s = dst.table.size();
			dst.table.putAll(table);
			return table.size() - s;
		}
	}
	
	public int copy(Properties src, String ... keys) {
		return src.export(this, keys);
	}
	
	public int exportAll(Properties dst) {
		return export(dst, (String[]) null);
	}
	
	public int copy(Properties src) {
		return src.export(this, (String[]) null);
	}
	
	public int copyPrefix(Properties src, String prefix) {
		if (table == null)
			throw new UnsupportedOperationException("Read-only");
		
		int copied = 0;
		for (Map.Entry<String, String> e : src.table.entrySet()) {
			final String key = e.getKey(); 
			if (key.startsWith(prefix)) {
				table.put(key, e.getValue());
				copied++;
			}
		}
		
		return copied;
	}
	
	public Map<String, String> getInternalTable() {
		return Collections.unmodifiableMap(table);
	}
	
	public Properties asReadOnly() {
		return new Properties(this, true);
	}
	
	@Override
	public String toString() {
		return table.toString();
	}
}
