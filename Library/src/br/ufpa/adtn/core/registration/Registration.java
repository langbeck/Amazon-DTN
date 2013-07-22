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
package br.ufpa.adtn.core.registration;

import java.util.HashMap;
import java.util.Map;

public class Registration<K, R> {
	private final Map<K, Registry<R>> registers;
	
	public Registration() {
		this.registers = new HashMap<K, Registry<R>>();
	}
	
	public boolean publish(K key, R data) {
		final Registry<R> registry = registers.get(key);
		if (registry == null)
			return false;
		
		registry.delivery(data);
		return true;
	}
	
	public void put(K key, Registry<R> value) {
		registers.put(key, value);
	}
}
