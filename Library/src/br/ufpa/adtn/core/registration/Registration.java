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
